package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 03.12.11
 * Time: 09:42
 */

class AntMapTest extends FunSuite with ShouldMatchers {
/*
  test("computeAntNodes") {
    val osmNodes1 = (0 to 2).map(OsmNode(_, GeographicCoordinate(0.0, 0.0))).toList
    val osmNodes2 = (3 to 5).map(OsmNode(_, GeographicCoordinate(0.0, 0.0))).toList
    val intersections = (6 to 7).map(Node(_)).toList
    val osmWay1 = OsmWay("", 1, "", osmNodes1, 0.0)
    val osmWay2 = OsmWay("", 2, "", osmNodes2, 0.0)
    val osmWays = List(osmWay1, osmWay2)
    val antNodes = AntMap.computeAntNodes(osmWays, intersections)
    antNodes.size should be (6)
    antNodes should equal (List(0, 2, 3, 5, 6, 7).map(Node(_)))
  }

  test("computeSourcesAndDestinations") {
    val nodes = AntMap.createAntNodes((0 to 2).map(Node(_)).toList)
    val outgoingWays = Map("0" -> Set(""))
    val incomingWays = Map("1" -> Set(""))
    val (sources, destinations) = AntMap.computeSourcesAndDestinations(nodes, outgoingWays, incomingWays)
    sources should be (Actor.registry.actorsFor("0").toList)
    destinations should be (Actor.registry.actorsFor("1").toList)
  }

  test("createAntNodes") {
    // alle Aktoren runterfahren, um nicht die Testergebnisse zu verfälschen
    Actor.registry.shutdownAll()
    val nodes = (0 to 4).map(Node(_)).toList
    val antNodes = AntMap.createAntNodes(nodes)
    antNodes.size should be (5)
    (0 to 4).foreach (id => {
      antNodes.get(id.toString) should not be ('empty)
      Actor.registry.actorsFor(id.toString).size should be (1)
    })
  }

  test("computeAntWaysData") {
    val osmNodes = (1 to 3).map(OsmNode(_)).toList
    val osmWay = OsmWay("", 1, "", osmNodes.toList, 1)
    val antWaysData = AntMap.computeAntWaysData(osmWay, List("1", "3"))
    antWaysData.size should be (1)
    antWaysData should equal (List(AntWayData("1-1", 1, osmNodes, false)))
  }

  test("computeAntWaysData, two OSM ways") {
    val osmNodes1 = (1 to 3).map(OsmNode(_)).toList
    val osmNodes2 = (3 to 5).map(OsmNode(_)).toList
    val osmWay1 = OsmWay("", 1, "", osmNodes1.toList, 1)
    val osmWay2 = OsmWay("", 2, "", osmNodes2.toList, 2)
    val antWaysData = List(osmWay1, osmWay2).flatMap(AntMap.computeAntWaysData(_, List("1", "3", "5")))
    antWaysData.size should be (2)
    antWaysData should equal (List(AntWayData("1-1", 1, osmNodes1, false), AntWayData("2-1", 2, osmNodes2, false)))
  }

  test("computeAntWaysData, one OSM way, two ant ways") {
    val osmNodes = (1 to 5).map(OsmNode(_)).toList
    val osmWay = OsmWay(1, osmNodes.toList)
    val antWaysData = AntMap.computeAntWaysData(osmWay, List("1", "3", "5"))
    antWaysData.size should be (2)
    antWaysData should equal (List(AntWayData("1-1", 0, osmNodes.take(3), false), AntWayData("1-2", 0, osmNodes.drop(2), false)))
  }

  test("computeAntWaysData, oneWay") {
    val osmNodes = (1 to 3).map(OsmNode(_)).toList
    val osmWay = OsmOneWay("", 1, "", osmNodes.toList, 1)
    val antWaysData = AntMap.computeAntWaysData(osmWay, List("1", "3"))
    antWaysData.size should be (1)
    antWaysData should equal (List(AntWayData("1-1", 1, osmNodes, true)))
  }

  test("computeIncomingAndOutgoingWays, 1") {
    val nodeIds = List("1", "3")
    val wayData = (AntWayData("1-1", 0, (1 to 3).map(OsmNode(_)).toList, false))
    val (incomingWays, outgoingWays) = AntMap.computeIncomingAndOutgoingWays(nodeIds, List(wayData))
    incomingWays should have size (2)
    incomingWays("1") should equal(Set("1-1"))
    incomingWays("3") should equal(Set("1-1"))
    outgoingWays should have size (2)
    outgoingWays("1") should equal(Set("1-1"))
    outgoingWays("3") should equal(Set("1-1"))
  }

  test("computeIncomingAndOutgoingWays, 2") {
    val nodeIds = List(1, 2, 3).map(_.toString)
    val wayData1 = AntWayData("1-1", 0, List(1, 2).map(OsmNode(_)), true)
    val wayData2 = AntWayData("2-1", 0, List(2, 3).map(OsmNode(_)), false)
    val waysData = List(wayData1, wayData2)
    val (incomingWays, outgoingWays) = AntMap.computeIncomingAndOutgoingWays(nodeIds, waysData)
    incomingWays should  have size (2)
    incomingWays("2") should equal (Set("1-1", "2-1"))
    incomingWays("3") should equal (Set("2-1"))
    outgoingWays should have size (3)
    outgoingWays("1") should equal (Set("1-1"))
    outgoingWays("2") should equal (Set("2-1"))
    outgoingWays("3") should equal (Set("2-1"))
  }
*/
}
