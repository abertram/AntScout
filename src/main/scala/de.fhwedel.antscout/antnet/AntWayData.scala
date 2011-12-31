package de.fhwedel.antscout
package antnet

import osm.OsmNode

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 30.12.11
 * Time: 23:16
 */

class AntWayData(val id: String, val maxSpeed: Double, val nodes: List[OsmNode], val oneWay: Boolean) {

  override def equals(that: Any) = {
    that match {
      case antWayData: AntWayData =>
        id == antWayData.id && maxSpeed == antWayData.maxSpeed && nodes == antWayData.nodes && oneWay == antWayData.oneWay
      case _ => false
    }
  }

  override def toString = "%s, %f, %s, %s".format(id, maxSpeed, nodes.toString, oneWay.toString)
}

object AntWayData {

  def apply(id: String, maxSpeed: Double, nodes: List[OsmNode], oneWay: Boolean) = new AntWayData(id, maxSpeed, nodes, oneWay)
}