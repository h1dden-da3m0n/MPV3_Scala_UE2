package mpv.exercises.actors.stream

import akka.actor.{ActorSystem, Props}

import scala.concurrent.Await
import scala.concurrent.duration._

object WeatherStationApp extends App {

  private def test23(readSpeed: FiniteDuration, props: Props, dataProcessorName: String): Unit = {
    val system = ActorSystem("WeatherStationSystem")

    val dataProcessor = system.actorOf(props, dataProcessorName)
    system.actorOf(
      WeatherStationActor.props(dataProcessor, readRange = (8.0, 16.0), readSpeed = readSpeed),
      "WeatherStation")

    Thread.sleep(8000)
    val done = system.terminate()
    Await.ready(done, Duration.Inf)
    Thread.sleep(50)
  }

  println("======== WeatherStationApp ========")
  println("---- Testing StorageActor 2.3b ----")
  test23(200.millis, StorageActor.props(), "StorageActor")

  println("---- Testing BufferedStorageActor 2.3c ----")
  test23(200.millis, BufferedStorageActor.props(4, 2.2.seconds), "BufferedStorageActor")

  println("---- Testing BufferedDistributedStorageActor 2.3d ----")
  test23(200.millis, BufferedDistributedStorageActor.props(4, 2.2.second),
    "BufferedDistributedStorageActor")
}
