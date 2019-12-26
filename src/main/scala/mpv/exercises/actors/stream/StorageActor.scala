package mpv.exercises.actors.stream

import akka.actor.{Actor, Props}
import akka.stream.scaladsl.{FileIO, Source}

import scala.concurrent.duration._

object StorageActor {
  def props(writeThrottle: FiniteDuration = 1.millis): Props =
    Props(new StorageActor(writeThrottle))
}

/**
 * StorageActor exercise 2.3 b)
 *
 * @param writeThrottle the throttle time used on the Akka File IO stream
 */
class StorageActor(writeThrottle: FiniteDuration) extends Actor {

  import java.nio.file.Paths

  import PersistUtil._
  import WeatherStationActor._
  import context.system

  private val weatherCsv = Paths.get("weatherData.csv")

  override def receive: Receive = {
    case msg: WeatherReading =>
      val source = Source.single(msg)
      source.throttle(1, writeThrottle)
        .via(weatherFlow)
        .runWith(FileIO.toPath(weatherCsv, fileOpts))
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
