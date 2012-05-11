package de.fhwedel.antscout
package routing

import annotation.tailrec
import net.liftweb.common.Logger
import collection.mutable
import akka.actor.Actor
import akka.util.duration._
import akka.util.Timeout
import antnet.{AntNode, AntMap, AntWay}
import akka.dispatch.Future

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 14.01.12
 * Time: 15:15
 */

class RoutingService extends Actor with Logger {

  import context.dispatcher // context == ActorContext and "dispatcher" in it is already implicit

  import RoutingService._

  val _routingTable = mutable.Map[AntNode, mutable.Map[AntNode, Seq[AntWay]]]()
  implicit val timeout = Timeout(5 seconds)

  /**
   * Sucht einen Pfad von einem Quell- zu einem Ziel-Knoten.
   */
  def findPath(source: AntNode, destination: AntNode): Seq[AntWay] = {
    @tailrec
    def findPathRecursive(source: AntNode, path: Seq[AntWay]): Seq[AntWay] = {
      if (source == destination || path.size == 100)
        return path
      val bestWay = _routingTable(source)(destination) head
      val newSource = bestWay.endNode(source)
      findPathRecursive(newSource, bestWay +: path)
    }
    findPathRecursive(source, Seq()).reverse
  }

  def init() {
    info("Initializing")
    (Future.traverse(AntMap.sources)(source => source.propabilities(240 seconds))).onComplete {
      case Left(e) =>
        error(e)
      case Right(propabilities) => {
        propabilities foreach {
          case (source, propabilities) => {
            propabilities foreach {
              case (destination, waysAndPropabilities) =>
                val sortedWays = waysAndPropabilities.toSeq.sortBy {
                  case (_, propability) => propability
                }.reverse.map {
                  case (way, _) => way
                }
                val a1 = _routingTable.getOrElse(source, mutable.Map[AntNode, Seq[AntWay]]())
                _routingTable += (source -> (_routingTable.getOrElse(source, mutable.Map[AntNode, Seq[AntWay]]()) + (destination -> sortedWays)))
                val a2 = _routingTable.getOrElse(source, mutable.Map[AntNode, Seq[AntWay]]())
                assert(a1.size < a2.size)
            }
          }
        }
        assert(_routingTable.size == AntMap.sources.size, _routingTable.size)
        AntMap.sources.foreach { source =>
          assert(_routingTable.isDefinedAt(source), source)
          assert((_routingTable(source).keySet & AntMap.destinations) == _routingTable(source).keySet && (AntMap.destinations &~ _routingTable(source).keySet).size <= 1)
          (AntMap.destinations - source).foreach { destination =>
            assert(_routingTable(source).isDefinedAt(destination), "%s, %s" format(destination, source))
            assert(_routingTable(source)(destination).size == AntMap.outgoingWays(source).size, _routingTable(source)(destination).size)
          }
        }
        info("Initialized")
        AntScout.instance ! AntScout.RoutingServiceInitialized
      }
    }
  }

  protected def receive = {
    case FindPath(source, destination) =>
      sender ! findPath(source, destination)
    case Initialize =>
      init()
    case UpdatePropabilities(source, destination, ways) =>
      _routingTable(source) += destination -> ways
    case m: Any =>
      warn("Unknown message: %s" format m.toString)
  }
}

object RoutingService {

  case class FindPath(source: AntNode, destination: AntNode)
  case object Initialize
  case object Initialized
  case class UpdatePropabilities(source: AntNode, destination: AntNode, ways: Seq[AntWay])
}
