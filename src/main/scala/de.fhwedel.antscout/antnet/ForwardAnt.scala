package de.fhwedel.antscout
package antnet

import java.util.concurrent.TimeUnit
import net.liftweb.util.Props
import util.Random
import extensions.ExtendedDouble._
import akka.util.Duration
import akka.actor.{ActorLogging, PoisonPill, Actor}

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
  val random = Random
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
    case AntNode.Propabilities(propabilities) =>
      val nextNode = selectNextNode(propabilities)
      visitNode(nextNode)
    case m: Any =>
      log.warning("Unknown message: {}", m)
  }

  def selectNextNode(propabilities: Map[AntWay, Double]) = {
    val outgoingWay = if (memory.containsNode(currentNode)) {
      trace("Circle detected")
      trace("Memory before circle deleted: %s" format(memory))
      memory.removeCircle(currentNode)
      trace("Memory after circle deleted: %s" format(memory))
      selectRandomWay(propabilities)
    } else
      selectWay(propabilities)
    val (nextNode, tripTime) = outgoingWay.cross(currentNode)
    memory.memorize(currentNode, outgoingWay, tripTime)
    nextNode
  }

  def selectRandomWay(propabilities: Map[AntWay, Double]) = {
    trace("Selecting random way")
    propabilities.toSeq(random.nextInt(propabilities.size))._1
  }

  def selectWay(propabilities: Map[AntWay, Double]) = {
    trace("Selecting way")
    trace("Memory: %s".format(memory.items))
    val notVisitedWays = propabilities.filter { case (w, p) => !memory.containsWay(w) }
    trace("Not visited ways: %s".format(notVisitedWays.mkString(", ")))
    val way = if (notVisitedWays.nonEmpty) {
      trace("Nicht besuchte Wege gefunden")
      // Höchste Wahrscheinlichkeit bestimmen
      val maxPropability = notVisitedWays.maxBy(_._2)._2
      trace("Max propability: %f".format(maxPropability))
      // Noch nicht besuchte Wege mit der höchsten Wahrscheinlichkeit bestimmen
      val maxPropabilityNotVisitedWays = notVisitedWays.filter(_._2 ~= maxPropability)
      trace("maxPropabilityNotVisitedWays: %s".format(maxPropabilityNotVisitedWays.mkString(", ")))
      if (maxPropabilityNotVisitedWays.size == 1) {
        trace("Nur ein Weg mit höchster Wahrscheinlichkeit")
        // Wenn nur ein Weg gefunden wurde, diesen Weg verwenden
        maxPropabilityNotVisitedWays.head._1
      }
      else {
        trace("Mehrere Wege mit höchster Wahrscheinlichkeit, bestimme einen per Zufall")
        // Wenn mehrere Wege gefunden werden, zufällig einen bestimmen
        maxPropabilityNotVisitedWays.toSeq(random.nextInt(maxPropabilityNotVisitedWays.size))._1
      }
    } else {
      trace("Alle Wege bereits besucht, bestimme einen per Zufall")
      selectRandomWay(propabilities)
    }
    trace("Selected way: #%s".format(way id))
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
