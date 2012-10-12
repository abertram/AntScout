package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import net.liftweb.util.TimeHelpers
import utils.StatisticsUtils

/**
 * Ameise die von einem Quell- zu einem Ziel-Knoten wandert. Der nächste Knoten wird anhand von Wahrscheinlichkeiten
 * bestimmt. Der durchlaufene Weg wird gemerkt. Nach dem Erreichen des Ziels werden die Knoten in umgekehrter
 * Reihenfolge besucht und dessen Datenstrukturen aktualisiert.
 *
 * @param source Quell-Knoten.
 * @param destination Ziel-Knoten.
 * @param memory Gedächtnis.
 * @param logEntries Log-Einträge.
 * @param startTime Startzeit.
 * @param isTraceEnabled Flag, ob die Ameise ihre Aktionen protokollieren soll.
 */
class Ant(val source: ActorRef, val destination: ActorRef, val memory: AntMemory, val logEntries: Seq[String],
    startTime: Long, val isTraceEnabled: Boolean) {

  import Ant._

  // für Debug-Zwecke
//  assert(source != destination, "Source equals destination, this shouldn't happen!")

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
    startTime, isTraceEnabled)

  def log(entry: String) = new Ant(source, destination, memory, logEntry(entry) +: logEntries, startTime,
    isTraceEnabled)

  def prepareLogEntries = logEntries.reverse.mkString("\n\t")

  /**
   * Wählt den nächsten zu besuchenden Knoten aus.
   *
   * @param node Aktueller Knoten.
   * @param probabilities Wahrscheinlichkeiten des aktuellen Knoten.
   * @return Der nächste zu besuchende Knoten und die aktualisierte Ameise.
   */
  def nextNode(node: ActorRef, probabilities: Map[AntWay, Double]) = {
    // Kreise entfernen
    val (memory1, logEntries1) = removeCycle(node)
    // noch nicht besuchte Wege berechnen
    val notVisitedWays = probabilities.filter { case (way, _) => !memory.containsWay(way) }
    val (way, logEntries2) = if (notVisitedWays.size == 1)
      // bei einem übrig gebliebenen Weg muss nicht gerechnet werden
      (notVisitedWays.head._1, if (isTraceEnabled) "Just one not visited way exists, selecting" else "")
    else if (notVisitedWays.nonEmpty)
      // bei mehreren Wegen wird der Weg anhand der Wahrscheinlichkeiten bestimmt
      (StatisticsUtils.selectByProbability(notVisitedWays), if (isTraceEnabled) "Selecting way by probability" else "")
    else
      // zufällig einen Weg bestimmen, falls alle Wege schonmal besucht wurden
      (StatisticsUtils.selectRandom(probabilities.keys.toSeq), if (isTraceEnabled) "Selecting random way" else "")
    // nächsten Knoten und die Reisezeit berechnen
    val (nextNode, tripTime) = way.cross(node)
    // berechnete Daten merken
    val memory2 = memory1.memorize(node, way, tripTime, probabilities.size > 1)
    // Log-Ausgaben generieren
    val logEntries3 = if (isTraceEnabled) Seq("Selecting next node") ++ logEntries1 ++
      Seq("Outgoing ways: %s".format(probabilities.keys), "Not visited ways: %s".format(notVisitedWays.mkString(", ")),
      logEntries2, "Selected way: %s".format(way))
    else
      Seq.empty
    (nextNode, new Ant(source, destination, memory2, logEntries3.reverse ++ logEntries, startTime, isTraceEnabled))
  }

  /**
   * Löscht einen Kreis bezüglich eines Knotens, falls vorhanden.
   *
   * @param node Knoten, für den der Kreis gelöscht werden soll.
   * @return Ameisen-Gedächtnis ohne Kreis und Log-Ausgaben, falls notwendig.
   */
  def removeCycle(node: ActorRef) =
    if (containsCycle(node)) {
      val memory1 = memory.removeCycle(node)
      (memory1, if (isTraceEnabled)
        Seq("Cycle detected, removing", "Memory before removing cycle: %s".format(memory),
        "Memory after removing cycle: %s".format(memory1))
      else
        Seq.empty)
    } else
      (memory, Seq.empty)

  override def toString = {
    "%s -> %s, memory: %s".format(AntNode.nodeId(source), AntNode.nodeId(destination), memory)
  }

  /**
   * Verschickt Nachrichten zum Aktualisieren der Knoten-Datenstrukturen.
   *
   * @return Aktualisierte Ameise, die Log-Ausgaben der durchgeführten Aktionen enthält.
   */
  def updateNodes() = {
    val (_, logEntries1) = memory.items.foldLeft((0.0, Seq[String]())) {
      case ((tripTimeAcc, logEntries), antMemoryItem @ AntMemoryItem(node, way, tripTime, shouldUpdate)) => {
        val tripTimeSum = tripTimeAcc + tripTime
        val updateDataStructures = AntNode.UpdateDataStructures(destination, way, tripTimeSum)
        if (shouldUpdate)
          node ! updateDataStructures
        (tripTimeSum, if (isTraceEnabled) {
          (if (shouldUpdate)
            "Updating %s: %s".format(node, updateDataStructures)
          else
            "Skipping update of %s" format node) +: logEntries
        } else Seq())
      }
    }
    new Ant(source, destination, memory, logEntries1 ++ logEntries, startTime, isTraceEnabled)
  }
}

object Ant {

  def apply(source: ActorRef, destination: ActorRef, isTraceEnabled: Boolean) =
    new Ant(source, destination, AntMemory(), if (isTraceEnabled) Seq(logEntry("%s -> %s"
      .format(AntNode.nodeId(source), AntNode.nodeId(destination)))) else Seq.empty, System.currentTimeMillis,
      isTraceEnabled)

  def apply(source: ActorRef, destination: ActorRef, logEntries: Seq[String], isTraceEnabled: Boolean) =
    new Ant(source, destination, AntMemory(), if (isTraceEnabled) logEntries.reverse ++ Seq(logEntry("%s -> %s".format
      (AntNode.nodeId(source), AntNode.nodeId(destination)))) else Seq.empty, System.currentTimeMillis, isTraceEnabled)

  def apply(source: ActorRef, destination: ActorRef, logEntry: String, isTraceEnabled: Boolean) =
    new Ant(source, destination, AntMemory(), if (isTraceEnabled) logEntry +: Seq(this.logEntry("%s -> %s".format(AntNode
      .nodeId(source), AntNode.nodeId(destination)))) else Seq.empty, System.currentTimeMillis, isTraceEnabled)

  def logEntry(entry: String) = "%tH:%1$tM:%1$tS:%1$tL: %s" format (TimeHelpers.now, entry)
}
