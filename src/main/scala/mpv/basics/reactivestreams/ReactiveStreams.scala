package mpv.basics.reactivestreams

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object ReactiveStreams extends App {

  implicit val system: ActorSystem = ActorSystem("MySystem")

  def isPrime(n: Long): Boolean = {
    val upper = math.sqrt(n + 1).toInt
    (2 to upper) forall (i => n % i != 0)
  }

  def trace[T](expr: => T): T = {
    val result = expr
    println(s"$result (thread=${Thread.currentThread.getId})")
    result
  }

  def tracef[T](expr: => T, formatString: String = "%s"): T = {
    val result = expr
    println((formatString + " (thread=%d)")
      .format(result, Thread.currentThread.getId))
    result
  }

  def computePrimes(): Unit = {
    val source: Source[Int, NotUsed] = Source(2 to 100)
    val flow: Flow[Int, Int, NotUsed] = Flow[Int].filter(isPrime(_))
    val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](n => print(s"$n "))

    val graph1: RunnableGraph[NotUsed] = source via flow to sink
    graph1.run()
    Thread.sleep(50)
    println()

    val graph2 = source.via(flow).toMat(sink)(Keep.right)
    val done = graph2.run()
    Await.ready(done, Duration.Inf)
    println()
  }

  def computePrimesAsync(): Unit = {
    val source = Source(2 to 100)
    val printIntSink = Sink.foreach[Int](n => print(s"$n "))

    val done = source.filter(isPrime(_)).runWith(printIntSink)
    Await.ready(done, Duration.Inf)
    println()

    val done2 = source.throttle(1, 10.millis)
      .async
      .filter(n => {
        trace(n);
        isPrime(n)
      })
      .runWith(Sink.ignore)
    Await.ready(done2, Duration.Inf)
    println()

    val done3 = source.mapAsync(4)(n => Future {
      (n, isPrime(n))
    })
      .collect({ case (n, true) => n })
      .runWith(printIntSink)
    Await.ready(done3, Duration.Inf)
    println()

    val done4 = source.mapAsync(4)(n => Future {
      (trace(n), isPrime(n))
    })
      .collect({ case (n, true) => n })
      .runWith(Sink.ignore)
    Await.ready(done4, Duration.Inf)
    println()
  }

  println("================= computePrimes =================")
  computePrimes()

  println("============== computePrimesAsync ===============")
  computePrimesAsync()

  system.terminate()
}
