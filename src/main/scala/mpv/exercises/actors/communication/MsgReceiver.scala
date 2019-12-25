package mpv.exercises.actors.communication

import akka.actor.Actor
import akka.http.javadsl.model.DateTime

import scala.collection.mutable
import scala.util.Random

object MsgReceiver {

  case class SimpleMessage(id: Long, msg: String)

  case class SimpleReceipt(capsuled: SimpleMessage)

}

class MsgReceiver extends Actor {

  import MsgReceiver._

  private val successProbability = 0.7
  private val handledMsgIds = mutable.SortedSet.empty[Long]
  println(s"[${self.path.name}]: CREATED! Becoming sentient ...")

  override def receive: Receive = {
    case msg: SimpleMessage =>
      if (Random.nextDouble() <= successProbability) {
        if (!handledMsgIds.contains(msg.id)) {
          println(s"[${self.path.name}]: RECIEVED @ ${DateTime.now()} {id: ${msg.id}, msg: '${msg.msg}'}")
          handledMsgIds += msg.id
        }
        sender ! SimpleReceipt(msg)
      }
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
