package mpv.basics.actors.advanced

import akka.actor.Actor
import akka.pattern.pipe

import scala.concurrent.Future
import scala.util.Random

class PrimeFinder(lower: Int, upper: Int) extends Actor {

  import PrimeCalculator._
  import context.dispatcher

  Future {
    (lower to upper) filter (isPrime(_))
  } pipeTo self

  private def failSometimes(probability: Double): Unit = {
    if (Random.nextDouble() <= probability) {
      println(s"${self.path.name} FAILED (ThreadId=${Thread.currentThread.getId})")
      throw new ArithmeticException("I Failed Sometimes in PrimeFinder")
    }
  }

  override def postRestart(reason: Throwable): Unit = {
    println(s"${self.path.name} RESTARTED")
    super.postRestart(reason)
  }

  override def receive: Receive = {
    case primes: Seq[_] =>
      failSometimes(0.1)

      println(s"${self.path.name} SUCCEEDED (ThreadId=${Thread.currentThread.getId}): ${primes.mkString("[", ",", "]")}")
      context.parent ! Found(lower, upper, primes.asInstanceOf[Seq[Int]])
      context.stop(self)
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
