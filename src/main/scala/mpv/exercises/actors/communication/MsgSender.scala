package mpv.exercises.actors.communication

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Random

object MsgSender {

  def props(retryCont: Int, msgCnt: Int = 8, maxReceiptTimout: FiniteDuration = 250.millis): Props =
    Props(new MsgSender(retryCont, msgCnt, maxReceiptTimout))

  case class MsgStatusCheck()

}

class MsgSender(retryCont: Int, msgCnt: Int, maxReceiptTimout: FiniteDuration) extends Actor {

  import MsgReceiver._
  import MsgSender._
  import context.dispatcher

  private val failProbability = 0.7
  private val msgReceiver: ActorRef = context.actorOf(Props[MsgReceiver], "MsgReceiver")
  private val sentMassages = mutable.Set.empty[SimpleMessage]
  println(s"[${self.path.name}]: CREATED! Start sending ...")
  for (i <- 0 to msgCnt) {
    val msg = SimpleMessage(i, "Your advertisement could be placed here")
    msgReceiver ! msg
    sentMassages += msg
  }

  context.system.scheduler.scheduleAtFixedRate(maxReceiptTimout, maxReceiptTimout, self, MsgStatusCheck())
  context watch msgReceiver

  override def receive: Receive = {
    case msg: SimpleReceipt =>
      if (Random.nextDouble() <= failProbability) {
        println(s"[${self.path.name}]: RECIVED {receiptId: ${msg.capsuled.id}}")
        sentMassages -= msg.capsuled
      }
    case msg: MsgStatusCheck =>
      println(s"[${self.path.name}]: RECIVED MsgStatusCheck")
      if (sentMassages.nonEmpty) {
        for (sent <- sentMassages) {
          msgReceiver ! sent
        }
      }
      else {
        println(s"[${self.path.name}]: All Messages delivered! Shutting down ...")
        msgReceiver ! PoisonPill
      }
    case actor: Terminated =>
      context.system.terminate()
  }

  override def unhandled(message: Any): Unit = {
    println(s"class = ${message.getClass}")
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
