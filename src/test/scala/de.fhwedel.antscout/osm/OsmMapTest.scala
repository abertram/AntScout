package de.fhwedel.antscout
package osm

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 30.11.11
 * Time: 23:20
 */

class OsmMapTest extends FunSuite with ShouldMatchers {
  
  test("nodeWaysMap") {
    val nodes = (1 to  5).map(OsmNode(_)).toList
    val way1 = OsmWay(1, nodes.take(3))
    val way2 = OsmWay(2, nodes.drop(2))
    val ways = List(way1, way2)
    val osmMap = OsmMap(nodes, ways)
    osmMap.nodeWaysMap should have size (5)
    osmMap.nodeWaysMap(nodes(0)) should equal (Set(way1))
    osmMap.nodeWaysMap(nodes(1)) should equal (Set(way1))
    osmMap.nodeWaysMap(nodes(2)) should equal (Set(way1, way2))
    osmMap.nodeWaysMap(nodes(3)) should equal (Set(way2))
    osmMap.nodeWaysMap(nodes(4)) should equal (Set(way2))
  }
  
  test("intersections") {
    val node1 = OsmNode(1, new GeographicCoordinate(1, 1))
    val node2 = OsmNode(2, new GeographicCoordinate(2, 2))
    val node3 = OsmNode(3, new GeographicCoordinate(3, 3))
    val node4 = OsmNode(4, new GeographicCoordinate(4, 4))
    val nodes = Vector(node1, node2, node3, node4)
    val way1 = OsmWay(1, "", List(node1, node2), 1)
    val way2 = OsmWay(2, "", List(node2, node3), 2)
    val ways = Vector(way1, way2)
    val osmMap = OsmMap(nodes, ways)
    val intersections = osmMap.intersections
    intersections should have size (1)
    intersections should contain (node2)
  }
}