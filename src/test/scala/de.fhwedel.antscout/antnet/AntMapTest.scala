package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import osm.{OsmWay, GeographicCoordinate, OsmNode}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 03.12.11
 * Time: 09:42
 */

class AntMapTest extends FunSuite with ShouldMatchers {
  test("convertOsmWayToAntWays") {
    val node1 = new OsmNode(1, new GeographicCoordinate(1, 1))
    val node2 = new OsmNode(2, new GeographicCoordinate(2, 2))
    val node3 = new OsmNode(3, new GeographicCoordinate(3, 3))
    val way = new OsmWay(1, "", Vector(node1, node2, node3), 0)
    val intersections = Seq(node1, node3)
    val antWays = AntMap convertOsmWayToAntWays (way, intersections)
    antWays should have size (1)
    antWays.head.id should be ("1-1")
    antWays.head.nodes should have size (2)
    antWays.head.nodes(0).id should be (1)
    antWays.head.nodes(1).id should be (3)
  }
}
