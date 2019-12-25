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

  private val successProbability = 0.7
  private val msgReceiver: ActorRef = context.actorOf(Props[MsgReceiver], "MsgReceiver")
  private val sentMassages = mutable.HashMap.empty[SimpleMessage, Int]
  private val undeliverableMsgs = mutable.Set.empty[SimpleMessage]
  println(s"[${self.path.name}]: CREATED! Start sending ...")
  for (i <- 0 to msgCnt) {
    val msg = SimpleMessage(i, "Your advertisement could be placed here")
    msgReceiver ! msg
    sentMassages += (msg -> 0)
  }

  context.system.scheduler.scheduleAtFixedRate(maxReceiptTimout, maxReceiptTimout, self, MsgStatusCheck())
  context watch msgReceiver

  override def receive: Receive = {
    case msg: SimpleReceipt =>
      if (Random.nextDouble() <= successProbability) {
        println(s"[${self.path.name}]: RECIVED {receiptId: ${msg.capsuled.id}}")
        sentMassages -= msg.capsuled
      }
    case msg: MsgStatusCheck =>
      println(s"[${self.path.name}]: RECIVED MsgStatusCheck")
      if (sentMassages.nonEmpty) {
        undeliverableMsgs ++= sentMassages.filter(x => x._2 >= retryCont).keys
        for (sent <- sentMassages.filter(x => x._2 < retryCont).keys) {
          println(s"[${self.path.name}]: REDELIVER {msgId: ${sent.id}, retriesLeft: ${retryCont - sentMassages(sent)}}")
          sentMassages(sent) += 1
          msgReceiver ! sent
        }
        sentMassages --= undeliverableMsgs
      }
      else if (undeliverableMsgs.isEmpty) {
        println(s"[${self.path.name}]: All Messages delivered! Shutting down ...")
        msgReceiver ! PoisonPill
      }
      else {
        println(s"[${self.path.name}]: Not all Messages could get delivered after $retryCont attempts:")
        println(s"[${self.path.name}]: undeliverableMessages = ${undeliverableMsgs.mkString("[", ", ", "]")}")
        println(s"[${self.path.name}]: Shutting down anyway ...")
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
