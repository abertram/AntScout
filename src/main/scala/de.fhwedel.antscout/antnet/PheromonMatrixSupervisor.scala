package de.fhwedel.antscout
package antnet

import akka.actor.{ActorLogging, Props, Actor}
import akka.dispatch.Future
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout


/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 03.05.12
 * Time: 07:14
 */

class PheromonMatrixSupervisor extends Actor with ActorLogging {

  import context.dispatcher // context == ActorContext and "dispatcher" in it is already implicit
  import PheromonMatrixSupervisor._

  implicit val timeout = Timeout(5 seconds)

  def init(sources: Set[AntNode], destinations: Set[AntNode]) {
    log.info("Initializing")
    Future.sequence(sources.map { source =>
      val outgoingWays = AntMap.outgoingWays(source)
      val tripTimes = outgoingWays.map(ow => (ow -> ow.tripTime)).toMap
      val actor = context.actorOf(Props(new PheromoneMatrix(AntMap.destinations - source, outgoingWays)), source.id)
      actor ? PheromoneMatrix.Initialize(tripTimes)
    }) onComplete {
      case Left(e) => log.error(e.toString)
      case Right(_) => {
        assert(context.children.size == AntMap.sources.size, context.children.size)
        log.info("Initialized")
        AntScout.instance ! AntScout.PheromonMatrixSupervisorInitialized
      }
    }
  }

  protected def receive = {
    case Initialize(sources, destinations) =>
      init(sources, destinations)
    case m: PheromoneMatrix.GetAllPropabilities =>
      context.actorFor(m.node.id) forward m
    case m: PheromoneMatrix.GetPropabilities =>
      context.actorFor(m.node.id) forward m
    case m: PheromoneMatrix.UpdatePheromones =>
      context.actorFor(m.node.id) forward m
    case m: Any =>
      log.warning("Unknown message: {}" format m)
  }
}

object PheromonMatrixSupervisor {

  case class Initialize(sources: Set[AntNode], destinations: Set[AntNode])
}
