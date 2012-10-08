package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import net.liftweb.util.TimeHelpers
import utils.StatisticsUtils

class Ant(val source: ActorRef, val destination: ActorRef, val memory: AntMemory, val logEntries: Seq[String],
    startTime: Long) {

  import Ant._

  assert(source != destination, "Source equals destination, this shouldn't happen!")

  /**
   * Berechnet das Alter der Ameise.
   *
   * @return Alter der Ameise in Millisekunden.
   */
  def age = System.currentTimeMillis - startTime

  /**
   * Prüft, ob das Ameisen-Gedächtnis einen Zyklus bezüglich des übergebenen Knotens enthält.
   *
   * @param node
   * @return True, wenn das Ameisen-Gedächtnis einen Zyklus enthält.
   */
  def containsCycle(node: ActorRef) = memory.containsNode(node)

  def log(entries: Seq[String]) = new Ant(source, destination, memory, entries.reverse.map(logEntry(_)) ++ logEntries,
    startTime)

  def log(entry: String) = new Ant(source, destination, memory, logEntry(entry) +: logEntries, startTime)

  def prepareLogEntries = logEntries.reverse.mkString("\n\t")

  /**
   * Wählt den nächsten zu besuchenden Knoten aus.
   *
   * @param node Aktueller Knoten.
   * @param probabilities Wahrscheinlichkeiten des aktuellen Knoten.
   * @return Der nächste zu besuchende Knoten.
   */
  def nextNode(node: ActorRef, probabilities: Map[AntWay, Double]) = {
    val (memory1, logEntries1) = removeCycle(node)
    val notVisitedWays = probabilities.filter { case (way, _) => !memory.containsWay(way) }
    val (way, logEntries2) = if (notVisitedWays.size == 1)
      (notVisitedWays.head._1, if (ShouldLog) "" else "Just one outgoing way exists, selecting")
    else if (notVisitedWays.nonEmpty)
      (StatisticsUtils.selectByProbability(notVisitedWays), if (ShouldLog) "Selecting way by probability" else "")
    else
      (StatisticsUtils.selectRandom(probabilities.keys.toSeq), if (ShouldLog) "Selecting random way" else "")
    val (nextNode, tripTime) = way.cross(node)
    val memory2 = memory1.memorize(node, way, tripTime)
    val logEntries3 = if (ShouldLog) Seq("Selecting next node") ++ logEntries1 ++
      Seq("Outgoing ways: %s".format(probabilities.keys), "Not visited ways: %s".format(notVisitedWays.mkString(", ")),
      logEntries2, "Selected way: %s".format(way))
    else
      Seq.empty
    (nextNode, new Ant(source, destination, memory2, logEntries3.reverse ++ logEntries, startTime))
  }

  def removeCycle(node: ActorRef) =
    if (containsCycle(node)) {
      val memory1 = memory.removeCycle(node)
      (memory1, if (ShouldLog)
        Seq("Cycle detected, removing", "Memory before removing cycle: %s".format(memory),
        "Memory after removing cycle: %s".format(memory1))
      else
        Seq.empty)
    } else
      (memory, Seq.empty)

  override def toString = {
    "%s -> %s, memory: %s".format(AntNode.nodeId(source), AntNode.nodeId(destination), memory)
  }

  def updateNodes() = {
    val (_, logEntries1) = memory.items.foldLeft((0.0, Seq[String]())) {
      case ((tripTimeAcc, logEntries), antMemoryItem @ AntMemoryItem(node, way, tripTime)) => {
        val tripTimeSum = tripTimeAcc + tripTime
        val updateDataStructures = AntNode.UpdateDataStructures(destination, way, tripTimeSum)
        node ! updateDataStructures
        (tripTimeSum, if (ShouldLog) "Updating %s: %s".format(node, updateDataStructures) +: logEntries else Seq())
      }
    }
    new Ant(source, destination, memory, logEntries1 ++ logEntries, startTime)
  }
}

object Ant {

  val ShouldLog = false

  def apply(source: ActorRef, destination: ActorRef) = new Ant(source, destination, AntMemory(), if (ShouldLog)
    Seq(logEntry("%s -> %s".format(AntNode.nodeId(source), AntNode.nodeId(destination)))) else Seq.empty,
    System.currentTimeMillis)

  def apply(source: ActorRef, destination: ActorRef, logEntries: Seq[String]) =
    new Ant(source, destination, AntMemory(), if (ShouldLog) logEntries.reverse ++ Seq(logEntry("%s -> %s".format
      (AntNode.nodeId(source), AntNode.nodeId(destination)))) else Seq.empty, System.currentTimeMillis)

  def apply(source: ActorRef, destination: ActorRef, logEntry: String) =
    new Ant(source, destination, AntMemory(), if (ShouldLog) logEntry +: Seq(this.logEntry("%s -> %s".format(AntNode
      .nodeId(source), AntNode.nodeId(destination)))) else Seq.empty, System.currentTimeMillis)

  def logEntry(entry: String) = "%tH:%1$tM:%1$tS:%1$tL: %s" format (TimeHelpers.now, entry)
}
