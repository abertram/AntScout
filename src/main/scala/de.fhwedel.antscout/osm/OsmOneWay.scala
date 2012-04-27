package de.fhwedel.antscout
package osm

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.12.11
 * Time: 15:49
 */

class OsmOneWay(highway: String, id: String, name: String, nodes: List[OsmNode], maxSpeed: Double) extends OsmWay(highway, id, name, nodes, maxSpeed) {

  override def toString = "#%s #%s -> #%s".format(id, nodes.head.id, nodes.last.id)
}

object OsmOneWay {
  
  def apply(id: Int, nodes: List[OsmNode]) = new OsmOneWay("", id.toString, "", nodes, 0)

  def apply(highway: String, id: Int, name: String, nodes: List[OsmNode], maxSpeed: Double) =
    new OsmOneWay(highway: String, id.toString, name, nodes, maxSpeed)

  def apply(id: String, name: String, nodes: List[OsmNode]) = new OsmOneWay("", id, name, nodes, 0)
  
  def apply(id: String, nodes: List[OsmNode]) = new OsmOneWay("", id, "", nodes, 0)

  def apply(highway: String, id: String, name: String, nodes: List[OsmNode], maxSpeed: Double) =
    new OsmOneWay(highway, id, name, nodes, maxSpeed)
}
                                                                               