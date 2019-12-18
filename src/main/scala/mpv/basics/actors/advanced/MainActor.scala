package mpv.basics.actors.advanced

import akka.actor.{Actor, Props, Terminated}

class MainActor extends Actor {

  import PrimeCalculator._

  //  context.actorOf(Props(classOf[PrimeCalculator], 2, 100), "PrimeCalc1")
  context.actorOf(Props(classOf[PrimeCalculator], 1000, 1200), "PrimeCalc2")

  context.children foreach context.watch

  override def receive: Receive = {
    case Found(l, u, p) =>
      println(s"primes in [$l, $u]: ${p.mkString("[", ",", "]")}")
    case Failed(l, u) =>
      println(s"primes in [$l, $u]: computation FAILED")
    case Terminated(actor) =>
      if (context.children.isEmpty) {
        context.system.terminate()
      }
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
