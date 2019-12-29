package mpv.exercises.actors.stream

import java.nio.file.Paths

import akka.actor.Actor
import akka.stream.scaladsl.{FileIO, Source}
import mpv.exercises.actors.stream.PersistUtil.{fileOpts, weatherFlow}

import scala.concurrent.duration.FiniteDuration

class PersistActor(writeThrottle: FiniteDuration) extends Actor {

  import BufferedDistributedStorageActor._
  import context.system

  private val weatherCsv = Paths.get("weatherData_23d.csv")

  override def receive: Receive = {
    case msg: WorkOrder =>
      println(s"[${self.path.name}]: RECEIVED WorkOrder. Starting to persist data ...")
      val source = Source(msg.readings.toSet)
      if (writeThrottle.length > 0) {
        source.throttle(1, writeThrottle)
          .via(weatherFlow)
          .runWith(FileIO.toPath(weatherCsv, fileOpts))
      }
      else {
        source.via(weatherFlow)
          .runWith(FileIO.toPath(weatherCsv, fileOpts))
      }
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
