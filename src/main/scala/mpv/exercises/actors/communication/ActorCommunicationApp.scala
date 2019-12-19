package mpv.exercises.actors.communication

import akka.actor.ActorSystem

object ActorCommunicationApp extends App {
  println("======== ActorCommunicationApp ========")
  val system = ActorSystem("ActorCommunicationSystem")
  system.actorOf(MsgSender.props(3), "MsgSender")
}
