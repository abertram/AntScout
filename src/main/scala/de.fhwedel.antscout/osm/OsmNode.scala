package de.fhwedel.antscout
package osm

import annotation.tailrec
import map.Node
import net.liftweb.common.Logger
import net.liftweb.json.JsonDSL._
import scala.math._

/**
 * Repräsentiert einen OpenStreetMap-Knoten.
 *
 * @param id Id
 * @param geographicCoordinate Geographische Koordinate
 */
class OsmNode(id: String, val geographicCoordinate: GeographicCoordinate) extends Node(id) {

  /**
   * Berechnet die geographische Entfernung zu einem anderen Knoten.
   *
   * Die Berechnung basiert auf der Vincenty-Formel (http://en.wikipedia.org/wiki/Vincenty%27s_formulae) inklusive
   * Vincenty's Modifizierung (http://en.wikipedia.org/wiki/Vincenty%27s_formulae#Vincenty.27s_modification).
   *
   * @param that Knoten, zu dem der Abstand berechnet werden soll.
   * @return Geographischer Abstand in Metern oder 0, wenn der Abstand nicht berechnet werden konnte.
   */
  def distanceTo(that: OsmNode) = {
    val a = 6378137.0
    val f = 1 / 298.257223563
    val b = (1 - f) * a
    val U1 = atan((1 - f) * tan(geographicCoordinate.latitude.toRadians))
    val U2 = atan((1 - f) * tan(that.geographicCoordinate.latitude.toRadians))
    val L = (geographicCoordinate.longitude - that.geographicCoordinate.longitude).toRadians
    val sinU1 = sin(U1)
    val cosU1 = cos(U1)
    val sinU2 = sin(U2)
    val cosU2 = cos(U2)

    @tailrec
    def iterate(lambda: Double, iterationLimit: Int): Option[(Double, Double, Double, Double, Double)] = {
      iterationLimit match {
        case 0 =>
          None
        case _ =>
          val sinLambda = sin(lambda)
          val cosLambda = cos(lambda)
          val sinSigma = sqrt(pow(cosU2 * sinLambda, 2) + pow(cosU1 * sinU2 - sinU1 * cosU2 * cosLambda, 2))
          if (sinSigma.isNaN)
            None
          val cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
          val sigma = atan2(sinSigma, cosSigma)
          val alpha = asin(cosU1 * cosU2 * sinLambda / sinSigma)
          val cosSquaredAlpha = 1 - pow(sin(alpha), 2)
          val cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSquaredAlpha
          val c = f / 16 * cosSquaredAlpha * (4 + f * (4 - 3 * cosSquaredAlpha))
          val lambdaPrime = L + (1 - c) * f * sin(alpha) * (sigma + c * sinSigma * (cos2SigmaM + c * cosSigma * (-1 + 2 * pow(cos2SigmaM, 2))))
          if (abs(lambda - lambdaPrime) <= 1e-12)
            Some(Tuple5(cosSquaredAlpha, sinSigma, cos2SigmaM, cosSigma, sigma))
          else
            iterate(lambdaPrime, iterationLimit - 1)
      }
    }

    iterate(L, 99) match {
      case None =>
        0.0
      case iterationResult =>
        val cosSquaredAlpha = iterationResult.get._1
        val sinSigma = iterationResult.get._2
        val cos2SigmaM = iterationResult.get._3
        val cosSigma = iterationResult.get._4
        val sigma = iterationResult.get._5
        val uSquared = cosSquaredAlpha * (pow(a, 2) - pow(b, 2)) / pow(b, 2)
        val sqrtOfOnePlusUSquared = sqrt(1 + uSquared)
        val k1 = (sqrtOfOnePlusUSquared - 1) / (sqrtOfOnePlusUSquared + 1)
        val A = (1 + 1 / 4 * pow(k1, 2)) / (1 - k1)
        val B = k1 * (1 - 3 / 8 * pow(k1, 2))
        val deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * pow(cos2SigmaM, 2)) - B / 6 * cos2SigmaM * (-3 + 4 * pow(sinSigma, 2)) * (-3 + 4 * pow(cos2SigmaM, 2))))
        b * A * (sigma - deltaSigma)
    }
  }

  /**
   * Berechnet, ob ein Knoten eine Verbindung zwischen mehreren Strassen-Segmenten ist, die zur selben Strasse gehören.
   *
   * Das ist der Fall, wenn folgende Bedingungen erfüllt sind:
   *
   * - Der Knoten hat genau zwei adjazente Strassen (Diese Bedingung gilt nur für Knoten, die keine Start-Knoten von
   *    Kreis-Wegen sind. Start-Knoten von Kreis-Wegen müssen drei adjazente Strassen haben, da zwei davon die selbe
   *    Strasse ist. Sonst würde der Knoten fälschlicherweise als Verbindung erkannt werden.)
   * - Der Knoten ist ein äußerer Knoten in beiden Strassen.
   * - Die adjazenten Wege sind entweder beide Einbahnstrassen oder beide keine Einbahnstrassen.
   *
   * @param nodeWaysMapping Abbildung von von OSM-Knoten auf OSM-Wege, die für die Berechnung verwendet werden soll.
   * @return true, wenn der Knoten eine Verbindung ist.
   */
  def isConnection(implicit nodeWaysMapping: Map[OsmNode, Iterable[OsmWay]] = OsmMap nodeWaysMapping) = {
    val adjacentWays = nodeWaysMapping (this)
    adjacentWays.size == {
      // Prüfung, ob der Weg ein Kreis-Weg ist
      if (adjacentWays.exists(_.isCircle))
        3
      else
        2
    } && adjacentWays.forall(_.getClass == adjacentWays.head.getClass) && {
      (adjacentWays.head, adjacentWays.last) match {
        case (way1: OsmOneWay, way2: OsmOneWay) =>
          way1.nodes.last == way2.nodes.head || way2.nodes.last == way1.nodes.head
        case (way1: OsmWay, way2: OsmWay) =>
          Set(way1.nodes.head, way1.nodes.last, way2.nodes.head, way2.nodes.last).size == 3
      }
    }
  }

  override def toJson = {
    super.toJson ~
    ("longitude" -> geographicCoordinate.longitude) ~
    ("latitude" -> geographicCoordinate.latitude)
  }

  override def toString = "OsmNode #%s".format(id)
}

/**
 * OsmNode-Factory.
 */
object OsmNode extends Logger {

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.osm.OsmNode]]-Instanz.
   *
   * @param id Id
   * @return Neue [[de.fhwedel.antscout.osm.OsmNode]]-Instanz
   */
  def apply(id: Int) = new OsmNode(id.toString, GeographicCoordinate(0, 0))

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.osm.OsmNode]]-Instanz.
   *
   * @param id Id
   * @param geographicCoordinate Geographische Koordinate
   * @return Neue [[de.fhwedel.antscout.osm.OsmNode]]-Instanz
   */
  def apply(id: Int, geographicCoordinate: GeographicCoordinate) = new OsmNode(id.toString, geographicCoordinate)

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.osm.OsmNode]]-Instanz.
   *
   * @param id Id
   * @param geographicCoordinate Geographische Koordinate
   * @return Neue [[de.fhwedel.antscout.osm.OsmNode]]-Instanz
   */
  def apply(id: String, geographicCoordinate: GeographicCoordinate) = new OsmNode(id, geographicCoordinate)

  /**
   * Parst einen OSM-XML-Knoten.
   *
   * @param node OSM-XML-Knoten
   * @return Neue [[de.fhwedel.antscout.osm.OsmNode]]-Instanz
   */
  def parseNode(node: xml.Node): OsmNode = {
    val id = (node \ "@id").text
    val latitude = (node \ "@lat").text.toFloat
    require(latitude > -90.0 && latitude < 90.0, "Node %d: invalid latitude" format id)
    val longitude = (node \ "@lon").text.toFloat
    require(longitude > -180.0 && longitude < 180.0, "Node %d: invalid longitude" format id)
    val geographicCoordinate = new GeographicCoordinate(latitude, longitude)
    new OsmNode(id, geographicCoordinate)
  }
}
