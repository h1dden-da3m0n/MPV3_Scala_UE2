package mpv.exercises.actors.ask

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object AskPatternApp extends App {

  implicit def actorRef2actorRefExtension(x: ActorRef): ActorRefExtension = {
    new ActorRefExtension(x)
  }

  implicit val timout: Timeout = 2.second

  def testAskingTheFuture(ref: ActorRef, actorName: String): Unit = {
    println(s"==== Testing 4 $actorName ====")
    val askTheFuture = ref ? TestActor.SomeMsg()
    askTheFuture onComplete {
      case Success(uv) => println(s"✔ asking the future finished successfully = $uv")
      case Failure(ex) => println(s"❌ asking the future finished with exception: $ex")
    }
    Await.ready(askTheFuture, Duration.Inf)
    Thread.sleep(50)
  }

  println("======== AskPatternApp ========")
  val system = ActorSystem("AskPatternSystem")

  val testActor1s = system.actorOf(Props(classOf[TestActor], 1.seconds), "TestActor_1s")
  val testActor4s = system.actorOf(Props(classOf[TestActor], 4.seconds), "TestActor_4s")

  testAskingTheFuture(testActor1s, "TestActor_1s")
  testAskingTheFuture(testActor4s, "TestActor_4s")

  Thread.sleep(50)
  val done = system.terminate()
  Await.ready(done, Duration.Inf)
}
