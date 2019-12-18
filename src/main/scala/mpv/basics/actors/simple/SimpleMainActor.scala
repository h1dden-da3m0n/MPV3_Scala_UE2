package mpv.basics.actors.simple

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}

class SimpleMainActor extends Actor {
  println("SimpleMainActor created")

  import SimplePrimeCalculator._

  val primeCalc: ActorRef = context.actorOf(Props[SimplePrimeCalculator], "SimplePrimeCalc")
  //  primeCalc.tell("Main actor sends its greetings!", self)
  //  primeCalc ! "Main actor sends its greetings!"
  //  primeCalc ! 42.42
  primeCalc ! Find(2, 100)
  primeCalc ! Find(1000, 1024)
  primeCalc ! PoisonPill

  context watch primeCalc

  override def receive: Receive = {
    //    case message: String => println(s"[${self.path.name}]: I receive so I am! ($message)")
    case Found(l, u, p) =>
      println(s"primes in [$l, $u]: [${p.mkString(",")}]")
    case Terminated(actor) =>
      context.system.terminate()
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
