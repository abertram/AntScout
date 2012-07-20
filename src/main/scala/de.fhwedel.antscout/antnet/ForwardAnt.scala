package de.fhwedel.antscout
package antnet

import java.util.concurrent.TimeUnit
import net.liftweb.util.{TimeHelpers, Props}
import akka.util.Duration
import akka.actor.{Cancellable, ActorLogging, Actor}
import util.Statistics
import collection.mutable
import java.text.SimpleDateFormat
import java.util.Date

class ForwardAnt(val source: AntNode, val destination: AntNode) extends Actor with ActorLogging {
  
  import ForwardAnt._

  val DefaultAntLifetime = 30
  val DefaultTimeUnit = "SECONDS"

  val antLifetime = Props.getInt("antLifetime", DefaultAntLifetime)
  var cancellable: Cancellable = _
  var currentNode: AntNode = _
  val logEntries = mutable.Buffer[String]()
  val memory = AntMemory()
  val timeUnit = TimeUnit.valueOf(Props.get("timeUnit", DefaultTimeUnit))
  var visitedNodesCount = 0

  /**
   * Fügt einen Log-Eintrag hinzu.
   *
   * @param message Nachricht
   * @param time Uhrzeit
   */
  def addLogEntry(message: String, time: Date = TimeHelpers.now) {
    val sdf = new SimpleDateFormat("HH:mm:ss.SSS")
    "%s %s".format(sdf.format(time), message) +=: logEntries
  }

  def launchBackwardAnt() {
    BackwardAnt(source, destination, memory)
  }

  override def preStart() {
    cancellable = context.system.scheduler.scheduleOnce(Duration(antLifetime, timeUnit)) {
      addLogEntry("Lifetime expired, restarting")
      self ! Restart
    }
    visitNode(source)
  }

  /**
   * Gibt die Log-Einträge aus.
   */
  def dumpLogEntries() {
    addLogEntry("Stopped, %s nodes visited".format(visitedNodesCount))
    if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
      log.debug(logEntries.reverse.mkString("\n\t", "\n\t", ""))
  }

  protected def receive = {
    case ForwardAnt.AddLogEntry(message, time) =>
      addLogEntry("%s".format(message), time)
    case AntNode.Probabilities(probabilities) =>
      addLogEntry("Probabilities received")
      // Prüfen, ob die empfangengen Wahrscheinlichkeiten zu dem aktuell besuchten Knoten passen.
      // Das ist z.B. nicht der Fall, wenn die Ameise gleich nach dem Betreten eines Knoten und damit verbundener
      // Anfrage nach Wahrscheinlichkeiten neugestartet wird. Der Neustart sorgt dafür, dass der aktuell besuchte
      // Knoten auf den Quell-Knoten gesetzt wird. Eine der anschließend empfangenen Wahrscheinlichkeiten-Nachrichten
      // ist überflüssig und muss(!) ignoriert werden.
      val probabilitiesMatchCurrentNode = probabilities forall {
        case (antWay, _) =>
          (antWay.isInstanceOf[AntOneWay] && antWay.startNode == currentNode) ||
            antWay.startAndEndNodes.contains(currentNode)
      }
      if (!probabilitiesMatchCurrentNode)
        addLogEntry("Probabilities(%s) don't match current node, ignoring".format(probabilities))
      else {
        val nextNode = selectNextNode(probabilities)
        visitNode(nextNode)
      }
    case Restart =>
      restart()
    case m: Any =>
      log.warning("Unknown message: {}", m)
  }

  /**
   * Resettet die Ameise.
   */
  def restart() {
    cancellable.cancel()
    dumpLogEntries()
    logEntries.clear()
    memory.clear()
    visitedNodesCount = 0
    cancellable = context.system.scheduler.scheduleOnce(Duration(antLifetime, timeUnit)) {
      addLogEntry("Lifetime expired, restarting")
      self ! Restart
    }
    visitNode(source)
  }

  /**
   * Wählt den nächsten zu besuchenden Knoten aus.
   *
   * @param probabilities Wahrscheinlichkeiten
   * @return Der nächste zu besuchende Knoten.
   */
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

  /**
   * Wählt den Weg aus, über den ein Knoten verlassen werden soll.
   *
   * @param probabilities Wahrscheinlichkeiten
   * @return Weg, über den der Knoten verlassen werden soll.
   */
  def selectOutgoingWay(probabilities: Map[AntWay, Double]) = {
    addLogEntry("Selecting way")
    addLogEntry("Memory: %s".format(memory.items))
    val notVisitedWays = probabilities.filter { case (w, p) => !memory.containsWay(w) }
    addLogEntry("Not visited ways: %s".format(notVisitedWays.mkString(", ")))
    val way = if (notVisitedWays.nonEmpty)
      Statistics.selectByProbability(notVisitedWays)
    else
      Statistics.selectRandom(probabilities.keys.toSeq)
    addLogEntry("Selected way: #%s".format(way.id))
    way
  }

  def trace(message: String) {
    if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
      log.debug("{} {}", self.path.elements.last, message)
  }

  /**
   * Besucht einen Knoten.
   *
   * @param node Knoten, der bescuht werden soll.
   */
  def visitNode(node: AntNode) {
    addLogEntry("Visiting node #%s".format(node id))
    currentNode = node
    // Der Knoten hat keine ausgehenden Wege, wir sind in einer Sackgasse angekommen.
    if (!AntMap.outgoingWays.contains(node)) {
      addLogEntry("Dead-end street, restarting")
      self ! Restart
    } else if (node == destination) {
      addLogEntry("Destinatination reached, launching backward ant and restarting")
      launchBackwardAnt()
      self ! Restart
    } else {
      addLogEntry("%s - Entering %s, destination: %s)".format(self, node, destination))
      node.enter(destination, self)
    }
    visitedNodesCount += 1
  }
}

object ForwardAnt {

  case class AddLogEntry(message: String, time: Date = TimeHelpers.now)
  case object Restart
}
