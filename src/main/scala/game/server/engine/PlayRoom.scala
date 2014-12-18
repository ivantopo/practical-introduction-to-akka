package game.server.engine

import akka.actor.{ActorRef, Actor}
import game.server.engine.PlayRoom.{Join, PlayerUpdate}

class PlayRoom extends Actor {
  var masterState = GameState.EmptyState

  def receive = {
    case Join(client, playerID, state)          => join(client, playerID, state)
    case PlayerUpdate(playerID, newPlayerState) => processPlayerUpdate(playerID, newPlayerState)

  }

  def processPlayerUpdate(playerID: String, newPlayerState: PlayerState): Unit = {
    masterState = GameState(masterState.players.updated(playerID, newPlayerState))
    context.children.foreach(_ ! masterState)
  }

  def join(newClient: ActorRef, playerID: String, newPlayerState: PlayerState): Unit = {
    context.actorOf(PlayerSync.props(newClient, playerID))
    processPlayerUpdate(playerID, newPlayerState)
  }

}

object PlayRoom {
  case class PlayerUpdate(playerID: String, newState: PlayerState)
  case class Join(client: ActorRef, playerID: String, state: PlayerState)
}

case class PlayerState(xPosition: Int, yPosition: Int)
case class GameState(players: Map[String, PlayerState])

object GameState {
  val EmptyState = GameState(Map.empty)
}


