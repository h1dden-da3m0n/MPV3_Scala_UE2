package mpv.exercises.actors.ask

import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}
import akka.util.Timeout

import scala.concurrent.Promise

object TemporaryAskActor {
  def props(promise: Promise[Any], msg: Any, target: ActorRef, timout: Timeout, sender: ActorRef): Props =
    Props(new TemporaryAskActor(promise, msg, target, timout, sender))
}

class TemporaryAskActor(promise: Promise[Any], msg: Any, target: ActorRef,
                        timeout: Timeout, sender: ActorRef) extends Actor {

  println(s"[${self.path.name}]: Came into existence as a TemporaryActor ...")
  target ! msg
  context.setReceiveTimeout(timeout.duration)
  println(s"[${self.path.name}]: Sent Ask Message to target waiting for response ...")

  override def receive: Receive = {
    case ReceiveTimeout =>
      println(s"[${self.path.name}]: Timout exceeded while waiting.")
      promise.failure(new RuntimeException("Receive timed out"))
      context.system.terminate()
    case msg: Any =>
      println(s"[${self.path.name}]: Received a response!")
      //      // incorrect behaviour according to documentation, raises the question why the sender param is even needed?
      //      if (sender != Actor.noSender) {
      //        sender ! msg
      //      }
      //      promise.success("Asking yielded a response!")
      promise.success(msg)
      context.system.terminate()
  }
}
