package mpv.exercises.actors.communication

import akka.actor.{ActorSystem, Props}

import scala.concurrent.duration._

object ActorCommunicationApp extends App {
  println("======== ActorCommunicationApp ========")
  val system = ActorSystem("ActorCommunicationSystem")
  system.actorOf(Props(classOf[MsgSender], 4, 250.milliseconds), "MsgSender")
}
