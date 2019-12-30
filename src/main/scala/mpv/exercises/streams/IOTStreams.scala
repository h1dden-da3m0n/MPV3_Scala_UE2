package mpv.exercises.streams

import java.nio.file.Paths
import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.scaladsl.{FileIO, Flow, Source}
import akka.util.ByteString
import mpv.exercises.actors.stream.PersistUtil.{fileOpts, println}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

object IOTStreams extends App {

  implicit val system: ActorSystem = ActorSystem("IOTStreamsSystem")

  import system.dispatcher

  private val coreCnt = Runtime.getRuntime.availableProcessors

  case class WeatherReading(timestamp: LocalDateTime, temperature: Double)

  def generateWeatherSource(readSpeed: FiniteDuration,
                            readRange: (Double, Double)): Source[WeatherReading, Cancellable] = {
    Source.tick(readSpeed, readSpeed, "genData").map { _ =>
      println("Source: GENERATING WeatherReading ...")
      WeatherReading(LocalDateTime.now(), Random.between(readRange._1, readRange._2))
    }
  }

  def weatherReadingMap(reading: WeatherReading): ByteString = {
    println("Flow: MAPPING WeatherReading 2 ByteString ...")
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
    def weatherReadingMap(reading: WeatherReading): String = {
      println("Flow: MAPPING WeatherReading(s) 2 ByteString ...")
      f"${reading.timestamp},${reading.temperature}%.2f"
    }

    def buildList(list: Seq[WeatherReading], elem: WeatherReading): Seq[WeatherReading] = {
      list :+ elem
    }

    if (throttle > 0.millis) {
      Flow[WeatherReading].batch(bulkSize, buildList(Seq.empty[WeatherReading], _)) {
        (readingSeq, reading) => buildList(readingSeq, reading)
      }
        .throttle(1, throttle)
        .mapAsync(coreCnt)(readingSeq => Future {
          ByteString(readingSeq.map(weatherReadingMap).mkString("\n"))
        })
    }
    else {
      Flow[WeatherReading].batch(bulkSize, buildList(Seq.empty[WeatherReading], _)) {
        (readingSeq, reading) => buildList(readingSeq, reading)
      }
        .mapAsync(coreCnt)(readingSeq => Future {
          ByteString(readingSeq.map(weatherReadingMap).mkString("\n"))
        })
    }
  }

  def testStream(testName: String, fileName: String,
                 source: => Source[WeatherReading, Cancellable],
                 flow: => Flow[WeatherReading, ByteString, NotUsed]): Unit = {
    val weatherCsv = Paths.get(fileName)
    print(s"---- Testing $testName ----\n")

    val f = source.limit(32).via(flow).runWith(FileIO.toPath(weatherCsv, fileOpts))

    Await.ready(f, Duration.Inf)
    Thread.sleep(50)
    print("\n\n")
  }

  print("==== IOTAkkaStreams ====\n")
  testStream("Default Flow", "weatherData_24b.csv",
    generateWeatherSource(250.millis, (12.0, 20.0)), generateWeatherFlow(1.second))
  testStream("Async Flow", "weatherData_24c.csv",
    generateWeatherSource(250.millis, (12.0, 20.0)), generateBulkAsyncWeatherFlow(1.second))

  Thread.sleep(200)
  system.terminate()
}
