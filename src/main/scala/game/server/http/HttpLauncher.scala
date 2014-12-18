package game.server.http

import akka.actor._
import akka.io.Tcp.ConnectionClosed
import game.server.engine.PlayRoom.{Join, PlayerUpdate}
import game.server.engine.PlayerSync.{PlayerDelta, Delta}
import game.server.engine.{PlayerState, PlayRoom}
import spray.http.CacheDirectives.`no-cache`
import spray.http.HttpHeaders.{RawHeader, `Cache-Control`}
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing.SimpleRoutingApp
import SseClient._


object JsonProtocol extends DefaultJsonProtocol {
  implicit val playerStateFormat = jsonFormat2(PlayerState)
  implicit val playerDeltaFormat = jsonFormat2(PlayerDelta)
  implicit val deltaJsonFormat = jsonFormat2(Delta)
}

class SseClient(connection: ActorRef) extends Actor with ActorLogging {
  import JsonProtocol._

  initStream(connection)

  def receive = {
    case delta: Delta  => pushDelta(delta)
    case closedEvent: ConnectionClosed    => close(closedEvent.getErrorCause)
  }

  def pushDelta(delta: Delta): Unit = {
    import spray.json._
    connection ! MessageChunk("data: " + delta.toJson.toString() + "\n\n")
  }

  def initStream(connection: ActorRef): Unit = {
    val startMessage = HttpEntity(`text/event-stream`, "{}")
    val firstChunk = ChunkedResponseStart(HttpResponse(status = StatusCodes.OK, entity = startMessage)
      .withHeaders(`Access-Control-Allow-Origin-From-Everywhere`, `Cache-Control`(`no-cache`)))

    connection ! firstChunk
  }

  def close(reason: String): Unit = {
    log.debug("Shutting down SseClient due to: {}", reason)
    context stop self
  }
}

object SseClient {
  val `Access-Control-Allow-Origin-From-Everywhere` = RawHeader("Access-Control-Allow-Origin", "*")
  val `text/event-stream` = ContentType(MediaType.custom("text", "event-stream"), HttpCharsets.`UTF-8`)

  def props(connection: ActorRef): Props = Props(new SseClient(connection))
}

object HttpLauncher extends App with SimpleRoutingApp with SprayJsonSupport {
  import JsonProtocol._

  implicit val system = ActorSystem("game-engine")
  val playRoom = system.actorOf(Props[PlayRoom], "play-room")

  startServer(interface = "0.0.0.0", port = 9098) {
    post {
      path("player" / Segment / "state") { playerID =>
        entity(as[PlayerState]) { playerState =>
          complete {
            playRoom ! PlayerUpdate(playerID, playerState)
            StatusCodes.Accepted
          }
        }
      }
    } ~
    get {
      path("join") {
        parameters('player_id, 'initial_x.as[Int], 'initial_y.as[Int]) { (playerID, x, y) => ctx =>
          val sseClient = system.actorOf(Props(new SseClient(ctx.responder)))
          playRoom ! Join(sseClient, playerID, PlayerState(x, y))
        }
      } ~
      path("") {
        getFromResource("web/index.html")

      } ~ getFromResourceDirectory("web")
    }
  }
}
