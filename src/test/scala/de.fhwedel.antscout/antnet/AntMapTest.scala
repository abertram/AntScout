package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import map.Node
import akka.actor.Actor
import osm._

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 03.12.11
 * Time: 09:42
 */

class AntMapTest extends FunSuite with ShouldMatchers {
  test("computeAntNodes") {
    val osmNodes1 = (0 to 2).map(OsmNode(_, GeographicCoordinate(0.0, 0.0))).toList
    val osmNodes2 = (3 to 5).map(OsmNode(_, GeographicCoordinate(0.0, 0.0))).toList
    val intersections = (6 to 7).map(Node(_)).toList
    val osmWay1 = OsmWay(1, "", osmNodes1, 0.0)
    val osmWay2 = OsmWay(2, "", osmNodes2, 0.0)
    val osmWays = List(osmWay1, osmWay2)
    val antNodes = AntMap.computeAntNodes(osmWays, intersections)
    antNodes.size should be (6)
    antNodes should equal (List(0, 2, 3, 5, 6, 7).map(Node(_)))
  }
  
  test("startAntNodes") {
    val nodes = (0 to 4).map(Node(_)).toList
    val antNodes = AntMap.startAntNodes(nodes)
    antNodes.size should be (5)
    (0 to 4).foreach (id => {
      antNodes.get(id.toString) should not be ('empty)
      Actor.registry.actorsFor(id.toString).size should be (1)
    })
  }

  test("computeAntWaysData") {
    val osmNodes = (1 to 3).map(OsmNode(_, GeographicCoordinate(0, 0))).toList
    val osmWay = OsmWay(1, "", osmNodes.toList, 0)
    val antWaysData = AntMap.computeAntWaysData(osmWay, List("1", "3"))
    antWaysData.size should be (1)
    antWaysData should equal (List(("1-1", false, osmNodes)))
  }

  test("computeAntWaysData, two OSM ways") {
    val osmNodes1 = (1 to 3).map(OsmNode(_, GeographicCoordinate(0, 0))).toList
    val osmNodes2 = (3 to 5).map(OsmNode(_, GeographicCoordinate(0, 0))).toList
    val osmWay1 = OsmWay(1, osmNodes1.toList)
    val osmWay2 = OsmWay(2, osmNodes2.toList)
    val antWaysData = List(osmWay1, osmWay2).flatMap(AntMap.computeAntWaysData(_, List("1", "3", "5")))
    antWaysData.size should be (2)
    antWaysData should equal (List(("1-1", false, osmNodes1), ("2-1", false, osmNodes2)))
  }

  test("computeAntWaysData, one OSM way, two ant ways") {
    val osmNodes = (1 to 5).map(OsmNode(_, GeographicCoordinate(0, 0))).toList
    val osmWay = OsmWay(1, osmNodes.toList)
    val antWaysData = AntMap.computeAntWaysData(osmWay, List("1", "3", "5"))
    antWaysData.size should be (2)
    antWaysData should equal (List(("1-1", false, osmNodes.take(3)), ("1-2", false, osmNodes.drop(2))))
  }

  test("computeAntWaysData, oneWay") {
    val osmNodes = (1 to 3).map(OsmNode(_, GeographicCoordinate(0, 0))).toList
    val osmWay = OsmOneWay(1, osmNodes.toList)
    val antWaysData = AntMap.computeAntWaysData(osmWay, List("1", "3"))
    antWaysData.size should be (1)
    antWaysData should equal (List(("1-1", true, osmNodes)))
  }

  test("computeIncomingWays") {
    val nodeIds = List("1")
    val wayData = ("1-1", false, (1 to 3).map(OsmNode(_, GeographicCoordinate(0, 0))).toList)
    val incomingWays = AntMap.computeIncomingWays(nodeIds, List(wayData))
    incomingWays should have size (1)
    incomingWays("1") should equal(List("1-1"))
  }

  test("incomingWays, 2") {
    val nodeIds = List(1, 2, 3).map(_.toString)
    val wayData1 = ("1-1", true, List(1, 2).map(OsmNode(_, GeographicCoordinate(0, 0)))) 
    val wayData2 = ("2-1", false, List(2, 3).map(OsmNode(_, GeographicCoordinate(0, 0)))) 
    val waysData = List(wayData1, wayData2)
    val incomingWays = AntMap.computeIncomingWays(nodeIds, waysData)
    incomingWays should  have size (nodeIds.size)
    incomingWays("1") should equal (Nil)
    incomingWays("2") should equal (List("1-1", "2-1"))
    incomingWays("3") should equal (List("2-1"))
  }

  test("computeIncomingWays, oneWay") {
    val nodeIds = List("1")
    val wayData = ("1-1", true, (List(3, 2, 1)).map(OsmNode(_, GeographicCoordinate(0, 0))).toList)
    val incomingWays = AntMap.computeIncomingWays(nodeIds, List(wayData))
    incomingWays should have size (1)
    incomingWays("1") should equal(List("1-1"))
  }

  test("computeIncomingWays, oneWay, no incoming ways") {
    val nodeIds = List("1")
    val wayData = ("1-1", true, (1 to 3).map(OsmNode(_, GeographicCoordinate(0, 0))).toList)
    val incomingWays = AntMap.computeIncomingWays(nodeIds, List(wayData))
    incomingWays should have size (1)
    incomingWays("1") should equal(List())
  }

  test("outgoingWays") {
    val antNodeIds = (1 to 4).map(_.toString).toList
    val wayData1 = ("1-1", false, (1 to  2).map(OsmNode(_)).toList)
    val wayData2 = ("2-1", false, (2 to  3).map(OsmNode(_)).toList)
    val wayData3 = ("3-1", true, List(4, 2).map(OsmNode(_)).toList)
    val waysData = List(wayData1, wayData2, wayData3)
    val outgoingWays = AntMap.computeOutgoingWays(antNodeIds, waysData)
    outgoingWays should have size (antNodeIds.size)
    outgoingWays("1") should equal (List("1-1"))
    outgoingWays("2") should equal (List("1-1", "2-1"))
    outgoingWays("3") should equal (List("2-1"))
    outgoingWays("4") should equal (List("3-1"))
  }
  
  test("") {
    val nodes = (1 to 2).map(OsmNode(_)).toList
    val way = OsmWay(1, nodes)
    val osmMap = OsmMap(nodes, Iterable(way))
    val antMap = AntMap(osmMap)
  }
}
