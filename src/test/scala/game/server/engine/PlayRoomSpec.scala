package game.server.engine

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestProbe, TestKit}
import game.server.engine.PlayRoom.Join
import game.server.engine.PlayerSync.{PlayerDelta, Delta}
import org.scalatest.{WordSpecLike, Matchers, WordSpec}
import scala.concurrent.duration._

class PlayRoomSpec extends TestKit(ActorSystem("play-room-spec")) with WordSpecLike with Matchers {

  "play room should dispatch status updates to clients" in {
    val playRoom = system.actorOf(Props[PlayRoom], "play-room")
    val player1 = TestProbe()
    val player2 = TestProbe()

    playRoom ! Join(player1.ref, "1", PlayerState(10, 10))
    playRoom ! Join(player2.ref, "2", PlayerState(20, 20))

    within(10 seconds) {
      player1.expectMsg(Delta(2, Map("2" -> PlayerDelta(Some(20), Some(20)))))

    }
  }
}
