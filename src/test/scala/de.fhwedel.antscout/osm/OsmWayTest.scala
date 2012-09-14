package de.fhwedel.antscout
package osm

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 24.11.11
 * Time: 18:20
 */

class OsmWayTest extends FunSuite with ShouldMatchers {
  test("parseWay, valid way") {
    val node1 = OsmNode(1, new GeographicCoordinate(1.0, 1.0))
    val node2 = OsmNode(2, new GeographicCoordinate(2.0, 2.0))
    val nodes = Map("1" -> node1, "2" -> node2)
    val way = OsmWay.parseWay(
      <way id="1">
          <nd ref="1"/>
          <nd ref="2"/>
          <tag k="highway" v="motorway"/>
          <tag k="maxspeed" v="1"/>
          <tag k="name" v="Test way"/>
      </way>, nodes)
    way.id should be("1")
    way.nodes should have length (2)
    way.nodes(0) should equal(node1)
    way.nodes(1) should equal(node2)
    way.name should be("Test way")
    way.maxSpeed should be(1.0)
  }

  test("parseWay, no name") {
    val nodes = Map.empty[String, OsmNode]
    val way = OsmWay.parseWay(<way id="1"/>, nodes)
    way.name should be("")
  }

  test("parseWay, empty name") {
    val nodes = Map.empty[String, OsmNode]
    val way = OsmWay.parseWay(
      <way id="1">
          <tag k="name" v=" "/>
      </way>, nodes)
    way.name.trim() should be ('empty)
  }

  test("parseWay, no maxspeed tag") {
    val nodes = Map.empty[String, OsmNode]
    val way = OsmWay.parseWay(<way id="1"/>, nodes)
    way.maxSpeed should be (Settings.defaultSpeed("default").get)
  }

  test("parseWay, empty maxspeed tag") {
    val nodes = Map.empty[String, OsmNode]
    val way = OsmWay.parseWay(
      <way id="1">
          <tag k="maxspeed" v=" "/>
      </way>, nodes)
    way.maxSpeed should be (Settings.defaultSpeed("default").get)
  }

  test("parseWay, maxspeed is not a number") {
    val nodes = Map.empty[String, OsmNode]
    val way = OsmWay.parseWay(
      <way id="1">
          <tag k="maxspeed" v="maxspeed"/>
      </way>, nodes)
    way.maxSpeed should be (Settings.defaultSpeed("default").get)
  }

  test("parseWay, no maxspeed tag, speed from highway tag") {
    val nodes = Map.empty[String, OsmNode]
    val way = OsmWay.parseWay(
      <way id="1">
          <tag k="highway" v="motorway"/>
      </way>, nodes)
    way.maxSpeed should be (Settings.defaultSpeed("motorway").get)
  }

  test("parseWay, empty maxspeed tag, speed from highway tag") {
    val nodes = Map.empty[String, OsmNode]
    val way = OsmWay.parseWay(
      <way id="1">
          <tag k="highway" v="motorway"/>
          <tag k="maxspeed" v=" "/>
      </way>, nodes)
    way.maxSpeed should be (Settings.defaultSpeed("motorway").get)
  }

  test("parseWay, maxspeed is not a number, speed from highway tag") {
    val nodes = Map.empty[String, OsmNode]
    val way = OsmWay.parseWay(
      <way id="1">
          <tag k="highway" v="motorway"/>
          <tag k="maxspeed" v="maxspeed"/>
      </way>, nodes)
    way.maxSpeed should be (Settings.defaultSpeed("motorway").get)
  }

  test("parseWay, oneway = yes") {
    val node1 = OsmNode(1, new GeographicCoordinate(1.0, 1.0))
    val node2 = OsmNode(2, new GeographicCoordinate(2.0, 2.0))
    val nodes = Map("1" -> node1, "2" -> node2)
    val way = OsmWay.parseWay(
      <way id="1">
          <nd ref="1"/>
          <nd ref="2"/>
          <tag k="highway" v="motorway"/>
          <tag k="maxspeed" v="1"/>
          <tag k="name" v="Test way"/>
          <tag k="oneway" v="yes"/>
      </way>, nodes)
    way.isInstanceOf[OsmOneWay] should be (true)
  }

  test("parseWay, oneway = no") {
    val node1 = OsmNode(1, new GeographicCoordinate(1.0, 1.0))
    val node2 = OsmNode(2, new GeographicCoordinate(2.0, 2.0))
    val nodes = Map("1" -> node1, "2" -> node2)
    val way = OsmWay.parseWay(
      <way id="1">
          <nd ref="1"/>
          <nd ref="2"/>
          <tag k="highway" v="motorway"/>
          <tag k="maxspeed" v="1"/>
          <tag k="name" v="Test way"/>
          <tag k="oneway" v="no"/>
      </way>, nodes)
    way.isInstanceOf[OsmWay] should be (true)
  }

  test("parseWay, oneway = -1") {
    val node1 = OsmNode(1, new GeographicCoordinate(1.0, 1.0))
    val node2 = OsmNode(2, new GeographicCoordinate(2.0, 2.0))
    val nodes = Map("1" -> node1, "2" -> node2)
    val way = OsmWay.parseWay(
      <way id="1">
          <nd ref="1"/>
          <nd ref="2"/>
          <tag k="highway" v="motorway"/>
          <tag k="maxspeed" v="1"/>
          <tag k="name" v="Test way"/>
          <tag k="oneway" v="-1"/>
      </way>, nodes)
    way.isInstanceOf[OsmOneWay] should be (true)
    way.nodes(0) should equal (node2)
    way.nodes(1) should equal (node1)
  }

  // Tests zur Berechnung von Tunnel-Längen
  /*
      test("Elbtunnel") {
          val node1 = new Node(1, new GeographicCoordinate(53.5537094f, 9.8971024f))
          val node2 = new Node(2, new GeographicCoordinate(53.5510178f, 9.898565f))
          val node3 = new Node(3, new GeographicCoordinate(53.5473983f, 9.9030333f))
          val node4 = new Node(4, new GeographicCoordinate(53.537068f, 9.9284762f))
          val node5 = new Node(5, new GeographicCoordinate(53.5358357f, 9.9304749f))
          val nodes = List[Node](node1, node2, node3, node4, node5)
          val length = nodes.zip(nodes.tail).map(n => n._1 distanceTo n._2).sum
          val tunnelLength = nodes.zip(nodes.tail).map(n => {
              val theta1 = math.Pi / 2 - n._1.geographicCoordinate.latitude.toRadians
              val lambda1 = n._1.geographicCoordinate.longitude.toRadians
              val theta2 = math.Pi / 2 - n._2.geographicCoordinate.latitude.toRadians
              val lambda2 = n._2.geographicCoordinate.longitude.toRadians
              val dX = math.cos(theta2) * math.cos(lambda2) - math.cos(theta1) * math.cos(lambda1)
              val dY = math.cos(theta2) * math.sin(lambda2) - math.cos(theta1) * math.sin(lambda1)
              math.sqrt(math.pow(dX, 2) + math.pow(dY, 2)) * 6371009
          }).sum
          // 3325 m
          length should be (tunnelLength)
      }

      test("Lærdalstunnel") {
          val nodes = List(
              new Node(1, new GeographicCoordinate(61.0641471f, 7.5030956f)),
              new Node(2, new GeographicCoordinate(61.0638896f, 7.501791f)),
              new Node(3, new GeographicCoordinate(61.0634245f, 7.5000229f)),
              new Node(4, new GeographicCoordinate(61.0626812f, 7.4979544f)),
              new Node(5, new GeographicCoordinate(61.061672f, 7.4956198f)),
              new Node(6, new GeographicCoordinate(61.0606129f, 7.4934483f)),
              new Node(7, new GeographicCoordinate(60.9483141f, 7.315854f)),
              new Node(8, new GeographicCoordinate(60.9401439f, 7.3021211f)),
              new Node(9, new GeographicCoordinate(60.9270505f, 7.2784318f)),
              new Node(10, new GeographicCoordinate(60.9233801f, 7.2710504f)),
              new Node(11, new GeographicCoordinate(60.9202098f, 7.2641839f)),
              new Node(12, new GeographicCoordinate(60.9001381f, 7.2177495f)),
              new Node(13, new GeographicCoordinate(60.8994911f, 7.2162217f)),
              new Node(14, new GeographicCoordinate(60.898907f, 7.2147322f)))
          val length = nodes.zip(nodes.tail).map(n => n._1 distanceTo n._2).sum
          val tunnelLength = nodes.zip(nodes.tail).map(n => {
              val theta1 = math.Pi / 2 - n._1.geographicCoordinate.latitude.toRadians
              val lambda1 = n._1.geographicCoordinate.longitude.toRadians
              val theta2 = math.Pi / 2 - n._2.geographicCoordinate.latitude.toRadians
              val lambda2 = n._2.geographicCoordinate.longitude.toRadians
              val dX = math.cos(theta2) * math.cos(lambda2) - math.cos(theta1) * math.cos(lambda1)
              val dY = math.cos(theta2) * math.sin(lambda2) - math.cos(theta1) * math.sin(lambda1)
              math.sqrt(math.pow(dX, 2) + math.pow(dY, 2)) * 6371009
          }).sum
          // 24509 m
          length should be (tunnelLength)
      }
  */
}
