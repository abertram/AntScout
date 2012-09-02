package de.fhwedel.antscout
package antnet

import akka.util.duration._
import java.util.concurrent.TimeUnit
import net.liftweb.util.{TimeHelpers, Props}
import akka.util.Duration
import akka.actor.{ActorRef, Cancellable, ActorLogging, Actor}
import utils.StatisticsUtils
import collection.mutable
import java.text.SimpleDateFormat
import java.util.Date

class ForwardAnt(val source: ActorRef) extends Actor with ActorLogging {
  
  import ForwardAnt._

  val DefaultAntLifetime = 30
  val DefaultTimeUnit = "SECONDS"

  val antLifetime = Props.getInt("antLifetime", DefaultAntLifetime)
  var cancellable: Cancellable = _
  var currentNode: ActorRef = _
  var destination: ActorRef = _
  val logEntries = mutable.Buffer[String]()
  val memory = AntMemory()
  val selectNextNodeDurations = mutable.Buffer[Long]()
  var startTime: Long = _
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

  def completeTask(logEntry: String = "", parentMessage: ScalaObject) {
    cancellable.cancel()
    val endTime = System.currentTimeMillis
    addLogEntry(logEntry)
    dumpLogEntries()
    currentNode = null
    logEntries.clear()
    memory.clear()
    visitedNodesCount = 0
    context.parent ! parentMessage
  }

  def launchBackwardAnt() {
    BackwardAnt(source, destination, memory)
  }

  /**
   * Gibt die Log-Einträge aus.
   */
  def dumpLogEntries() {
    addLogEntry("Stopped, %s nodes visited".format(visitedNodesCount))
    trace(logEntries.reverse.mkString("\n\t", "\n\t", ""))
  }

  override def preStart() {
    addLogEntry("Started, waiting for task")
    context.system.scheduler.schedule(1 seconds, 1 seconds, self, ProcessStatistics)
  }

  def processStatistics() {
    val selectNextNodeDuration = if (selectNextNodeDurations.size > 0)
      selectNextNodeDurations.sum / selectNextNodeDurations.size
    else
      0
    context.parent ! Statistics(selectNextNodeDuration)
    selectNextNodeDurations.clear()
  }

  protected def receive = {
    case AddLogEntry(message, time) =>
      addLogEntry("%s".format(message), time)
    case AntNode.DeadEndStreet =>
      // Der Knoten hat keine ausgehenden Wege, wir sind in einer Sackgasse angekommen.
      completeTask("Dead-end street, restarting", DeadEndStreet(System.currentTimeMillis - startTime))
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
        val (time, nextNode) = TimeHelpers.calcTime(selectNextNode(probabilities))
        selectNextNodeDurations += time
        visitNode(nextNode)
      }
    case LifetimeExpired =>
      completeTask("Lifetime expired, restarting", LifetimeExpired(System.currentTimeMillis - startTime))
    case ProcessStatistics =>
      processStatistics()
    case Task(destination) =>
      startTime = System.currentTimeMillis
      addLogEntry("Task received")
      this.destination = destination
      cancellable = context.system.scheduler.scheduleOnce(Duration(antLifetime, timeUnit), self, LifetimeExpired)
      visitNode(source)
    case m: Any =>
      log.warning("Unknown message: {}", m)
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
      try {
        memory.removeCircle(currentNode)
      } catch {
        case _ =>
          log.error("Source: {}, current node: {}, destination: {}, log entries: {}", source, destination, currentNode,
            logEntries.reverse.mkString("\n\t", "\n\t", ""))
      }
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
      StatisticsUtils.selectByProbability(notVisitedWays)
    else
      StatisticsUtils.selectRandom(probabilities.keys.toSeq)
    addLogEntry("Selected way: #%s".format(way.id))
    way
  }

  def trace(message: String) {
    for {
      traceSourceId <- AntScout.traceSourceId
      traceDestinationId <- AntScout.traceDestinationId
      if (source.path.elements.last.matches(traceSourceId) && destination.path.elements.last.matches
        (traceDestinationId))
    } yield
      log.debug("{} {}", self.path.elements.last, message)
  }

  /**
   * Besucht einen Knoten.
   *
   * @param node Knoten, der besucht werden soll.
   */
  def visitNode(node: ActorRef) {
    addLogEntry("Visiting node #%s".format(node))
    currentNode = node
    if (node == destination) {
      launchBackwardAnt()
      completeTask("Destinatination reached, launching backward ant and restarting",
        DestinationReached(System.currentTimeMillis - startTime))
    } else {
      addLogEntry("%s - Entering %s, destination: %s)".format(self, node, destination))
      node ! AntNode.Enter(destination)
    }
    visitedNodesCount += 1
  }
}

object ForwardAnt {

  case class AddLogEntry(message: String, time: Date = TimeHelpers.now)
  case class DeadEndStreet(processDuration: Long)
  case class DestinationReached(processDuration: Long)
  case class LifetimeExpired(processDuration: Long)
  case object ProcessStatistics
  case class Statistics(averageSelectNextNodeDuration: Double)
  case class Task(destination: ActorRef)
  case object TaskFinished
}
