package de.fhwedel.antscout
package antnet

import java.util.concurrent.TimeUnit
import net.liftweb.util.{TimeHelpers, Props}
import akka.util.Duration
import akka.actor.{ActorLogging, PoisonPill, Actor}
import util.Statistics
import collection.mutable
import java.text.SimpleDateFormat
import java.util.Date

class ForwardAnt(val source: AntNode, val destination: AntNode) extends Actor with ActorLogging {
  
  val DefaultAntLifetime = 30
  val DefaultTimeUnit = "SECONDS"

  var currentNode: AntNode = _
  val logEntries = mutable.Buffer[String]()
  val memory = AntMemory()
  var visitedNodesCount = 0

  /**
   * Fügt einen Log-Eintrag hinzu.
   *
   * @param message Nachricht
   * @param time Uhrzeit
   */
  def addLogEntry(message: String, time: Date = TimeHelpers.now) {
    if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId) {
      val sdf = new SimpleDateFormat("HH:mm:ss.SSS")
      "%s %s".format(sdf.format(time), message) +=: logEntries
    }
  }

  def launchBackwardAnt() {
    BackwardAnt(source, destination, memory)
  }

  override def postStop() {
    if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId) {
      addLogEntry("Stopped, %s nodes visited".format(visitedNodesCount))
      log.debug(logEntries.reverse.mkString("\n", "\n\t", ""))
    }
  }

  override def preStart() {
    val antLifetime = Props.getInt("antLifetime", DefaultAntLifetime)
    val timeUnit = TimeUnit.valueOf(Props.get("timeUnit", DefaultTimeUnit))
    context.system.scheduler.scheduleOnce(Duration(antLifetime, timeUnit)) {
      addLogEntry("Committing suicide!")
      self ! PoisonPill
    }
    visitNode(source)
  }

  protected def receive = {
    case ForwardAnt.AddLogEntry(message, time) =>
      addLogEntry(message, time)
    case AntNode.Probabilities(probabilities) =>
      addLogEntry("Probabilites received")
      val nextNode = selectNextNode(probabilities)
      visitNode(nextNode)
    case m: Any =>
      log.warning("Unknown message: {}", m)
  }

  def selectNextNode(probabilities: Map[AntWay, Double]) = {
    addLogEntry("Selecting next node")
    if (memory.containsNode(currentNode)) {
      addLogEntry("Circle detected")
      addLogEntry("Memory before circle deleted: %s" format(memory))
      memory.removeCircle(currentNode)
      addLogEntry("Memory after circle deleted: %s" format(memory))
    }
    val outgoingWay = selectOutgoingWay(probabilities)
    val (nextNode, tripTime) = outgoingWay.cross(currentNode)
    memory.memorize(currentNode, outgoingWay, tripTime)
    nextNode
  }

  def selectOutgoingWay(probabilities: Map[AntWay, Double]) = {
    addLogEntry("Selecting way")
    addLogEntry("Memory: %s".format(memory.items))
    val notVisitedWays = probabilities.filter { case (w, p) => !memory.containsWay(w) }
    addLogEntry("Not visited ways: %s".format(notVisitedWays.mkString(", ")))
    val way = if (notVisitedWays.nonEmpty)
      Statistics.selectByProbability(probabilities)
    else
      Statistics.selectRandom(probabilities.keys.toSeq)
    addLogEntry("Selected way: #%s".format(way.id))
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
    addLogEntry("Visiting node #%s".format(node id))
    currentNode = node
    // Der Knoten hat keine ausgehenden Wege, wir sind in einer Sackgasse angekommen.
    if (!AntMap.outgoingWays.contains(node)) {
      addLogEntry("Dead-end street")
      context.stop(self)
    } else if (node == destination) {
      addLogEntry("Destinatination reached")
//      debug("Destination reached, needed %s ms" format(System.currentTimeMillis - startTime))
      launchBackwardAnt()
      context.stop(self)
    } else {
      node.enter(destination, self)
    }
    visitedNodesCount += 1
  }
}

object ForwardAnt {

  case class AddLogEntry(message: String, time: Date = TimeHelpers.now)
}
