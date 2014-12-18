package game.server.engine

import akka.actor.{ActorLogging, Props, Actor, ActorRef}
import game.server.engine.PlayRoom.PlayerAck
import game.server.engine.PlayerSync.{ PlayerDelta, Snapshot, Delta}

class PlayerSync(playerClient: ActorRef, playerID: String) extends Actor with ActorLogging {
  log.info("New player {} connected.", playerID)

  var sequenceNumber = 0L
  var snapshots = Vector.empty[Snapshot]

  def receive = {
    case newState: GameState => dispatchGameStateDeltas(newState)
    case PlayerAck(_, ackedSequence) => ack(ackedSequence)
  }

  def dispatchGameStateDeltas(newGameState: GameState): Unit = {
    val sequence = nextSequence()
    registerSnapshot(sequence, newGameState)

    val changes = diff(lastAcknowledgedState, newGameState)
    if(changes.nonEmpty)
      playerClient ! Delta(sequence, changes)
  }

  def nextSequence(): Long = {
    sequenceNumber += 1
    sequenceNumber
  }

  def ack(sequence: Long): Unit = {
    snapshots.find(_.sequence == sequence) map(_.acked = true)
  }

  def registerSnapshot(sequence: Long, state: GameState): Unit =
    snapshots = snapshots :+ Snapshot(sequence, state, false)

  def lastAcknowledgedState: GameState =
    snapshots.collectFirst { case snap if(snap.acked) => snap.gameState } getOrElse(GameState.EmptyState)

  def cleanOldSnapshots(): Unit =
    snapshots = snapshots.dropWhile(_.sequence - sequenceNumber > 30)

  def diff(oldState: GameState, newState: GameState): Map[String, PlayerDelta] = {
    val changes = Map.newBuilder[String, PlayerDelta]

    newState.players.filterKeys(_ != playerID).foreach {
      case (newPlayerID, newPlayerState) if(playerID != newPlayerID) =>
        oldState.players.get(newPlayerID) map { oldState =>
          val xUpdate = if(newPlayerState.xPosition != oldState.xPosition) Some(newPlayerState.xPosition) else None
          val yUpdate = if(newPlayerState.yPosition != oldState.yPosition) Some(newPlayerState.yPosition) else None

          if(xUpdate.nonEmpty || yUpdate.nonEmpty)
            changes += (newPlayerID -> PlayerDelta(xUpdate, yUpdate))


        } getOrElse  {
          changes += (newPlayerID -> PlayerDelta(Some(newPlayerState.xPosition), Some(newPlayerState.yPosition)))
        }
    }

    changes.result()
  }

}

object PlayerSync {
  def props(playerClient: ActorRef, playerName: String): Props = Props(new PlayerSync(playerClient, playerName))

  case class PlayerDelta(xPosition: Option[Int], yPosition: Option[Int])
  case class Snapshot(sequence: Long, gameState: GameState, var acked: Boolean)
  case class Delta(sequence: Long, changes: Map[String, PlayerDelta])
}
