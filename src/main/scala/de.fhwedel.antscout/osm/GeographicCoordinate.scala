package de.fhwedel.antscout
package osm

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 11:40
 */

class GeographicCoordinate(val latitude: Double, val longitude: Double) {
  override def toString = "%f, %f" format(latitude, longitude)
}

object GeographicCoordinate {
  def apply(latitude: Double, longitude: Double) = new GeographicCoordinate(latitude, longitude)
}