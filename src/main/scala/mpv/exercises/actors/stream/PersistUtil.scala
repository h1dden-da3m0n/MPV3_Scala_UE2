package mpv.exercises.actors.stream

import java.nio.file.{OpenOption, StandardOpenOption}

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import mpv.exercises.actors.stream.WeatherStationActor.WeatherReading

object PersistUtil {

  private val EPS = 0.5

  def weatherFlow: Flow[WeatherReading, ByteString, NotUsed] =
    Flow[WeatherReading].map(x => ByteString(f"${x.timestamp},${x.temperature}%.2f\n"))

  def fileOpts: Set[OpenOption] = Set(StandardOpenOption.WRITE,
    StandardOpenOption.APPEND, StandardOpenOption.CREATE)

  def println(x: Any): Unit = Console.println(s"$x (thread id=${Thread.currentThread.getId})")
}
