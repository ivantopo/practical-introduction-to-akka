package game.server

import akka.actor.{ActorLogging, Props, Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

// If you want to follow the code, please clone
//    https://github.com/ivantopo/practical-introduction-to-akka

case class WifiInfo(ssid: String, password: String)
object WifiInfo {
  val ElementDooWifi = WifiInfo("element1", "elementgost")
}

object BaseExample extends App {
  import scala.concurrent.duration._

  val system = ActorSystem("test-system")
  val printer = system.actorOf(Props(new Printer(1)).withDispatcher("my-dispatcher"), "printer")
  implicit val timeout = Timeout(5 seconds)
  implicit val execContext = system.dispatcher


  val response = (printer ? "question").mapTo[String]

  response onComplete(println)


}

class Printer(param: Int) extends Actor with ActorLogging {
  val storage = context.actorOf(Props[Storage], "storage")

  def receive = {
    case anything: String =>
      log.info(anything)
      sender ! "response"
      storage ! anything
  }
}

class Storage extends Actor with ActorLogging {
  var x = 1
  def receive = {
    case anything: String =>
      x += 1
      log.info("Storing {} - {}", x,  anything)
  }
}
