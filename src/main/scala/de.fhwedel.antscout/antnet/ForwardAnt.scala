package de.fhwedel.antscout
package antnet

import java.util.concurrent.TimeUnit
import net.liftweb.util.Props
import akka.util.Duration
import akka.actor.{ActorLogging, PoisonPill, Actor}
import util.Statistics

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 25.12.11
 * Time: 15:21
 */

class ForwardAnt(val source: AntNode, val destination: AntNode) extends Actor with ActorLogging {
  
  val DefaultAntLifetime = 30
  val DefaultTimeUnit = "SECONDS"

  var currentNode: AntNode = _
  val memory = AntMemory()
  var startTime: Long = _

  def launchBackwardAnt() {
    BackwardAnt(source, destination, memory)
    context.stop(self)
  }

  override def preStart() {
    val antLifetime = Props.getInt("antLifetime", DefaultAntLifetime)
    val timeUnit = TimeUnit.valueOf(Props.get("timeUnit", DefaultTimeUnit))
    context.system.scheduler.scheduleOnce(Duration(antLifetime, timeUnit)) {
      trace("Committing suicide!")
      self ! PoisonPill
    }
    startTime = System.currentTimeMillis
    visitNode(source)
  }

  protected def receive = {
    case AntNode.Propabilities(probabilities) =>
      val nextNode = selectNextNode(probabilities)
      visitNode(nextNode)
    case m: Any =>
      log.warning("Unknown message: {}", m)
  }

  def selectNextNode(probabilities: Map[AntWay, Double]) = {
    if (memory.containsNode(currentNode)) {
      trace("Circle detected")
      trace("Memory before circle deleted: %s" format(memory))
      memory.removeCircle(currentNode)
      trace("Memory after circle deleted: %s" format(memory))
    }
    val outgoingWay = selectOutgoingWay(probabilities)
    val (nextNode, tripTime) = outgoingWay.cross(currentNode)
    memory.memorize(currentNode, outgoingWay, tripTime)
    nextNode
  }

  def selectOutgoingWay(probabilities: Map[AntWay, Double]) = {
    trace("Selecting way")
    trace("Memory: %s".format(memory.items))
    val notVisitedWays = probabilities.filter { case (w, p) => !memory.containsWay(w) }
    trace("Not visited ways: %s".format(notVisitedWays.mkString(", ")))
    val way = if (notVisitedWays.nonEmpty)
      Statistics.selectByProbability(probabilities)
    else
      Statistics.selectRandom(probabilities.keys.toSeq)
    trace("Selected way: #%s".format(way.id))
    way
  }

//  override def trace(msg: => AnyRef) {
//    super.trace("#%s: %s".format(self.path, msg))
//  }

  def trace(message: String) {
    if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
      log.debug("{} {}", self.path.elements.last, message)
  }

  def visitNode(node: AntNode) {
    trace("Visiting node #%s".format(node id))
    currentNode = node
    // Der Knoten hat keine ausgehenden Wege, wir sind in einer Sackgasse angekommen.
    if (!AntMap.outgoingWays.contains(node)) {
      trace("Dead-end street")
      self ! PoisonPill
    } else if (node == destination) {
      trace("Destinatination reached")
//      debug("Destination reached, needed %s ms" format(System.currentTimeMillis - startTime))
      launchBackwardAnt()
      self ! PoisonPill
    } else {
      node.enter(destination, self)
    }
  }
}
