package mpv.exercises.actors.stream

import akka.actor.{Actor, Props}
import akka.stream.scaladsl.{FileIO, Flow, Source}
import akka.util.ByteString

import scala.collection.mutable
import scala.concurrent.duration._

object BufferedStorageActor {
  def props(maxBufferSize: Int, bufferIdleTime: FiniteDuration, writeThrottle: FiniteDuration = 1.millis): Props =
    Props(new BufferedStorageActor(maxBufferSize, bufferIdleTime, writeThrottle))

  case class ScheduledPersistBuffer()

  case class PersistBuffer()

}

/**
 * BufferedStorageActor exercise 2.3 c)
 *
 * @param maxBufferSize  the maximum size the buffer may reach before it gets persisted to a file
 * @param bufferIdleTime the maximum time the buffer hast to reach it max size before it gets persisted automatically
 * @param writeThrottle  the throttle time used on the Akka File IO stream
 */
class BufferedStorageActor(maxBufferSize: Int, bufferIdleTime: FiniteDuration,
                           writeThrottle: FiniteDuration) extends Actor {

  import java.nio.file.{OpenOption, Paths, StandardOpenOption}

  import BufferedStorageActor._
  import WeatherStationActor._
  import context.{dispatcher, system}

  private val weatherCsv = Paths.get("weatherData.csv")
  private val fileOpts = Set[OpenOption](StandardOpenOption.WRITE,
    StandardOpenOption.APPEND, StandardOpenOption.CREATE)
  private val readingList = mutable.Set.empty[WeatherReading]
  private var persistSchedule = context.system.scheduler.scheduleOnce(bufferIdleTime, self, ScheduledPersistBuffer())

  private def persistBuffer2File(): Unit = {
    val source = Source(readingList.toSet)
    val flow = Flow[WeatherReading].map(x => ByteString(f"${x.timestamp},${x.temperature}%.2f\n"))
    source.throttle(1, writeThrottle)
      .via(flow)
      .runWith(FileIO.toPath(weatherCsv, fileOpts))
    readingList.clear()
  }

  override def receive: Receive = {
    case msg: WeatherReading =>
      readingList += msg
      if (readingList.size > maxBufferSize) {
        println(s"[${self.path.name}]: SENDING messaging self to persist data ...")
        self ! PersistBuffer()
      }
    case _: PersistBuffer =>
      persistSchedule.cancel()
      println(s"[${self.path.name}]: RECEIVED Instruction to persist 2 file due to buffer size")
      persistBuffer2File()
      persistSchedule = context.system.scheduler.scheduleOnce(bufferIdleTime, self, ScheduledPersistBuffer())
    case _: ScheduledPersistBuffer =>
      println(s"[${self.path.name}]: RECEIVED Instruction to persist 2 file due to idling buffer")
      persistBuffer2File()
      persistSchedule = context.system.scheduler.scheduleOnce(bufferIdleTime, self, ScheduledPersistBuffer())
  }
}
