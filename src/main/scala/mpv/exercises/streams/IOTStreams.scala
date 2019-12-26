package mpv.exercises.streams

import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.util.Random

object IOTStreams extends App {

  implicit val system: ActorSystem = ActorSystem("IOTStreamsSystem")

  case class WeatherReading(timestamp: LocalDateTime, temperature: Double)

  def generateWeatherSource(readSpeed: FiniteDuration,
                            readRange: (Double, Double)): Source[WeatherReading, Cancellable] = {
    Source.tick(readSpeed, readSpeed, WeatherReading(LocalDateTime.now(), Random.between(readRange._1, readRange._2)))
  }

  def weatherReadingMap(reading: WeatherReading): ByteString = {
    ByteString(f"${reading.timestamp},${reading.temperature}%.2f\n")
  }

  def generateWeatherFlow(throttle: FiniteDuration): Flow[WeatherReading, ByteString, NotUsed] = {
    if (throttle > 0.millis) {
      Flow[WeatherReading].throttle(1, throttle).map(weatherReadingMap)
    }
    else {
      Flow[WeatherReading].map(weatherReadingMap)
    }
  }

  system.terminate()
}
