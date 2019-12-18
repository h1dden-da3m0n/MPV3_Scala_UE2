package mpv.basics.actors.simple

import akka.actor.Actor


object SimplePrimeCalculator {
  def isPrime(n: Long): Boolean = {
    val upper = math.sqrt(n + 1).toInt
    (2 to upper) forall (i => n % i != 0)
  }

  case class Find(lower: Int, upper: Int)

  case class Found(lower: Int, upper: Int, primes: Seq[Int])

}

class SimplePrimeCalculator extends Actor {

  import SimplePrimeCalculator._

  println("SimplePrimeCalculator created")

  override def receive: Receive = {
    //    case message: String =>
    //      println(s"[${self.path.name}]: I receive so I am! ($message)")
    //      sender ! "Greetings back from the PrimeCalculator"
    case Find(l, u) =>
      val primes = (l to u) filter (isPrime(_))
      sender ! Found(l, u, primes)
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
