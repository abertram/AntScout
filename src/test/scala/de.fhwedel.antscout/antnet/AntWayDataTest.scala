package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import osm.{GeographicCoordinate, OsmNode}

class AntWayDataTest extends FunSuite with ShouldMatchers {

  test("extend1") {
    val node0 = OsmNode(0, GeographicCoordinate(0, 0))
    val node1 = OsmNode(1, GeographicCoordinate(0.00001, 0.00001))
    val antWayData = AntWayData(1, Seq(node0, node1))
    val node2 = OsmNode(2, GeographicCoordinate(0.00002, 0.00002))
    val extendedAntWayData = antWayData.extend(Seq(node1, node2), 1)
    extendedAntWayData.maxSpeed should be(antWayData.maxSpeed)
  }

  test("extend2") {
    val node0 = OsmNode(0, GeographicCoordinate(0, 0))
    val node1 = OsmNode(1, GeographicCoordinate(0.00001, 0.00001))
    val antWayData = AntWayData(1, Seq(node0, node1))
    val node2 = OsmNode(2, GeographicCoordinate(0.00002, 0.00002))
    val extendedAntWayData = antWayData.extend(Seq(node1, node2), 2)
    extendedAntWayData.maxSpeed should be(1.5 * antWayData.maxSpeed plusOrMinus 0.001)
  }

  test("extend3") {
    val node0 = OsmNode(0, GeographicCoordinate(0, 0))
    val node1 = OsmNode(1, GeographicCoordinate(0.00001, 0.00001))
    val antWayData = AntWayData(1, Seq(node0, node1))
    val node2 = OsmNode(2, GeographicCoordinate(0.00002, 0.00002))
    val extendedAntWayData = antWayData.extend(Seq(node1, node2), 3)
    extendedAntWayData.maxSpeed should be(2 * antWayData.maxSpeed plusOrMinus 0.001)
  }

  test("extend4") {
    val node0 = OsmNode(0, GeographicCoordinate(0, 0))
    val node1 = OsmNode(1, GeographicCoordinate(0.00001, 0.00001))
    val antWayData = AntWayData(1, Seq(node0, node1), true)
    val node2 = OsmNode(2, GeographicCoordinate(0.00002, 0.00002))
    val extendedAntWayData = antWayData.extend(Seq(node1, node2), 1)
    extendedAntWayData.maxSpeed should be(antWayData.maxSpeed)
  }

  test("extend5") {
    val node0 = OsmNode(0, GeographicCoordinate(0, 0))
    val node1 = OsmNode(1, GeographicCoordinate(0.00001, 0.00001))
    val antWayData = AntWayData(1, Seq(node0, node1), true)
    val node2 = OsmNode(2, GeographicCoordinate(0.00002, 0.00002))
    val extendedAntWayData = antWayData.extend(Seq(node1, node2), 2)
    extendedAntWayData.maxSpeed should be(1.5 * antWayData.maxSpeed plusOrMinus 0.001)
  }

  test("extend6") {
    val node0 = OsmNode(0, GeographicCoordinate(0, 0))
    val node1 = OsmNode(1, GeographicCoordinate(0.00001, 0.00001))
    val antWayData = AntWayData(1, Seq(node0, node1), true)
    val node2 = OsmNode(2, GeographicCoordinate(0.00002, 0.00002))
    val extendedAntWayData = antWayData.extend(Seq(node1, node2), 3)
    extendedAntWayData.maxSpeed should be(2 * antWayData.maxSpeed plusOrMinus 0.001)
  }

}
