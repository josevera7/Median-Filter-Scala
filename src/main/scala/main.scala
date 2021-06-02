import java.awt.image.BufferedImage
import java.io.File

import javax.imageio.ImageIO
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case object Start
case object Finish
case object StartSerial
case object StartParallel

class ClientActor(serial:ActorRef,parallel:ActorRef) extends Actor{
  def receive ={
    case StartSerial =>
      println("Starting Serial Implementation")
      serial ! Start

    case StartParallel =>
      println("Starting Parallel Implementation")
      parallel ! Start

    case Finish =>
      println("Finished client")
      context.stop(self)

  }


}

class SerialMFActor(image:BufferedImage) extends Actor{
  def receive={
    case Start =>
      val outputImage:BufferedImage = new BufferedImage(image.getWidth,image.getHeight,image.getType)
      val windowwidth = 5
      val windowheight = 5
      val window = Array.ofDim[Int](windowwidth * windowheight)
      val edgex:Int = (windowwidth/2).toInt
      val edgey:Int = (windowheight/2).toInt
      val imageheight:Int = image.getHeight()
      val imagewidth:Int = image.getWidth()
      val t1 = System.currentTimeMillis()

      for(x <- edgex to imagewidth-edgex){
        for(y <- edgey to imageheight-edgey){
          var i = 0
          for (fx <- 0 to windowwidth-1){
            for (fy <- 0 to windowheight-1){
              try{
                window(i) = image.getRGB(x + fx - edgex,y + fy - edgey)
              }
              catch{
                case _ => window(i) = image.getRGB(fx,fy)
              }
              i = i+1
            }
          }
          window.sortInPlace()
          outputImage.setRGB(x,y,window(windowwidth * windowheight / 2))
        }
      }
      ImageIO.write(outputImage,"jpg",new File("serial.jpg"))
      var t2 = System.currentTimeMillis()
      var time = (t2-t1)/1000F
      println("Finished Serial Implementation with time "+time+" seconds" )
      sender ! StartParallel
      context.stop(self)
  }
}

class ParallelMFActor (image:BufferedImage) extends Actor{
  def receive ={
    case Start =>
      val outputImage:BufferedImage = new BufferedImage(image.getWidth,image.getHeight,image.getType)
      val windowwidth = 3
      val windowheight = 3
      val edgex:Int = (windowwidth/2).toInt
      val edgey:Int = (windowheight/2).toInt
      val imageheight:Int = image.getHeight()
      val imagewidth:Int = image.getWidth()
      val t1 = System.currentTimeMillis()

      val left = Future{
        val window = Array.ofDim[Int](windowwidth * windowheight)
        for(x <- edgex to ((imagewidth/2)-1)-edgex){
          for(y <- edgey to imageheight-edgey){
            var i = 0
            for (fx <- 0 to windowwidth-1){
              for (fy <- 0 to windowheight-1){
                try{
                  window(i) = image.getRGB(x + fx - edgex,y + fy - edgey)
                }
                catch{
                  case _ => window(i) = image.getRGB(fx,fy)
                }
                i = i+1
              }
            }
            window.sortInPlace()
            outputImage.setRGB(x,y,window(windowwidth * windowheight / 2))
          }
        }
      }

      val right = Future{
        val window = Array.ofDim[Int](windowwidth * windowheight)
        for(x <- edgex+(imagewidth/2) to imagewidth-edgex){
          for(y <- edgey to imageheight-edgey){
            var i = 0
            for (fx <- 0 to windowwidth-1){
              for (fy <- 0 to windowheight-1){
                try{
                  window(i) = image.getRGB(x + fx - edgex,y + fy - edgey)
                }
                catch{
                  case _ => window(i) = image.getRGB(fx,fy)
                }
                i = i+1
              }
            }
            window.sortInPlace()
            outputImage.setRGB(x,y,window(windowwidth * windowheight / 2))
          }
        }
      }


      ImageIO.write(outputImage,"jpg",new File("parallel.jpg"))
      var t2 = System.currentTimeMillis()
      var time = (t2-t1)/1000F
      println("Finished Parallel Implementation with time "+time+" seconds" )
      sender ! Finish
      context.stop(self)
  }
}



object project {
  def main(args: Array[String]): Unit ={
    val inputimage:BufferedImage= ImageIO.read(new File("Example_lena_denoise_noisy.jpg"))
    val system = ActorSystem("ProjectSystem")
    val serial = system.actorOf(Props(new SerialMFActor(inputimage)), name="serial")
    val parallel = system.actorOf(Props(new ParallelMFActor(inputimage)), name="parallel")
    val client = system.actorOf(Props(new ClientActor(serial,parallel)), name = "client")
    client ! StartSerial

  }

}
