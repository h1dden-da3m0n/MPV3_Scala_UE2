package mpv.exercises.actors.stream

import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration._

object WeatherStationApp extends App {
  println("======== WeatherStationApp ========")
  val system = ActorSystem("WeatherStationSystem")

  //val dataProcessor = system.actorOf(StorageActor.props(500.millis), "StorageActor")
  val dataProcessor = system.actorOf(BufferedStorageActor.props(4, 2.2.seconds), "StorageActor")
  system.actorOf(WeatherStationActor.props(dataProcessor, readRange = (10.0, 16.0)), "WeatherStation")

  Thread.sleep(16000)
  val done = system.terminate()
  Await.ready(done, Duration.Inf)
}
