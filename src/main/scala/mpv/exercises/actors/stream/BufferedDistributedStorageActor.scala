package mpv.exercises.actors.stream

import akka.actor.{Actor, ActorRef, Props}
import mpv.exercises.actors.stream.BufferedDistributedStorageActor.WorkOrder

import scala.collection.mutable
import scala.concurrent.duration._


object BufferedDistributedStorageActor {

  import WeatherStationActor._

  def props(maxBufferSize: Int, bufferIdleTime: FiniteDuration, writeThrottle: FiniteDuration = 0.millis): Props =
    Props(new BufferedDistributedStorageActor(maxBufferSize, bufferIdleTime, writeThrottle))

  case class ScheduledPersistBuffer()

  case class PersistBuffer()

  case class WorkOrder(readings: Seq[WeatherReading])

}

/**
 * BufferedDistributedStorageActor exercise 2.3 d)
 *
 * @param maxBufferSize  the maximum size the buffer may reach before it gets persisted to a file
 * @param bufferIdleTime the maximum time the buffer hast to reach it max size before it gets persisted automatically
 * @param writeThrottle  the throttle time used on the Akka File IO stream
 */

class BufferedDistributedStorageActor(maxBufferSize: Int, bufferIdleTime: FiniteDuration,
                                      writeThrottle: FiniteDuration) extends Actor {

  import BufferedStorageActor._
  import PersistUtil.println
  import WeatherStationActor._
  import context.dispatcher

  private val workerCnt = Runtime.getRuntime.availableProcessors
  private val readingList = mutable.SortedSet.empty[WeatherReading]
  private val workerRefs = new Array[ActorRef](workerCnt)
  private var nextWorkerId = 0
  private var persistSchedule = context.system.scheduler.scheduleOnce(bufferIdleTime, self, ScheduledPersistBuffer())

  createChildren()

  private def createChildren(): Unit = {
    for (i <- 0 until workerCnt) {
      workerRefs(i) = context.actorOf(Props(classOf[PersistActor], writeThrottle), s"PersistActor_${i}")
    }
  }

  override def receive: Receive = {
    case msg: WeatherReading =>
      println(s"[${self.path.name}]: RECEIVED WeatherReading ...")
      readingList += msg
      if (readingList.size > maxBufferSize) {
        println(s"[${self.path.name}]: SENDING message to self to persist data ...")
        self ! PersistBuffer()
      }
    case _: PersistBuffer =>
      persistSchedule.cancel()
      println(s"[${self.path.name}]: RECEIVED Instruction to persist 2 file due to buffer size")
      sendWork2Worker()
      persistSchedule = context.system.scheduler.scheduleOnce(bufferIdleTime, self, ScheduledPersistBuffer())
    case _: ScheduledPersistBuffer =>
      println(s"[${self.path.name}]: RECEIVED Instruction to persist 2 file due to idling buffer")
      sendWork2Worker()
      persistSchedule = context.system.scheduler.scheduleOnce(bufferIdleTime, self, ScheduledPersistBuffer())
  }

  private def sendWork2Worker(): Unit = {
    println(s"[${self.path.name}]: SENDING WorkOrder to Worker $nextWorkerId ...")
    workerRefs(nextWorkerId) ! WorkOrder(readingList.toSeq)
    readingList.clear()
    nextWorkerId = (nextWorkerId + 1) % workerCnt
  }

  override def unhandled(message: Any): Unit = {
    println(s"[${self.path.name}]: UNHANDLED $message")
    super.unhandled(message)
  }
}
