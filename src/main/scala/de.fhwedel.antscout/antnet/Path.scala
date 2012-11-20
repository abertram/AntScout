package de.fhwedel.antscout
package antnet

import net.liftweb.common.Box
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST.JArray

/**
 * Repräsentiert einen Pfad.
 *
 * @param ways Wege
 */
class Path(val ways: Seq[AntWay]) {

  /**
   * Länge und Reise-Zeit
   */
  lazy val (length, tripTime) = ways.foldLeft(0.0, 0.0) {
    case ((lengthAcc, tripTimeAcc), way) => (way.length + lengthAcc, way.tripTime + tripTimeAcc)
  }

  /**
   * Erzeugt eine Json-Repräsentation.
   *
   * @return Json-Repräsentation
   */
  def toJson = {
    ("length" -> "%.4f".format(length / 1000)) ~
    ("lengths" -> JArray(List(
      ("unit" -> "m") ~
        ("value" -> "%.4f".format(length))))) ~
    ("tripTime" -> "%.4f".format(tripTime / 60)) ~
    ("tripTimes" -> JArray(List(
      ("unit" -> "s") ~
        ("value" -> "%.4f".format(tripTime)),
      ("unit" -> "h") ~
        ("value" -> "%.4f".format(tripTime / 3600))))) ~
    ("ways" ->  ways.map(_.toJson))
  }
}

/**
 * Path-Factory.
 */
object Path {

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.antnet.Path]]-Instanz.
   *
   * @param ways Wege
   * @return [[de.fhwedel.antscout.antnet.Path]]-Instanz
   */
  def apply(ways: Box[Seq[AntWay]]) = ways map { new Path(_) }

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.antnet.Path]]-Instanz.
   *
   * @param ways Wege
   * @return [[de.fhwedel.antscout.antnet.Path]]-Instanz
   */
  def apply(ways: Seq[AntWay]) = new Path(ways)
}
