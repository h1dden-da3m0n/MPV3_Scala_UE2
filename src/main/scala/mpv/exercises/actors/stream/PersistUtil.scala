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

  /**
   * Adaptation of function from RangeUtil to split up the persistence buffer into smaller segments
   *
   * @param elementCnt the size of the persistence buffer
   * @param coreCnt    the core count of the executing system
   * @return sequence of index interval tuples
   */
  def splitIntoIntervals(elementCnt: Int, coreCnt: Int): Seq[(Int, Int)] = {
    val d = math.max(elementCnt / coreCnt.toDouble, 1)
    val steps = BigDecimal(0) to (elementCnt - EPS) by d

    val s = steps.size
    val intervals =
      for (i <- 0 to s - 2)
        yield (((steps(i) + EPS).toInt, (steps(i + 1) + EPS).toInt - 1))

    intervals :+ ((steps(s - 1) + EPS).toInt, elementCnt)
  }
}
