package de.fhwedel.antscout
package antnet.pheromoneMatrix

import collection.mutable
import akka.actor.ActorRef
import net.liftweb.common.Logger
import antnet.AntWay

/**
 * Pheromon-Matrix.
 *
 * Enthält eine Zeile pro ausgehenden Weg und eine Spalte pro Ziel-Knoten.
 *
 * Hier werden sowohl die Pheromone verwaltet als auch die Wahrscheinlichkeiten, anhand der sich die Ameisen endgültig
 * für einen Weg eintscheiden.
 *
 * @param destinations Ziel-Knoten
 * @param outgoingWays Ausgehende Wege
 */
class PheromoneMatrix(destinations: Set[ActorRef], outgoingWays: Set[AntWay]) extends Logger {

//  assert((destinations & AntMap.destinations) == destinations && (AntMap.destinations &~ destinations).size <= 1)

  /**
   * Heuristische Werte
   */
  val heuristicValues = mutable.Map[AntWay, Double]()
  /**
   * Pheromone
   */
  val pheromones = mutable.Map[ActorRef, mutable.Map[AntWay, Double]]()
  /**
   * Wahrscheinlichkeiten
   */
  val probabilities = mutable.Map[ActorRef, mutable.Map[AntWay, Double]]()

  /**
   * Berechnet die Wahrscheinlichkeiten.
   */
  def calculateProbabilities() {
    destinations.foreach(calculateProbabilities _)
  }

  /**
   * Berechnet die Wahrscheinlichkeiten für einen Knoten.
   *
   * @param destination Knoten
   */
  def calculateProbabilities(destination: ActorRef) = {
    outgoingWays.foreach { outgoingWay =>
        probabilities(destination) += outgoingWay -> calculateProbability(destination, outgoingWay)
    }
  }

  /**
   * Berechnet die Wahrscheinlichkeiten für einen Knoten und einen Weg.
   *
   * @param destination Knoten
   * @param outgoingWay Weg
   * @return Wahrscheinlichkeit
   */
  def calculateProbability(destination: ActorRef, outgoingWay: AntWay) = {
    val probability =
      pheromones(destination)(outgoingWay) + Settings.Alpha * heuristicValues(outgoingWay) /
          (1 + Settings.Alpha * (outgoingWays.size - 1))
    // Zusicherung, dass die Wahrscheinlichkeiten für den ausgehenden Weg 0 wird. Das würde bedeuten,
    // dass sich keine Ameise mehr für diesen Weg entscheiden würde.
//    assert(probability ~> 0, "%s-%s: Probability = 0, pheromone: %s, alpha: %s, heuristic value: %s" format (self
//      .path.elements.last, destination, pheromones(destination)(outgoingWay), alpha, heuristicValues(outgoingWay)))
    probability
  }

  /**
   * Initialisiert die Pheromon-Matrix.
   *
   * @param pheromones Ziele
   * @param tripTimes Reise-Zeiten pro ausgehenden Weg
   */
  def initialize(pheromones: Map[ActorRef, Map[AntWay, Double]], tripTimes: Map[AntWay, Double]) {
    // Wahrscheinlichkeit pro Ziel erzeugen
    probabilities ++= destinations.map((_ -> mutable.Map[AntWay, Double]()))
    // Heuristische Werte initialisieren
    initHeuristicValues(tripTimes)
    // Pheromone initialisieren
    initPheromones(pheromones)
    // Wahrscheinlichkeiten berechnen
    calculateProbabilities()
  }

  /**
   * Berechnet die heuristische Größe η, die sich durch Normalisierung der Größe q aller Kanten, die zu einem benachbarten Knoten führen, ergibt. q ist gleich der Fahrdauer im statischen Fall.
   */
  def initHeuristicValues(tripTimes: Map[AntWay, Double]) {
    val travelTimesSum = tripTimes.values.sum
    tripTimes.map {
      case (antWay, travelTime) => heuristicValues += (antWay -> (1 - travelTime / travelTimesSum))
    }
  }

  /**
   * Initialisiert die Pheromone.
   *
   * @param pheromones Initiale Pheromone
   */
  def initPheromones(pheromones: Map[ActorRef, Map[AntWay, Double]]) {
    pheromones.foreach {
      case (destination, pheromones) =>
        this.pheromones += destination -> mutable.Map[AntWay, Double]()
        pheromones.foreach {
          case (way, pheromone) =>
            this.pheromones(destination) += way -> pheromone
        }
    }
  }

  /**
   * Aktualisiert die Pheromone.
   *
   * @param destination Ziel
   * @param way Weg
   * @param reinforcement Verstärkung
   */
  def updatePheromones(destination: ActorRef, way: AntWay, reinforcement: Double) {
    outgoingWays.foreach(ow => {
      // Alter Pheromon-Wert
      val oldPheromone = pheromones(destination)(ow)
      if (ow == way)
        // Besuchter Weg: Konzentration verstärken
        pheromones(destination) += ow -> (oldPheromone + reinforcement * (1 - oldPheromone))
      else
        // Nicht besuchter Weg: Konzentration verringern
        pheromones(destination) += ow -> (oldPheromone - reinforcement * oldPheromone)
    })
    // Wahrscheinlichkeiten aktualisieren
    calculateProbabilities(destination)
  }
}

/**
 * Pheromon-Matrix-Factory
 */
object PheromoneMatrix {

  def apply(destinations: Set[ActorRef], outgoingWays: Set[AntWay]) = new PheromoneMatrix(destinations, outgoingWays)
}
