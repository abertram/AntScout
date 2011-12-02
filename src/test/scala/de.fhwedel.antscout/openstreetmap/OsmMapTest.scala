package de.fhwedel.antscout
package openstreetmap

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import collection.immutable.IntMap

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 30.11.11
 * Time: 23:20
 */

class OsmMapTest extends FunSuite with ShouldMatchers {
  test("createNodeWays") {
    val node1 = new Node(1, new GeographicCoordinate(1, 1))
    val node2 = new Node(2, new GeographicCoordinate(2, 2))
    val node3 = new Node(3, new GeographicCoordinate(3, 3))
    val node4 = new Node(4, new GeographicCoordinate(4, 4))
    val nodes = Vector(node1, node2, node3, node4)
    val way1 = new Way(1, "", Vector(node1, node2), 1)
    val way2 = new Way(2, "", Vector(node2, node3), 2)
    val ways = Vector(way1, way2)
    val nodeWays = OsmMap createNodeWays (nodes, ways)
    nodeWays should not be ('empty)
    nodeWays should have size (4)
    nodeWays(1) should have size (1)
    nodeWays(1) should contain (way1)
    nodeWays(2) should have size (2)
    nodeWays(2) should contain (way1)
    nodeWays(2) should contain (way2)
    nodeWays(3) should have size (1)
    nodeWays(3) should contain (way2)
    nodeWays(4) should be ('empty)
  }
}