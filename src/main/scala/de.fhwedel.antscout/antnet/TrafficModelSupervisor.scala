package de.fhwedel.antscout
package antnet

import akka.dispatch.Future
import akka.actor.{ActorLogging, Props, Actor}
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.05.12
 * Time: 11:52
 */

class TrafficModelSupervisor extends Actor with ActorLogging {

  import context.dispatcher // context == ActorContext and "dispatcher" in it is already implicit
  import TrafficModelSupervisor._

  implicit val timeout = Timeout(5 seconds)

  def init(sources: Set[AntNode], destinations: Set[AntNode], varsigma: Double) {
    log.info("Initializing")
    Future.sequence(sources.map { source =>
      val actor = context.actorOf(Props[TrafficModel], source.id)
      actor ? TrafficModel.Initialize(AntMap.destinations - source, varsigma, (5 * (0.3 / varsigma)).toInt)
    }) onComplete {
      case Left(e) => log.error(e.toString)
      case Right(_) => {
        assert(context.children.size == AntMap.sources.size, context.children.size)
        log.info("Initialized")
        AntScout.instance ! AntScout.TrafficModelSupervisorInitialized
      }
    }
  }

  protected def receive = {
    case Initialize(sources, destinations, varsigma) =>
      init(sources, destinations, varsigma)
    case m: TrafficModel.AddSample =>
      context.actorFor(m.node.id) forward m
    case m: TrafficModel.GetReinforcement =>
      context.actorFor(m.node.id) forward m
    case m: Any =>
      log.warning("Unknown message: {}", m)
  }
}

object TrafficModelSupervisor {

  case class Initialize(sources: Set[AntNode], destinations: Set[AntNode], varsigma: Double)
}
