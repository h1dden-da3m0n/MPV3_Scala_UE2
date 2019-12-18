package mpv.basics.actors.simple

import akka.actor.{ActorSystem, Props}

object SimplePrimeCalculatorApp extends App {
  println("======== SimplePrimeCalculatorApp ========")
  val system = ActorSystem("SimplePrimeCalcSystem")
  system.actorOf(Props[SimpleMainActor], "MainActor")

  //  Thread.sleep(1000)
  //  val done = system.terminate()
  //  Await.ready(done, Duration.Inf)
}
