package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import osm.{OsmMap, OsmWay, GeographicCoordinate, OsmNode}
import xml.XML

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 03.12.11
 * Time: 09:42
 */

class AntMapTest extends FunSuite with ShouldMatchers {
  test("convertOsmWayToAntWays") {
    val osmNode1 = new OsmNode(1, new GeographicCoordinate(1, 1))
    val osmNode2 = new OsmNode(2, new GeographicCoordinate(2, 2))
    val osmNode3 = new OsmNode(3, new GeographicCoordinate(3, 3))
    val osmWay = new OsmWay(1, "", Vector(osmNode1, osmNode2, osmNode3), 0)
    val antNode1 = AntNode(1)
    val antNode3 = AntNode(3)
    val antNodes = Map(1 -> antNode1, 3 -> antNode3)
    val antWays = AntMap.convertOsmWayToAntWays(osmWay, antNodes)
    antWays should have size (1)
    antWays.head.id should be ("1-1")
    antWays.head.startNode.id should be (1)
    antWays.head.endNode.id should be (3)
  }
  
  test("incomingWays, 1") {
    val node1 = AntNode(1)
    val node2 = AntNode(2)
    val node3 = AntNode(3)
    val node4 = AntNode(4)
    val nodes = Iterable(node1, node2, node3, node4)
    val way1 = AntWay(1, node1, node2)
    val way2 = AntWay(2, node2, node3)
    val way3 = AntOneWay(3, node4, node2).asInstanceOf[AntWay]
    val ways = Iterable(way1, way2, way3)
    val antMap = AntMap(nodes, ways)
    antMap.incomingWays should have size (4)
    antMap.incomingWays(node1) should have size (1)
    antMap.incomingWays(node1) should contain (way1)
    antMap.incomingWays(node2) should have size (3)
    antMap.incomingWays(node2) should contain (way1)
    antMap.incomingWays(node2) should contain (way2)
    antMap.incomingWays(node2) should contain (way3)
    antMap.incomingWays(node3) should have size (1)
    antMap.incomingWays(node3) should contain (way2)
    antMap.incomingWays(node4) should have size (0)
  }

  test("incomingWays, 2") {
    val node1 = AntNode(1)
    val node2 = AntNode(2)
    val node3 = AntNode(3)
    val node4 = AntNode(4)
    val nodes = Iterable(node1, node2, node3, node4)
    val way1 = AntWay(1, node1, node2)
    val way2 = AntWay(2, node2, node3)
    val way3 = AntOneWay(3, node2, node4).asInstanceOf[AntWay]
    val ways = Iterable(way1, way2, way3)
    val antMap = AntMap(nodes, ways)
    antMap.incomingWays should have size (4)
    antMap.incomingWays(node1) should have size (1)
    antMap.incomingWays(node1) should contain (way1)
    antMap.incomingWays(node2) should have size (2)
    antMap.incomingWays(node2) should contain (way1)
    antMap.incomingWays(node2) should contain (way2)
    antMap.incomingWays(node3) should have size (1)
    antMap.incomingWays(node3) should contain (way2)
    antMap.incomingWays(node4) should have size (1)
    antMap.incomingWays(node4) should contain (way3)
  }

  test("outgoingWays, 1") {
    val node1 = AntNode(1)
    val node2 = AntNode(2)
    val node3 = AntNode(3)
    val node4 = AntNode(4)
    val nodes = Iterable(node1, node2, node3, node4)
    val way1 = AntWay(1, node1, node2)
    val way2 = AntWay(2, node2, node3)
    val way3 = AntOneWay(3, node4, node2).asInstanceOf[AntWay]
    val ways = Iterable(way1, way2, way3)
    val antMap = AntMap(nodes, ways)
    antMap.outgoingWays should have size (4)
    antMap.outgoingWays(node1) should have size (1)
    antMap.outgoingWays(node1) should contain (way1)
    antMap.outgoingWays(node2) should have size (2)
    antMap.outgoingWays(node2) should contain (way1)
    antMap.outgoingWays(node2) should contain (way2)
    antMap.outgoingWays(node3) should have size (1)
    antMap.outgoingWays(node3) should contain (way2)
    antMap.outgoingWays(node4) should have size (1)
    antMap.outgoingWays(node4) should contain (way3)
  }

  test("outgoingWays, 2") {
    val node1 = AntNode(1)
    val node2 = AntNode(2)
    val node3 = AntNode(3)
    val node4 = AntNode(4)
    val nodes = Iterable(node1, node2, node3, node4)
    val way1 = AntWay(1, node1, node2)
    val way2 = AntWay(2, node2, node3)
    val way3 = AntOneWay(3, node2, node4).asInstanceOf[AntWay]
    val ways = Iterable(way1, way2, way3)
    val antMap = AntMap(nodes, ways)
    antMap.outgoingWays should have size (4)
    antMap.outgoingWays(node1) should have size (1)
    antMap.outgoingWays(node1) should contain (way1)
    antMap.outgoingWays(node2) should have size (3)
    antMap.outgoingWays(node2) should contain (way1)
    antMap.outgoingWays(node2) should contain (way2)
    antMap.outgoingWays(node2) should contain (way3)
    antMap.outgoingWays(node3) should have size (1)
    antMap.outgoingWays(node3) should contain (way2)
    antMap.outgoingWays(node4) should have size (0)
  }

  test("") {
    val osmData = XML.loadString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><osm version=\"0.6\" generator=\"Overpass API\"><note>The data included in this document is from www.openstreetmap.org. It has there been collected by a large group of contributors. For individual attribution of each item please refer to http://www.openstreetmap.org/api/0.6/[node|way|relation]/#id/history </note><meta osm_base=\"2011-12-09T09\\:56\\:02Z\"/><node id=\"26316282\" lat=\"53.5468489\" lon=\"10.0116903\"/><node id=\"26316285\" lat=\"53.5459268\" lon=\"10.0143962\"/><node id=\"26316286\" lat=\"53.5455610\" lon=\"10.0155103\"/><node id=\"29547265\" lat=\"53.5476913\" lon=\"10.0087891\"/><node id=\"155475676\" lat=\"53.5474036\" lon=\"10.0101152\"/><node id=\"155477654\" lat=\"53.5464285\" lon=\"10.0129075\"/><node id=\"161976211\" lat=\"53.5459694\" lon=\"10.0142317\"/><node id=\"161976301\" lat=\"53.5461175\" lon=\"10.0143069\"/><node id=\"161976404\" lat=\"53.5463197\" lon=\"10.0142645\"/><node id=\"161976511\" lat=\"53.5464801\" lon=\"10.0141244\"/><node id=\"161976614\" lat=\"53.5468552\" lon=\"10.0138241\"/><node id=\"232443091\" lat=\"53.5454979\" lon=\"10.0161451\"/><node id=\"232457941\" lat=\"53.5455119\" lon=\"10.0159585\"><tag k=\"TMC:cid_58:tabcd_1:Class\" v=\"Point\"/><tag k=\"TMC:cid_58:tabcd_1:Direction\" v=\"negative\"/><tag k=\"TMC:cid_58:tabcd_1:LCLversion\" v=\"8.00\"/><tag k=\"TMC:cid_58:tabcd_1:LocationCode\" v=\"53761\"/><tag k=\"TMC:cid_58:tabcd_1:NextLocationCode\" v=\"23249\"/><tag k=\"TMC:cid_58:tabcd_1:PrevLocationCode\" v=\"53757\"/><tag k=\"bicycle\" v=\"yes\"/><tag k=\"crossing\" v=\"traffic_signals\"/><tag k=\"highway\" v=\"traffic_signals\"/></node><node id=\"250282555\" lat=\"53.5476709\" lon=\"10.0090232\"/><node id=\"250282556\" lat=\"53.5476165\" lon=\"10.0093953\"/><node id=\"250282557\" lat=\"53.5475310\" lon=\"10.0097022\"/><node id=\"603195590\" lat=\"53.5463478\" lon=\"10.0131378\"/><node id=\"603195591\" lat=\"53.5463874\" lon=\"10.0132564\"/><node id=\"603195592\" lat=\"53.5463853\" lon=\"10.0133077\"/><node id=\"603195593\" lat=\"53.5463581\" lon=\"10.0133884\"/><node id=\"603195594\" lat=\"53.5462600\" lon=\"10.0136794\"/><node id=\"603195597\" lat=\"53.5460259\" lon=\"10.0142604\"/><node id=\"603195598\" lat=\"53.5460994\" lon=\"10.0141554\"/><node id=\"603195600\" lat=\"53.5460643\" lon=\"10.0142142\"/><node id=\"603195602\" lat=\"53.5474387\" lon=\"10.0100066\"><tag k=\"highway\" v=\"traffic_signals\"/></node><way id=\"4069392\"><nd ref=\"29547265\"/><nd ref=\"250282555\"/><nd ref=\"250282556\"/><nd ref=\"250282557\"/><nd ref=\"603195602\"/><nd ref=\"155475676\"/><nd ref=\"26316282\"/><nd ref=\"155477654\"/><nd ref=\"603195590\"/><nd ref=\"161976211\"/><nd ref=\"26316285\"/><nd ref=\"26316286\"/><nd ref=\"232457941\"/><nd ref=\"232443091\"/><tag k=\"cycleway\" v=\"track\"/><tag k=\"highway\" v=\"secondary\"/><tag k=\"maxspeed\" v=\"50\"/><tag k=\"name\" v=\"Högerdamm\"/><tag k=\"oneway\" v=\"yes\"/><tag k=\"postal_code\" v=\"20097\"/></way><way id=\"16091890\"><nd ref=\"161976211\"/><nd ref=\"603195597\"/><nd ref=\"161976301\"/><nd ref=\"161976404\"/><nd ref=\"161976511\"/><nd ref=\"161976614\"/><tag k=\"highway\" v=\"residential\"/><tag k=\"maxspeed\" v=\"50\"/><tag k=\"name\" v=\"Högerdamm\"/><tag k=\"postal_code\" v=\"20097\"/></way><way id=\"47382068\"><nd ref=\"603195590\"/><nd ref=\"603195591\"/><nd ref=\"603195592\"/><nd ref=\"603195593\"/><nd ref=\"603195594\"/><nd ref=\"603195598\"/><nd ref=\"603195600\"/><nd ref=\"603195597\"/><tag k=\"highway\" v=\"service\"/><tag k=\"service\" v=\"driveway\"/></way></osm>")
    val osmMap = OsmMap(osmData)
    val antMap = AntMap(osmMap)
  }
}
