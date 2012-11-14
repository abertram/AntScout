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
  
  test("nodeWaysMapping") {
    val nodes = (1 to  5).map(OsmNode(_)).toList
    val way1 = OsmWay(1, nodes.take(3))
    val way2 = OsmWay(2, nodes.drop(2))
    val ways = List(way1, way2)
    OsmMap(nodes, ways)
    OsmMap.nodeWaysMapping should have size (5)
    OsmMap.nodeWaysMapping(nodes(0)) should equal (Set(way1))
    OsmMap.nodeWaysMapping(nodes(1)) should equal (Set(way1))
    OsmMap.nodeWaysMapping(nodes(2)) should equal (Set(way1, way2))
    OsmMap.nodeWaysMapping(nodes(3)) should equal (Set(way2))
    OsmMap.nodeWaysMapping(nodes(4)) should equal (Set(way2))
  }
}
