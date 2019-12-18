package mpv.basics.actors.advanced

import akka.actor.{ActorSystem, Props}

object PrimeCalculatorApp extends App {
  println("====== PrimeCalculatorApp ======")
  val system = ActorSystem("PrimeCalcSystem")

  system.actorOf(Props[MainActor], "MainActor")

  //  Thread.sleep(2000)
  //  val done = system.terminate()
  //  Await.ready(done, Duration.Inf)
}
