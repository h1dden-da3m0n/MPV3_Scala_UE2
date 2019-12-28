package mpv.exercises.actors.ask

import akka.actor.Actor

import scala.concurrent.duration.FiniteDuration

object TestActor {

  case class SomeMsg()

}

class TestActor(workDuration: FiniteDuration) extends Actor {

  import TestActor._

  override def receive: Receive = {
    case _: Any =>
      println(s"[${self.path.name}]: Received a message! Starting work ...")
      Thread.sleep(workDuration.toMillis)
      println(s"[${self.path.name}]: Work finished. Answering ...")
      sender ! SomeMsg()
  }
}
