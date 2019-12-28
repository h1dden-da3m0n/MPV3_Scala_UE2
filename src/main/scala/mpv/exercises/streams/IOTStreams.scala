package mpv.exercises.streams

import java.nio.file.Paths
import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.scaladsl.{FileIO, Flow, Source}
import akka.util.ByteString
import mpv.exercises.actors.stream.PersistUtil.fileOpts

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

object IOTStreams extends App {

  implicit val system: ActorSystem = ActorSystem("IOTStreamsSystem")

  import system.dispatcher

  private val weatherCsv = Paths.get("weatherData.csv")
  private val coreCnt = Runtime.getRuntime.availableProcessors

  case class WeatherReading(timestamp: LocalDateTime, temperature: Double)

  def generateWeatherSource(readSpeed: FiniteDuration,
                            readRange: (Double, Double)): Source[WeatherReading, Cancellable] = {
    Source.tick(readSpeed, readSpeed, "genData").map { _ =>
      WeatherReading(LocalDateTime.now(), Random.between(readRange._1, readRange._2))
    }
  }

  def weatherReadingMap(reading: WeatherReading): ByteString = {
    ByteString(f"${reading.timestamp},${reading.temperature}%.2f\n")
  }

  def generateWeatherFlow(throttle: FiniteDuration = 0.millis): Flow[WeatherReading, ByteString, NotUsed] = {
    if (throttle > 0.millis) {
      Flow[WeatherReading].throttle(1, throttle).map(weatherReadingMap)
    }
    else {
      Flow[WeatherReading].map(weatherReadingMap)
    }
  }

  def generateBulkAsyncWeatherFlow(throttle: FiniteDuration = 0.millis,
                                   bulkSize: Int = 4): Flow[WeatherReading, ByteString, NotUsed] = {
    if (throttle > 0.millis) {
      Flow[WeatherReading].throttle(1, throttle).mapAsync(coreCnt)(x => Future {
        weatherReadingMap(x)
      })
    }
    else {
      Flow[WeatherReading].mapAsync(coreCnt)(x => Future {
        weatherReadingMap(x)
      })
    }
  }

  generateWeatherSource(250.millis, (12.0, 20.0))
    .via(generateWeatherFlow())
    .to(FileIO.toPath(weatherCsv, fileOpts))
    .run()

  Thread.sleep(2000)
  system.terminate()
}
