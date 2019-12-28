package mpv.exercises.actors.ask

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.util.Timeout

import scala.concurrent.{Future, Promise}

class ActorRefExtension(val actorRef: ActorRef) {
  private val system = ActorSystem("TemporarySystem")

  def ?(msg: Any)
       (implicit timeout: Timeout, sender: ActorRef = Actor.noSender): Future[Any] = {
    val promise = Promise[Any]()
    system.actorOf(TemporaryAskActor.props(promise, msg, actorRef, timeout, sender), "TemporaryAskActor")
    promise.future
  }
}
