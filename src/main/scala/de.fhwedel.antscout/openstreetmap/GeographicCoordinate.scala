package de.fhwedel.antscout
package openstreetmap

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 11:40
 */

class GeographicCoordinate(latitude: Float, longitude: Float) {
    override def toString = "%f, %f" format (latitude, longitude)
}
