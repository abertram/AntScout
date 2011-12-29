package de.fhwedel.antscout
package osm

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 20.11.11
 * Time: 11:44
 */

class NodeTest extends FunSuite with ShouldMatchers {
  test("parseNode, valid node") {
    val node = OsmNode.parseNode(<node id="1" lat="1.0" lon="1.0"/>)
    node.id should be("1")
    node.geographicCoordinate.latitude should be(1)
    node.geographicCoordinate.longitude should be(1)
  }

  test("distanceTo, Fasanenweg") {
    val node1 = OsmNode(1, new GeographicCoordinate(53.7556053, 9.9441606))
    val node2 = OsmNode(2, new GeographicCoordinate(53.7538758, 9.9483534))
    node1 distanceTo node2 should be(336.95584080 plusOrMinus 336.95584080 * 0.005)
  }

  test("distanceTo, Berlin - Tokio") {
    val node1 = OsmNode(1, new GeographicCoordinate(52.5167, 13.4000))
    val node2 = OsmNode(2, new GeographicCoordinate(35.7000, 139.7667))
    node1 distanceTo node2 should be(8941201.228 plusOrMinus 8941201.228 * 0.005)
  }

  test("distanceTo, Flinders Peak - Buninyong") {
    val node1 = OsmNode(1, new GeographicCoordinate(-37.95103341666667, 144.42486788888888))
    val node2 = OsmNode(2, new GeographicCoordinate(-37.65282113888889, 143.92649552777777))
    node1 distanceTo node2 should be(54972.271 plusOrMinus 54972.271 * 0.005)
  }

  test("distanceTo, nearly antipodal points 1") {
    val node1 = OsmNode(1, new GeographicCoordinate(0, 0))
    val node2 = OsmNode(2, new GeographicCoordinate(0.5, 179.5))
    // richtiges Ergebnis: 19936288.579 m
    node1 distanceTo node2 should be(0.0)
  }

  test("distanceTo, nearly antipodal points 2") {
    val node1 = OsmNode(1, new GeographicCoordinate(0, 0))
    val node2 = OsmNode(2, new GeographicCoordinate(0.5, 179.0))
    node1 distanceTo node2 should be(19944127.421 plusOrMinus 19944127.421 * 0.005)
  }
}