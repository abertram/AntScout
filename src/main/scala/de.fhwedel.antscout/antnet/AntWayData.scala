package de.fhwedel.antscout
package antnet

import osm.{OsmMap, OsmWay, OsmNode}

/**
 * Wird für die Vorberechnung der Ant-Wege benötigt.
 *
 * @param maxSpeed Maximale Geschwidigkeit
 * @param nodes Knoten, aus denen der Weg besteht
 */
class AntWayData(val maxSpeed: Double, val nodes: Seq[OsmNode]) {

  /**
   * Berechnet, ob eine Osm-Knoten-Sequenz in diesen Ant-Weg-Daten enthalten ist.
   *
   * @param nodes Osm-Knoten-Sequenz
   * @return true, wenn die Osm-Knoten-Sequenz enthalten ist
   */
  def containsSlice(nodes: Seq[OsmNode]) = {
    this.nodes.containsSlice(nodes) || this.nodes.containsSlice(nodes.reverse)
  }

  /**
   * Erweitert den Weg um eine Knoten-Sequenz und verrechnet die beiden maximalen Geschwindigkeiten.
   *
   * @param nodes Knoten, um die der Weg erweitert werden soll
   * @param maxSpeed Maximale Geschwindigkeit des Weges, zu dem die Knoten gehören
   * @return Erweiterter Weg
   */
  def extend(nodes: Seq[OsmNode], maxSpeed: Double): AntWayData = {
    if (this.nodes.containsSlice(nodes) || this.nodes.containsSlice(nodes.reverse))
      this
    else {
      assert(Set(this.nodes.head, this.nodes.last, nodes.head, nodes.last).size == 3, "this.nodes: %s, " +
          "nodes: %s" format (this.nodes, nodes))
      val newNodes = if (this.nodes.last == nodes.head)
        this.nodes ++ nodes.tail
      else if (this.nodes.last == nodes.last)
        this.nodes ++ nodes.reverse.tail
      else if (this.nodes.head == nodes.head)
        this.nodes.reverse ++ nodes.tail
      else // this.nodes.head == nodes.last
        nodes ++ this.nodes.tail
      AntWayData(calculateWeightedMaxSpeed(nodes, maxSpeed), newNodes)
    }
  }

  /**
   * Verrechnet die maximale Geschwindigkeit dieses Weges mit einer anderen maximalen Geschwindigkeit. Dabei wird nach
   * der Weg-Länge gewichtet.
   *
   * @param nodes Knoten des anderen Weges (notwendig für die Längen-Berechnung)
   * @param maxSpeed Maximale Geschwidigkeit des anderen Weges
   * @return Gewichtete maximale Geschwindigkeit
   */
  def calculateWeightedMaxSpeed(nodes: Seq[OsmNode], maxSpeed: Double) = {
    val length1 = OsmWay.length(this.nodes)
    val length2 = OsmWay.length(nodes)
    val newLength = length1 + length2
    this.maxSpeed * length1 / newLength + maxSpeed * length2 / newLength
  }

  /**
   * Prüft anhand eines Knoten-Wege-Mappings, ob ein Weg an einem seiner Knoten erweiterbar ist.
   *
   * @param node
   * @param nodeToWaysMapping
   * @return
   */
  def isExtendable(node: OsmNode)(implicit nodeToWaysMapping: Map[OsmNode, Iterable[OsmWay]] = OsmMap.nodeWaysMapping) = {
    node.isConnection(nodeToWaysMapping)
  }
}

/**
 * AntWayData-Factory.
 */
object AntWayData {

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.antnet.AntWayData]]-Instanz.
   *
   * @param maxSpeed Maximale Geschwidigkeit
   * @param nodes Osm-Knoten-Sequenz
   * @param oneWay true, wenn die Ant-Weg-Daten eine Einbahn-Strasse repräsentieren
   * @return Neue [[de.fhwedel.antscout.antnet.AntWayData]]-Instanz
   */
  def apply(maxSpeed: Double, nodes: Seq[OsmNode], oneWay: Boolean = false) = {
    if (oneWay)
      new AntOneWayData(maxSpeed, nodes)
    else
      new AntWayData(maxSpeed, nodes)
  }
}
