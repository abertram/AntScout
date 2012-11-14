package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import osm.{OsmOneWay, OsmWay, OsmNode, OsmMap}
import net.liftweb.util.TimeHelpers
import annotation.tailrec
import collection.immutable.{Set, Map}
import collection.mutable
import collection.mutable.ListBuffer
import map.Node

/**
 * Repräsentiert einen Graphen, auf dem der AntNet-Algortihmus operiert.
 */
object AntMap extends Logger {

  /**
   * Ziele.
   */
  private var _destinations: Set[Node] = _
  /**
   * Eingehende Wege pro Knoten.
   */
  private var _incomingWays: Map[Node, Set[AntWay]] = _
  /**
   * Knoten.
   */
  private val _nodes = mutable.Set[Node]()
  /**
   * Ausgehende Wege pro Knoten.
   */
  private var _outgoingWays: Map[Node, Set[AntWay]] = _
  /**
   * Quellen.
   */
  private var _sources: Set[Node] = _
  /**
   * Wege.
   */
  private var _ways: Set[AntWay] = _

  def apply() {
    info("%d ant nodes".format(_nodes.size))
//    info("Nodes without incoming ways: %s".format(_nodes.filter(n => !incomingWays.contains(n)).map(n => "\nhttp://www.openstreetmap.org/browse/node/%s".format(n.id))))
//    info("Nodes without outgoing ways: %s".format(_nodes.filter(n => !outgoingWays.contains(n)).map(n => "\nhttp://www.openstreetmap.org/browse/node/%s".format(n.id))))
  }

  /**
   * Berechnet Ant-Weg-Daten anhand einer Abbildung von Knoten auf die adjazenten Wege.
   *
   * @param nodeWaysMapping Abbildung von Knoten auf die adjazenten Wege.
   * @return Eine Menge von Ant-Weg-Daten.
   */
  def computeAntWayData(nodeWaysMapping: Map[OsmNode, Set[OsmWay]]): Set[AntWayData] = {
    @tailrec
    def computeAntWayDataRec(id: Int, innerNodeWaysMapping: Map[OsmNode, Set[OsmWay]],
        osmNodeAntWayDataMapping: Map[OsmNode, Set[AntWayData]], antWayData: Set[AntWayData]): Set[AntWayData] = {
      // Das Mapping ist leer, wir sind fertig.
      if (innerNodeWaysMapping.isEmpty)
        antWayData
      else {
        // der aktuell zu verarbeitende Knoten und die adjazenten Wege
        val (node, ways) = innerNodeWaysMapping.head
        // Der aktuelle Knoten hat keine adjazenten Wege mehr, weiter mit dem nächsten Knoten.
        if (ways.isEmpty)
          computeAntWayDataRec(id, innerNodeWaysMapping tail, osmNodeAntWayDataMapping, antWayData)
        else {
          // der aktuell zu verarbeitende Weg
          val way = ways.head
          // Start- und End-Indices ausgehend von dem aktuellen Knoten berechnen. Diese werden benutzt, um einen Knoten-Segment aus dem aktuellen Weg auszuschneiden und daraus einen Ant-Weg zu erstellen.
          val (startNodeIndex, endNodeIndex) = {
            // Der aktuelle Knoten ist nicht der End-Knoten des Weges.
            if (!way.isEndNode(node)) {
              // Start- und End-Inex in die Vorwärts-Richtung suchen
              startAndEndNodeIndexForward(way, node, nodeWaysMapping)
            }
            // Der aktuelle Knoten ist der End-Knoten des Weges.
            else {
              // Start- und End-Inex in die Rückwärts-Richtung suchen
              val (startNodeIndex, endNodeIndex) = startAndEndNodeIndexBackward(way, node, nodeWaysMapping)
              // Wenn der aktuelle Weg ein Kreis-Weg ist und der aktuelle Knoten der Anfangsknoten des Weges ist,
              // dann ist eine Sonderprüfung nötig. Es wird geprüft, ob das letzte Segment des Weges schon
              // verarbeitet wurde. Falls das der Fall sein sollte, wird mit dem aktuellen Knoten "manuell" auf das
              // erste Segment umgeschaltet und dessen Start- und End-Knoten-Index berechnet.
              val nodes = way.nodes.slice(startNodeIndex, endNodeIndex + 1)
              if (!(way.isCircle && way.isStartNode(node) && ((osmNodeAntWayDataMapping.contains(nodes.head) && osmNodeAntWayDataMapping(nodes.head).find(_.nodes.containsSlice(nodes)).isDefined) || (osmNodeAntWayDataMapping.contains(nodes.last) && osmNodeAntWayDataMapping(nodes.last).find(_.nodes.containsSlice(nodes)).isDefined) || (osmNodeAntWayDataMapping.contains(nodes.head) && osmNodeAntWayDataMapping.contains(nodes.last) && osmNodeAntWayDataMapping(nodes.head) == osmNodeAntWayDataMapping(nodes.last)))))
                (startNodeIndex, endNodeIndex)
              else
                startAndEndNodeIndexForward(way, node, nodeWaysMapping)
            }
          }
          // Die aktuell zu verarbeitende Knoten-Sequenz besteht aus Knoten zwischen dem berechneten Start- und End-Index.
          val nodes = way.nodes.slice(startNodeIndex, endNodeIndex + 1)
//          assert(nodes.nonEmpty, way.id)
          // TODO Prüfen, was zu tun ist, wenn nodes nur ein Element enthält
          // Verarbeitung der berechneten Knoten-Sequenz. Es werden fünf mögliche Fälle unterschieden.
          // Fall 1: Die Knoten-Sequenz ist bereits in einem anderen Ant-Weg enthalten.
          val (newWays, newAntWays, oldWays) = if ((osmNodeAntWayDataMapping.contains(nodes.head) && osmNodeAntWayDataMapping(nodes.head).find(_.nodes.containsSlice(nodes)).isDefined) || (osmNodeAntWayDataMapping.contains(nodes.last) && osmNodeAntWayDataMapping(nodes.last).find(_.nodes.containsSlice(nodes)).isDefined) || (osmNodeAntWayDataMapping.contains(nodes.head) && osmNodeAntWayDataMapping.contains(nodes.last) && osmNodeAntWayDataMapping(nodes.head) == osmNodeAntWayDataMapping(nodes.last)))
            (Set[AntWayData](), antWayData, Set[AntWayData]())
          // Fall 2: Die Knoten-Sequenz ist das Verbindungsstück zwischen zwei bereits existierenden Ant-Wegen.
          else if (osmNodeAntWayDataMapping.contains(nodes.head) && osmNodeAntWayDataMapping(nodes.head).size == 1 && osmNodeAntWayDataMapping(nodes.head).find(w => w.isExtendable(nodes.head)(nodeWaysMapping)).isDefined && osmNodeAntWayDataMapping.contains(nodes.last) && osmNodeAntWayDataMapping(nodes.last).size == 1 && osmNodeAntWayDataMapping(nodes.last).find(w => w.isExtendable(nodes.last)(nodeWaysMapping)).isDefined) {
            // Beide Wege ermitteln, die verbunden werden sollen.
            val oldWayData1 = osmNodeAntWayDataMapping(nodes.head).head
            val oldWayData2 = osmNodeAntWayDataMapping(nodes.last).head
            // Weg erweitern
            val newWayData = Set(oldWayData1.extend(nodes, way.maxSpeed).extend(oldWayData2.nodes, way.maxSpeed))
            // oldWays musste explizit erstellt werden.
            // Bei (newWays, antWayData -- oldWays ++ newWays, Set(oldWay1, oldWay2)) hat der Compiler gemeckert (recursive value ... needs type)
            val oldWayData = Set(oldWayData1, oldWayData2)
            (newWayData, antWayData -- oldWayData ++ newWayData, oldWayData)
          }
          // Fall 3: Ein oder mehrere bereits vorhandene Wege können am Kopf um die Knoten-Sequens erweitert werden.
          else if (osmNodeAntWayDataMapping.contains(nodes.head) && osmNodeAntWayDataMapping(nodes.head).find(_.isExtendable(nodes.head)(nodeWaysMapping)).isDefined) {
            val oldWayData = osmNodeAntWayDataMapping(nodes.head).filter(_.isExtendable(nodes.head)(nodeWaysMapping))
            val newWayData = oldWayData.map(_.extend(nodes, way.maxSpeed))
            (newWayData, antWayData -- oldWayData ++ newWayData, oldWayData)
          // Fall 4: Ein oder mehrere bereits vorhandene Wege können am Ende um die Knoten-Sequens erweitert werden.
          } else if (osmNodeAntWayDataMapping.contains(nodes.last) && osmNodeAntWayDataMapping(nodes.last).find(_.isExtendable(nodes.last)(nodeWaysMapping)).isDefined) {
            val oldWayData = osmNodeAntWayDataMapping(nodes.last).filter(_.isExtendable(nodes.last)(nodeWaysMapping))
            val newWayData = oldWayData.map(_.extend(nodes, way.maxSpeed))
            (newWayData, antWayData -- oldWayData ++ newWayData, oldWayData)
          // Fall 5: Keiner der oberen Fälle trifft zu. Es muss ein neuer Weg erstellt werden.
          } else {
            val newAntWayData = AntWayData(way.maxSpeed, nodes, way.isInstanceOf[OsmOneWay])
            (Set(newAntWayData), antWayData + newAntWayData, Set[AntWayData]())
          }
          // Mapping aktualisieren, sodass neu berechnte Wege die alten Wege ersetzen.
          val updatedAntNodeAntWaysMapping = osmNodeAntWayDataMapping ++ oldWays.flatMap { ow =>
            val nodeWayDataMappingToUpdate = osmNodeAntWayDataMapping.filter { case (n, ws) => ws.contains(ow) }
            val newWay = newWays.find(_.containsSlice(ow.nodes)).get
            nodeWayDataMappingToUpdate.map {
              case (n, ws) => n -> (osmNodeAntWayDataMapping.getOrElse(n, Set[AntWayData]()) - ow + newWay)
            }
          }
          // Mapping um die neu berechneten Wege und dessen Start- und End-Knoten ergänzen
          val newAntNodeAntWaysMapping = (updatedAntNodeAntWaysMapping ++ newWays.map { nw =>
            val startNode = nw.nodes.head
            startNode -> (updatedAntNodeAntWaysMapping.getOrElse(startNode, Set.empty[AntWayData]) + nw)
          } ++ newWays.map { nw =>
            val endNode = nw.nodes.last
            endNode -> (updatedAntNodeAntWaysMapping.getOrElse(endNode, Set.empty[AntWayData]) + nw)
          })
            // leere Einträge entfernen
            .filterNot(_._2.isEmpty)
          // Rekursiver Aufruf
          computeAntWayDataRec(id + 1, innerNodeWaysMapping + (node -> (ways - way)), newAntNodeAntWaysMapping, newAntWays)
        }
      }
    }
    info("Computing ant ways data")
    val (time, ways) = TimeHelpers.calcTime(computeAntWayDataRec(0, nodeWaysMapping, Map[OsmNode, Set[AntWayData]](), Set[AntWayData]()))
    info("%d ant ways data computed, took %d ms".format(ways.size, time))
    ways
  }

  /**
   * Berechnet die Ant-Wege aus extra dafür aufbereiteten Daten.
   *
   * @param wayData Aufbereitete Daten.
   */
  def computeAntWays(wayData: Set[AntWayData]) {
    info("Computing ant ways")
    _ways = (1 to wayData.size).zip(wayData).map {
      case (id, wd) => AntWay(id.toString, wd.nodes, wd.maxSpeed, wd.isInstanceOf[AntOneWayData])
    }.toSet
    info("Ant ways computed")
    assert(_ways.size == wayData.size)
    assert(_ways.map(_.id).toSet.size == _ways.size, "%s ids, %s ways".format(_ways.map(_.id).toSet.size, _ways.size))
  }

  /**
   * Berechnet die eingehenden und ausgehenden Wege pro Knoten.
   *
   * @return Zweier-Tupel. Das erste Element repräsentiert die eingehenden, das zweite Element die ausgehenden Wege. Die
   *         Datenstruktur ist jeweils eine Map, deren Schlüssel die einzelnen Knoten und die Werte die zugehörigen Wege
   *         sind.
   */
  def computeIncomingAndOutgoingWays() {
    assert(_ways != null)
    @tailrec
    def computeIncomingAndOutgoingWaysRec(ways: Set[AntWay], incomingWays: Map[Node, Set[AntWay]],
        outgoingWays: Map[Node, Set[AntWay]]): (Map[Node, Set[AntWay]], Map[Node, Set[AntWay]]) = {
      if (ways.isEmpty)
        // Wege sind leer, fertig
        (incomingWays, outgoingWays)
      else {
        // eingehende Wege
        val newIncomingWays = {
          ways.head match {
            // Einbahn-Strasse
            case oneWay: AntOneWay => {
              // nur der End-Knoten enthält einen eingehenden Weg
              Map(oneWay.nodes.last -> (incomingWays.getOrElse(oneWay.nodes.last, Set.empty[AntWay]) + oneWay))
            }
            // normaler Weg
            case way: AntWay => {
              // sowohl der Start- als auch der End-Knoten enthalten eingehende Wege
              Map(
                way.nodes.head -> (incomingWays.getOrElse(way.nodes.head, Set.empty[AntWay]) + way),
                way.nodes.last -> (incomingWays.getOrElse(way.nodes.last, Set.empty[AntWay]) + way))
            }
          }
        }
        // ausgehende Wege
        val newOutgoingWays = {
          ways.head match {
            // Einbahn-Strasse
            case oneWay: AntOneWay => {
              // nur der Start-Knoten enthält einen ausgehenden Weg
              Map(oneWay.nodes.head -> (outgoingWays.getOrElse(oneWay.nodes.head, Set.empty[AntWay]) + oneWay))
            }
            // normaler Weg
            case way: AntWay => {
              // sowohl der Start- als auch der End-Knoten enthalten ausgehende Wege
              Map(
                way.nodes.head -> (outgoingWays.getOrElse(way.nodes.head, Set[AntWay]()) + way),
                way.nodes.last -> (outgoingWays.getOrElse(way.nodes.last, Set[AntWay]()) + way))
            }
          }
        }
        computeIncomingAndOutgoingWaysRec(ways.tail, incomingWays ++ newIncomingWays, outgoingWays ++ newOutgoingWays)
      }
    }
    info("Computing incoming and outgoing ways")
    val (incomingWays, outgoingWays) = computeIncomingAndOutgoingWaysRec(_ways.toSet, Map[Node, Set[AntWay]](),
      Map[Node, Set[AntWay]]())
    info("Incoming and outgoing ways computed")
    _incomingWays = incomingWays
    _outgoingWays = outgoingWays
  }

  /**
   * Berechnet die Knoten aus der Ant-Weg-Daten.
   *
   * @param wayData Ant-Weg-Daten
   */
  def computeNodes(wayData: Set[AntWayData]) {
    info("Computing nodes")
    wayData.foreach { wd =>
      _nodes += wd.nodes.head.asInstanceOf[Node] += wd.nodes.last.asInstanceOf[Node]
    }
    info("%d nodes computed" format _nodes.size)
  }

  /**
   * Berechnet die Quell- und die Ziel-Knoten mit Hilfe der ein- und ausgehenden Wege.
   *
   */
  def computeSourcesAndDestinations() {
    assert(_nodes != null)
    assert(_incomingWays != null)
    assert(_outgoingWays != null)
    info("Computing sources and destinations")
    val sources = new ListBuffer[Node]
    val destinations = new ListBuffer[Node]
    val (time, _) = TimeHelpers.calcTime {
      _nodes.foreach {
        n => {
          // Wenn ein Knoten eingehende Wege hat, kann er als Ziel dienen
          if (_incomingWays.contains(n)) n +=: destinations
          // Wenn ein Knoten ausgehende Wege hat, kann er als Quelle dienen
          if (_outgoingWays.contains(n)) n +=: sources
        }
      }
    }
    info("%d sources and %d destinations computed, took %d ms".format(sources.size, destinations.size, time))
    _sources = sources.toSet
    _destinations = destinations.toSet
  }

  /**
   * Getter für Ziel-Knoten.
   *
   * @return Ziel-Knoten
   */
  def destinations = _destinations

  /**
   * Berechnet die Distanz zwischen zwei Knoten.
   *
   * @param source Quell-Knoten
   * @param destination Ziel-Knoten
   * @return 0, wenn es sich bei den beiden Knoten um den selben Knoten handelt. Weg-Länge, wenn die beiden Knoten durch
   *         einen Weg verbunden sind und der Weg vom Quell- zum Ziel-Knoten zeigt. Unendlich, wenn die beiden Knoten
   *         nicht durch einen Weg verbunden sind.
   */
  def distance(source: Node, destination: Node) = {
    if (source == destination)
      // Knoten sind gleich
      0.0
    else {
      if (!_outgoingWays.isDefinedAt(source))
        // Knoten hat keine ausgehenden Wege, ist also kein Quell-Knoten
        Double.PositiveInfinity
      else {
        _outgoingWays(source).find { way =>
          // Weg suchen, der die beiden Knoten verbindet
          Set(way.nodes.head, way.nodes.last) == Set(source, destination)
        }.map { way =>
          // Weg-Länge zurückgeben
          way.length
        }
          // Knoten sind nicht durch einen Weg verbunden, die Distanz ist unendlich.
          .getOrElse(Double.PositiveInfinity)
      }
    }
  }

  /**
   * Berechnet die Adjazenz-Matrix. Die Elemente werden wie folgt berechnet: adjacencyMatrix(i)(j) = weight(i, j)
   *
   * @return Adjazenz-Matrix
   */
  def adjacencyMatrix = {
    nodes.map { source =>
      source -> nodes.map { destination =>
        destination -> distance(source, destination)
      }.toMap
    }.toMap
  }

  /**
   * Berechnet die kürzesten Pfade zwischen allen Knoten-Paaren und deren Längen mit Hilfe des
   * Floyd-Warshall-Algorithmus. Der Pfad kann anschließend rekonstruiert werden.
   *
   * @param adjacencyMatrix Adjazenz-Matrix
   * @param predecessorMatrix Vorgänger-Matrix
   * @return Zweier-Tupel bestehend aus der Distanz- und der Zwischen-Knoten-Matrix.
   */
  def calculateShortestPaths(adjacencyMatrix: Map[Node, Map[Node, Double]], predecessorMatrix: Map[Node, Map[Node,
      Option[Node]]]): (Map[Node, Map[Node, Double]], Map[Node, Map[Node, Option[Node]]]) = {
    info("Calculating shortest paths")
    debug(distanceMatrixToString(adjacencyMatrix))
    debug(predecessorMatrixToString(predecessorMatrix))
    val (time, (distanceMatrix, intermediateMatrix)) = TimeHelpers.calcTime {
      // TODO Rausfinden, warum die Berechnung mit dem SynchronizedMap-Trait wesentlich schneller ist.
      // Distanz-Matrix initialisieren, initiale Distanz wird der Adjazenz-Matrix entnommen
      val distanceMatrix = new mutable.HashMap[Node, mutable.Map[Node, Double] with mutable.SynchronizedMap[Node, Double]]
        with mutable.SynchronizedMap[Node, mutable.Map[Node, Double] with mutable.SynchronizedMap[Node, Double]]
      adjacencyMatrix.par.foreach {
        case (source, distances) =>
          distanceMatrix += source -> new mutable.HashMap[Node, Double] with mutable.SynchronizedMap[Node, Double]
          source -> distances.par.map {
            case (destination, distance) =>
              distanceMatrix(source) += destination -> distance
          }
      }
      // Zwischen-Knoten-Matrix initialisieren, initiale Vorgänger werden der Vorgänger-Matrix entnommen
      val intermediateMatrix = new mutable.HashMap[Node, mutable.Map[Node, Option[Node]] with mutable
        .SynchronizedMap[Node, Option[Node]]] with mutable.SynchronizedMap[Node, mutable.Map[Node, Option[Node]] with
        mutable.SynchronizedMap[Node, Option[Node]]]
      predecessorMatrix.par.foreach {
        case (source, predecessors) =>
          intermediateMatrix += source -> new mutable.HashMap[Node, Option[Node]] with mutable.SynchronizedMap[Node,
            Option[Node]]
          source -> predecessors.par.map {
            case (destination, predecessor) =>
              intermediateMatrix(source) += destination -> predecessor
          }
      }
      // Über alle Knoten iterieren
      nodes.foreach { intermediate =>
        trace("Intermediate: %s" format intermediate)
        trace("Filtering sources")
        // Quell-Knoten berechnen
        val sources = nodes.filter { node =>
          node != intermediate && distanceMatrix(node)(intermediate) < Double.PositiveInfinity
        }
        trace("Sources filtered, result: %s" format sources)
        // Über die Quellen iterieren
        sources.foreach { source =>
          trace("Source: %s" format source)
          trace("Filtering destinations")
          // Ziel-Knoten berechnen
          val destinations = nodes.filter { node =>
            node != intermediate && distanceMatrix(intermediate)(node) < Double.PositiveInfinity
          }
          trace("Destinations filtered, result: %s" format destinations)
          // Über die Ziel-Knoten iterieren
          destinations.foreach { destination =>
            trace("Intermediate: %s, source: %s, destination: %s" format (intermediate, source, destination))
            // Distanz zwischen Quell-, Zwischen- und Ziel-Knoten besser als Distanz zwischen Quell- und Ziel-Knoten?
            if (distanceMatrix(source)(intermediate) + distanceMatrix(intermediate)(destination) <
                distanceMatrix(source)(destination)) {
              trace("\n\tpath(%s)(%s) + path(%s)(%s) = %1.2f < path(%s)(%s) = %1.2f, updating matrix"
                .format(source.id, intermediate.id, intermediate.id, destination.id, distanceMatrix(source)(intermediate) +
                distanceMatrix(intermediate)(destination), source.id, destination.id, distanceMatrix(source)(destination)))
              // Distanz aktualisieren
              distanceMatrix(source).update(destination, distanceMatrix(source)(intermediate) +
                distanceMatrix(intermediate)(destination))
              // Zwischen-Knoten-Matrix aktualisieren
              intermediateMatrix(source).update(destination, Some(intermediate))
//              trace(AntMap.distanceMatrixToString(distanceMatrix.map {
//                case (source, distances) => source -> distances.toMap
//              }.toMap))
//              trace(AntMap.predecessorMatrixToString(intermediateMatrix.map {
//                case (source, intermediates) => source -> intermediates.toMap
//              }.toMap))
            }
          }
        }
      }
      // Veränderliche Daten-Strukturen in unveränderliche umwandeln
      (distanceMatrix.map {
        case (source, distances) => source -> distances.toMap
      }.toMap, intermediateMatrix.map {
        case (source, predecessors) => source -> predecessors.toMap
      }.toMap)
    }
    info("Shortest paths calculated, took %d ms" format time)
    debug(distanceMatrixToString(distanceMatrix))
    debug(predecessorMatrixToString(intermediateMatrix))
    (distanceMatrix, intermediateMatrix)
  }

  /**
   * Bereitet die Distanz-Matrix so auf, dass diese als eine gut lesbare Tabelle dargestellt werden kann.
   *
   * @param matrix
   * @return
   */
  def distanceMatrixToString(matrix: collection.Map[Node, Map[Node, Double]]) = matrixToString[Double](matrix, ".2f",
    identity)

  /**
   * Getter für eingehende Wege.
   *
   * @return Eingehende Wege
   */
  def incomingWays = _incomingWays

  /**
   * Bereitet eine Matrix so auf, dass diese als eine gut lesbare Tabelle dargestellt werden kann.
   *
   * @param matrix Matrix, die aufbereitet werden soll.
   * @param elementFormatSuffix Format-Suffix, mit dem die Elemente der Matrix formatiert werden sollen. Der
   *                            Präfix besteht aus % und der Länge der längsten Knoten-Id. Sollen die Matrix-Elemente
   *                            z.B. als Gleitkommazahlen mit zwei Nachkommastellen formatiert werden, muss der
   *                            Parameter ".2f" lauten. Der endgültige Format-String lautet dann "%[Länge der
   *                            längsten Knoten-Id].2f". Darstellung als String wird mit "s" erreicht. In diesem Fall
   *                            lautet der endgültige Format-String "%[Länge der längsten Knoten-Id]s".
   * @param extract Extraktor-Funktion, die auf die Matrix-Elemente vor der Formatierung angewendet wird. So kann z.B.
   *                der Inhalt aus einem Option-Objekt extrahiert werden.
   * @tparam T Datentyp der Matrixelemente.
   * @return Matrix aufbereitet als Tabelle.
   */
  def matrixToString[T](matrix: collection.Map[Node, Map[Node, T]], elementFormatSuffix: String, extract: (T) => Any) = {
    // Quell-Knoten für die Darstellung
    val sources = matrix.map {
      case (source, elements) => source.id
    }
    // Ziel-Knoten für die Darstellung
    val destinations = {
      val (_, elements) = matrix.head
      elements.map {
        case (destination, element) =>
          destination.id
      }
    }
    // Längste Id berechnen
    val longestId = (sources.toSet ++ destinations.toSet).maxBy(_.length)
    // Tabelle für die Darstellung erzeugen
    destinations.map(id => ("%" + longestId.length + "s").format(id)).mkString("\n" + " " * longestId.length + "|", "|",
      "") + matrix.map {
        case (source, elements) =>
          ("%" + longestId.length + "s").format(source.id) + elements.map {
            case (destination, element) =>
              ("%" + longestId.length + elementFormatSuffix).format(extract(element))
          }.mkString("|", "|", "")
      }.mkString("\n", "\n", "")
  }

  /**
   * Getter für die Knoten.
   *
   * @return Knoten
   */
  def nodes: collection.Set[Node] = _nodes

  /**
   * Getter für die ausgehenden Wege.
   *
   * @return Ausgehende Wege
   */
  def outgoingWays = _outgoingWays

  /**
   * Berechnet einen Pfad vom `source`- zum `destination`-Knoten anhand der Distanz- und Vorgänger-Matrizen.
   *
   * @param source Quell-Knoten
   * @param destination Ziel-Knoten
   * @param distanceMatrix Distanz-Matrix
   * @param predecessorMatrix Vorgänger-Matrix
   * @return Pfad vom `source`- zum `destination`-Knoten oder None, wenn kein Pfad vorhanden ist.
   */
  def path(source: Node, destination: Node, distanceMatrix: Map[Node, Map[Node, Double]],
      predecessorMatrix: Map[Node, Map[Node, Option[Node]]]): Option[Seq[Node]] = {
    // TODO @tailrec
    def pathRec(source: Node, destination: Node): Seq[Node] = {
      assert(predecessorMatrix(source).isDefinedAt(destination))
      if (predecessorMatrix(source)(destination).get == source)
        // Wieder beim Quell-Knoten angekommen, fertig
        Seq[Node]()
      else {
        // Zwischen-Knoten berechnen
        val intermediate = predecessorMatrix(source)(destination).get
        // Pfad setzt sich zusammen aus dem Teil-Pfad von Quell- zum Zwischen-Knoten, dem Zwischen-Knoten und dem
        // Teil-Pfad vom Zwischen- zum Ziel-Knoten
        pathRec(source, intermediate) ++ Seq(intermediate) ++ pathRec(intermediate, destination)
      }
    }
    if (distanceMatrix(source)(destination) == 0.0 || distanceMatrix(source)(destination) == Double.PositiveInfinity)
      // Kein Pfad
      None
    else {
      // Pfad berechnen
      Some(Seq(source) ++ pathRec(source, destination) ++ Seq(destination))
    }
  }

  /**
   * Berechnet die Vorgänger-Matrix. Die Elemente der Matrix werden wie folgt berechnet: predecessorMatrix(i)(j) = i.
   *
   * @return Vorgänger-Matrix
   */
  def predecessorMatrix = {
    val distanceMatrix = this.adjacencyMatrix
    nodes.map { source =>
      source -> nodes.map { destination =>
        destination -> { if (destination != source && distanceMatrix(source)(destination) < Double.PositiveInfinity)
          Some(source)
        else
          None
        }
      }.toMap
    }.toMap
  }

  /**
   * Bereitet die Vorgänger-Matrix so auf, dass diese als eine gut lesbare Tabelle dargestellt werden kann.
   *
   * @param matrix Vorgänger-Matrix
   * @return Vorgänger-Matrix als String
   */
  def predecessorMatrixToString(matrix: Map[Node, Map[Node, Option[Node]]]) = matrixToString[Option[Node]](matrix,
    "s", (element) => element match {
      case Some(node) => node.id
      case None => ""
    })

  /**
   * Bereitet die AntMap vor, indem die Ant-Weg-Daten berechnet werden.
   *
   * @return Ant-Weg-Daten
   */
  def prepare = {
    info("Preparing")
    val nodeWaysMapping = OsmMap.nodeWaysByHighwayMapping(Settings.RelevantHighWays.toSet).par.filter {
      case (node, ways) => ways.size >= 2
    }.seq
    computeAntWayData(nodeWaysMapping)
  }

  /**
   * Getter für die Quell-Knoten.
   *
   * @return Quell-Knoten
   */
  def sources = _sources

  /**
   * Sucht die Indices zweier Knoten, die Kreuzungen sind, von einem Knoten aus in die Rückwärts-Richtung.
   *
   * @param way Weg, in dem gesucht werden soll.
   * @param node Start-Knoten
   * @param nodeWaysMapping Knoten-Wege-Mapping
   * @return Start- und End-Indices
   */
  def startAndEndNodeIndexBackward(way: OsmWay, node: OsmNode, nodeWaysMapping: Map[OsmNode, Set[OsmWay]]) = {
    // End-Index ist der Index des aktuellen Knoten. Der passende Start-Index muss gesucht werden.
    // Index des letzten Verbindungsknotens vom End-Knoten des Weges aus suchen.
    val index = way.nodes.lastIndexWhere(nodeWaysMapping.contains(_), way.nodes.size - 2)
    // Wenn kein passender Knoten gefunden wird, des Index des Start-Knotens verwenden.
    val startNodeIndex = if (index != -1) index else 0
    val endNodeIndex = way.nodes.lastIndexOf(node)
    (startNodeIndex, endNodeIndex)
  }

  /**
   * Sucht die Indices zweier Knoten, die Kreuzungen sind, von einem Knoten aus in die Vorwärts-Richtung.
   *
   * @param way Weg, in dem gesucht werden soll.
   * @param node Start-Knoten
   * @param nodeWaysMapping Knoten-Wege-Mapping
   * @return Start- und End-Indices
   */
  def startAndEndNodeIndexForward(way: OsmWay, node: OsmNode, nodeWaysMapping: Map[OsmNode, Set[OsmWay]]) = {
    // Start-Index ist der Index des aktuellen Knoten. Der passende End-Index muss gesucht werden.
    val startNodeIndex = way.nodes.indexOf(node)
    assert(startNodeIndex > -1)
    // passenden End-Knoten-Index (Verbindungs-Knoten zwischen mehreren Wegen) suchen
    val index = way.nodes.indexWhere(nodeWaysMapping.contains(_), startNodeIndex + 1)
    // Wenn kein passender Knoten gefunden wird, den Index des End-Knotens verwenden.
    val endNodeIndex = if (index != -1) index else way.nodes.size - 1
    (startNodeIndex, endNodeIndex)
  }

  /**
   * Getter für die Wege.
   *
   * @return Wege
   */
  def ways = _ways
}
