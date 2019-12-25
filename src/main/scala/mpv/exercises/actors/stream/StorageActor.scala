package mpv.exercises.actors.stream

import akka.actor.{Actor, Props}
import akka.stream.scaladsl.{FileIO, Flow, Source}
import akka.util.ByteString

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

  import java.nio.file.{OpenOption, Paths, StandardOpenOption}

  import WeatherStationActor._
  import context.system

  private val weatherCsv = Paths.get("weatherData.csv")
  private val fileOpts = Set[OpenOption](StandardOpenOption.WRITE,
    StandardOpenOption.APPEND, StandardOpenOption.CREATE)

  override def receive: Receive = {
    case msg: WeatherReading =>
      val source = Source.single(msg)
      val flow = Flow[WeatherReading].map(x => ByteString(f"${x.timestamp},${x.temperature}%.2f\n"))
      source.throttle(1, writeThrottle)
        .via(flow)
        .runWith(FileIO.toPath(weatherCsv, fileOpts))
  }
}
