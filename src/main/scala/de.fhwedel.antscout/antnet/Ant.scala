package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import utils.StatisticsUtils

class Ant(val source: ActorRef, val destination: ActorRef, val memory: AntMemory, val logEntries: Seq[String],
    startTime: Long) {

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

  def log(entry: String) = new Ant(source, destination, memory, entry +: logEntries, startTime)

  def prepareLogEntries = logEntries.mkString("\n\t")

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
    val (way, logEntries2) = if (notVisitedWays.nonEmpty)
      (StatisticsUtils.selectByProbability(notVisitedWays), "Selecting way by probability")
    else
      (StatisticsUtils.selectRandom(probabilities.keys.toSeq), "Selecting random way")
    val (nextNode, tripTime) = way.cross(node)
    val memory2 = memory1.memorize(node, way, tripTime)
    val logEntries3 = Seq("Selecting next node") ++ logEntries1 ++
      Seq("Outgoing ways: %s".format(probabilities.keys), "Not visited ways: %s".format(notVisitedWays.mkString(", ")),
      logEntries2, "Selected way: %s".format(way))
    (nextNode, new Ant(source, destination, memory2, logEntries3.reverse ++ logEntries, startTime))
  }

  def removeCycle(node: ActorRef) =
    if (containsCycle(node)) {
      val memory1 = memory.removeCycle(node)
      (memory1, Seq("Cycle detected, removing", "Memory before removing cycle: %s".format(memory),
        "Memory after removing cycle: %s".format(memory1)))
    } else
      (memory, Seq())

  override def toString = {
    "%s -> %s, memory: %s".format(AntNode.nodeId(source), AntNode.nodeId(destination), memory)
  }

  def updateNodes() {
    memory.items.foldLeft(0.0) {
      case (tripTimeAcc, antMemoryItem @ AntMemoryItem(node, way, tripTime)) => {
        val tripTimeSum = tripTimeAcc + tripTime
        val updateDataStructures = AntNode.UpdateDataStructures(destination, way, tripTimeSum)
        node ! updateDataStructures
        tripTimeSum
      }
    }
  }
}

object Ant {

  def apply(source: ActorRef, destination: ActorRef) = new Ant(source, destination, AntMemory(),
    Seq("%s -> %s".format(AntNode.nodeId(source), AntNode.nodeId(destination))), System.currentTimeMillis)
}
