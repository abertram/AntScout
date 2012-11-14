package de.fhwedel.antscout
package osm

import net.liftweb.common.Logger
import collection.immutable.Map
import net.liftweb.util.TimeHelpers
import collection.mutable
import mutable.{SynchronizedMap, SynchronizedSet}
import xml.{XML, NodeSeq, Elem}

/**
 * Repräsentiert eine OpenStreetMap-Karte.
 */
object OsmMap extends Logger {

  /**
   * Knoten
   */
  private var _nodes: Map[String, OsmNode] = _

  /**
   * Abbildung von Knoten auf adjazente Wege
   */
  private var _nodeWaysMapping: Map[OsmNode, Set[OsmWay]] = _

  /**
   * Wege
   */
  private var _ways: Map[String, OsmWay] = _

  /**
   * Berechnet das Knoten-Wege-Mapping.
   *
   * @return Knoten-Wege-Mapping
   */
  def computeNodeWaysMapping() = {
    info("Computing node ways mapping")
    // Mapping erstellen
    val synchronizedNodeWaysMapping = new mutable.HashMap[OsmNode, mutable.HashSet[OsmWay] with SynchronizedSet[OsmWay]] with SynchronizedMap[OsmNode, mutable.HashSet[OsmWay] with SynchronizedSet[OsmWay]]
    // Mapping initialisieren
    _nodes.values.map { node =>
      synchronizedNodeWaysMapping += (node -> new mutable.HashSet[OsmWay] with SynchronizedSet[OsmWay])
    }
    val (time, nodeWaysMapping) = TimeHelpers.calcTime {
      // Über die Wege iterieren
      _ways.values.par.foreach { way =>
        // Über die Weg-Knoten iterieren
        way.nodes.par.foreach { node =>
          // Knoten als Schlüssel und Weg als Wert in das Mapping einfügen
          synchronizedNodeWaysMapping(node) += way
        }
      }
      // In unveränderliche Daten-Strukturen umwandeln
      synchronizedNodeWaysMapping map {
        case (osmNode, osmWays) => (osmNode, osmWays toSet)
      } toMap
    }
    info("Node ways mapping with %d elements computed, took %d ms".format(nodeWaysMapping.size, time))
    nodeWaysMapping
  }

  /**
   * Lädt die OSM-Karte aus einer Datei.
   *
   * @param fileName Datei-Name
   */
  def apply(fileName: String) {
    info("Loading file %s" format fileName)
    apply(XML loadFile(fileName))
  }

  /**
   * Lädt die OSM-Karte aus XML-Daten.
   *
   * @param osmData XML-Daten
   */
  def apply(osmData: Elem) {
    info("Initializing")
    // Knoten parsen
    _nodes = parseNodes(osmData \ "node")
    // Wege parsen
    _ways = parseWays(osmData \ "way", _nodes)
    // Knoten-Wege-Mapping berechnen
    _nodeWaysMapping = computeNodeWaysMapping()
  }

  /**
   * Lädt die OSM-Karte aus Knoten und Wegen.
   *
   * @param nodes Knoten
   * @param ways Wege
   */
  def apply(nodes: Map[String, OsmNode], ways: Map[String, OsmWay]) {
    // Knoten speichern
    _nodes = nodes
    // Wege speichern
    _ways = ways
    // Knoten-Wege-Mapping berechnen
    _nodeWaysMapping = computeNodeWaysMapping()
  }
  
  /**
   * Lädt die OSM-Karte aus Knoten und Wegen.
   *
   * @param nodes Knoten
   * @param ways Wege
   */
  def apply(nodes: Iterable[OsmNode], ways: Iterable[OsmWay]) {
    // Knoten speichern
    val ns = nodes.map(n => {
      (n.id, n)
    }).toMap
    // Wege speichern
    val ws = ways.map(w => {
      (w.id, w)
    }).toMap
    this(ns, ws)
  }

  /**
   * Filtert das Knoten-Wege-Mapping anhand von Weg-Kategorien.
   *
   * @param highways Weg-Kategorien
   * @return Gefiltertes Knoten-Wege-Mapping
   */
  def nodeWaysByHighwayMapping(highways: Set[String]) = {
      _nodeWaysMapping.par.map {
        case (node, ways) => (node, ways.flatMap { way =>
          if (highways contains way.highway)
            Some(way)
          else
            None
        })
      }.seq
  }
  
  /**
   * Getter für das Knoten-Wege-Mapping
   *
   * @return Knoten-Wege-Mapping
   */
  def nodeWaysMapping = _nodeWaysMapping

  /**
   * Getter für Knoten
   *
   * @return Knoten
   */
  def nodes = _nodes

  /**
   * Parst die OSM-XML-Knoten.
   *
   * @param nodes OSM-XML-Knoten
   * @return OSM-Knoten
   */
  def parseNodes(nodes: NodeSeq) = {
    info("Parsing nodes")
    val (time, osmNodes) = TimeHelpers.calcTime(nodes.map { node =>
      // Id auslesen
      val id = (node \ "@id").text
      // OSM-Knoten erzeugen
      (id, OsmNode.parseNode(node))
    } toMap)
    info("%d nodes parsed in %d milliseconds".format(osmNodes.size, time))
    osmNodes
  }

  /**
   * Parst die OSM-XML-Wege.
   *
   * @param ways OSM-XML-Wege
   * @param nodes OSM-Knoten
   * @return OSM-Wege
   */
  def parseWays(ways: NodeSeq, nodes: Map[String, OsmNode]) = {
    info("Parsing ways")
    val (time, osmWays) = TimeHelpers.calcTime(ways.map(way => {
      val id = (way \ "@id").text
      (id, OsmWay.parseWay(way, nodes))
    }) toMap)
    info("%d ways parsed in %d milliseconds".format(osmWays.size, time))
    osmWays
  }

  /**
   * Getter für Wege.
   *
   * @return Wege
   */
  def ways = _ways
}
