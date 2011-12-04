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
  test("intersections") {
    val node1 = new Node(1, new GeographicCoordinate(1, 1))
    val node2 = new Node(2, new GeographicCoordinate(2, 2))
    val node3 = new Node(3, new GeographicCoordinate(3, 3))
    val node4 = new Node(4, new GeographicCoordinate(4, 4))
    val nodes = Vector(node1, node2, node3, node4)
    val way1 = new Way(1, "", Vector(node1, node2), 1)
    val way2 = new Way(2, "", Vector(node2, node3), 2)
    val ways = Vector(way1, way2)
    val osmMap = OsmMap(nodes, ways)
    val intersections = osmMap.intersections
    intersections should have size (1)
    intersections should contain (node2)
  }
}