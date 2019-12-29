package mpv.exercises.actors.stream

import java.time.LocalDateTime

import akka.actor.{Actor, ActorRef, Props}
import mpv.exercises.actors.stream.PersistUtil.println

import scala.concurrent.duration._
import scala.util.Random

object WeatherStationActor {
  def props(dataProcessor: ActorRef,
            readSpeed: FiniteDuration = 500.millis,
            readRange: (Double, Double) = (0.0, 32.0)): Props =
    Props(new WeatherStationActor(dataProcessor, readSpeed, readRange))

  case class WeatherProbe()

  case class WeatherReading(timestamp: LocalDateTime, temperature: Double) extends Ordered[WeatherReading] {

    import scala.math.Ordered.orderingToOrdered

    def compare(that: WeatherReading): Int = this.timestamp compare that.timestamp
  }

}

/**
 * WeatherStationActor exercise 2.3 a)
 *
 * @param dataProcessor a actor reference to the data processing actor (eg.: StorageActor)
 * @param readSpeed     the duration between weather probe readings
 * @param readRange     the range in which the random temperature values fluctuate
 */
class WeatherStationActor(dataProcessor: ActorRef, readSpeed: FiniteDuration,
                          readRange: (Double, Double)) extends Actor {

  import WeatherStationActor._
  import context.dispatcher

  private var probeCnt: Long = 0
  println(s"[${self.path.name}]: CREATED! Scheduling weather readings ...")

  context.system.scheduler.scheduleAtFixedRate(readSpeed, readSpeed, self, WeatherProbe())
  context watch dataProcessor

  override def receive: Receive = {
    case msg: WeatherProbe =>
      println(f"[${self.path.name}]: PROBING #$probeCnt%02d Reading weather data and transmitting to processor ...")
      dataProcessor ! WeatherReading(LocalDateTime.now(), Random.between(readRange._1, readRange._2))
      probeCnt += 1
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
