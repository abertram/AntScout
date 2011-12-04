package de.fhwedel.antscout
package osm

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.12.11
 * Time: 15:49
 */

class OsmOneWay(id: String, name: String, nodes: Vector[OsmNode], maxSpeed: Double) extends OsmWay(id, name, nodes, maxSpeed)
