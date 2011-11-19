package de.fhwedel.antscout
package openstreetmap

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 11:36
 */

class Node(id: Int, geographicCoordinate: GeographicCoordinate) {
    override def toString = "[%d] %s".format(id, geographicCoordinate)
}
