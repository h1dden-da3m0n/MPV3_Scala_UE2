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
    val coreCnt = Runtime.getRuntime.availableProcessors

    def buildList(list: Seq[WeatherReading], elem: WeatherReading): Seq[WeatherReading] = {
      list :+ elem
    }

    def weatherReadingSeqMap(readingSeq: Seq[WeatherReading]): Future[ByteString] = {
      Future {
        println(s"Flow: MAPPING ${readingSeq.size} WeatherReading(s) 2 ByteString ...")
        ByteString(readingSeq.map(weatherReading2String).mkString(""))
      }
    }

    def weatherReading2String(reading: WeatherReading): String = {
      f"${reading.timestamp},${reading.temperature}%.2f\n"
    }

    if (throttle > 0.millis) {
      Flow[WeatherReading].batch(bulkSize, buildList(Seq.empty[WeatherReading], _))(buildList)
        .throttle(1, throttle)
        .mapAsync(coreCnt)(weatherReadingSeqMap)
    }
    else {
      Flow[WeatherReading].batch(bulkSize, buildList(Seq.empty[WeatherReading], _))(buildList)
        .mapAsync(coreCnt)(weatherReadingSeqMap)
    }
  }

  def testStream(testName: String, fileName: String,
                 source: => Source[WeatherReading, Cancellable],
                 flow: => Flow[WeatherReading, ByteString, NotUsed]): Unit = {
    val weatherCsv = Paths.get(fileName)
    print(s"---- Testing $testName ----\n")

    val f = source.limit(38).via(flow).runWith(FileIO.toPath(weatherCsv, fileOpts))

    Await.ready(f, Duration.Inf)
    Thread.sleep(50)
    print("\n\n")
  }

  print("==== IOTAkkaStreams ====\n")
  testStream("Default Flow", "weatherData_24b.csv",
    generateWeatherSource(200.millis, (12.0, 20.0)), generateWeatherFlow(1.second))
  testStream("Async Flow", "weatherData_24c.csv",
    generateWeatherSource(200.millis, (12.0, 20.0)), generateBulkAsyncWeatherFlow(1.second))

  Thread.sleep(200)
  system.terminate()
}
