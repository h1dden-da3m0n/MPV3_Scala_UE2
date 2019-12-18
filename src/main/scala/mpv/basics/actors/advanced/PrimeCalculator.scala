package mpv.basics.actors.advanced

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated}

import scala.collection.mutable

object PrimeCalculator {
  def isPrime(n: Long): Boolean = {
    val upper = math.sqrt(n + 1).toInt
    (2 to upper) forall (i => n % i != 0)
  }

  case class Find(lower: Int, upper: Int)

  case class Found(lower: Int, upper: Int, primes: Seq[Int])

  case class Failed(lower: Int, upper: Int)

}

class PrimeCalculator(lower: Int, upper: Int) extends Actor {

  import PrimeCalculator._
  import RangeUtil._

  private val workerCnt = Runtime.getRuntime.availableProcessors;
  private var nWorkers = 0;
  private val primes = mutable.SortedSet.empty[Int]
  private val completed = mutable.Set.empty[ActorRef]

  createChildren()
  registerDeathWatch()

  private def createChildren(): Unit = {
    for ((l, u) <- splitIntoIntervals(lower, upper, workerCnt)) {
      context.actorOf(Props(classOf[PrimeFinder], l, u), s"PrimeFinder_${l}_${u}")
    }
    nWorkers = context.children.size
  }

  private def registerDeathWatch(): Unit = {
    context.children foreach context.watch
  }

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy(2) {
    case _: ArithmeticException => Restart
    case ex => SupervisorStrategy.defaultStrategy.decider(ex)
  }

  override def receive: Receive = {
    case Found(l, u, p) =>
      primes ++= p
      completed += sender
      if (completed.size >= nWorkers) {
        context.parent ! Found(lower, upper, primes.toSeq)
        context.stop(self)
      }
    case Terminated(actor) =>
      if (!completed.contains(actor)) {
        context.parent ! Failed(lower, upper)
        context.stop(self)
      }
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
