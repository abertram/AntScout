package de.fhwedel.antscout
package osm

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.12.11
 * Time: 15:49
 */

class OsmOneWay(id: String, name: String, nodes: List[OsmNode], maxSpeed: Double) extends OsmWay(id, name, nodes, maxSpeed)

object OsmOneWay {
  
  def apply(id: Int, nodes: List[OsmNode]) = new OsmOneWay(id.toString, "", nodes, 0)

  def apply(id: String, name: String, nodes: List[OsmNode]) = new OsmOneWay(id, name, nodes, 0)
  
  def apply(id: String, nodes: List[OsmNode]) = new OsmOneWay(id, "", nodes, 0)

  def apply(id: String, name: String, nodes: List[OsmNode], maxSpeed: Double) = new OsmOneWay(id, name, nodes, maxSpeed)
}
                                                                               