package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 28.12.11
 * Time: 23:47
 */

class NodeTest extends FunSuite with ShouldMatchers {

/*
  test("apply") {
    // alle Aktoren,die von anderen Tests gestartet wurden, stoppen, um das Testergebnis nicht zu verfälschen
    Actor.registry.shutdownAll()
    val node = Node(0)
    Actor.registry.actorsFor("0").size should be (1)
  }

  test("tripTimes") {
    val startNode = Node(1)
    val endNode1 = Node(2)
    val endNode2 = Node(3)
    val antWay1 = AntWay("1-1", startNode, endNode1, 2, 1)
    val antWay2 = AntWay("2-1", startNode, endNode2, 3, 1)
    val antWays = List(antWay1, antWay2)
    startNode ! OutgoingWays(antWays)
  }

  test("") {
    val osmData = XML loadFile("./maps/Ellerau-Zoomstufe-18-preprocessed.osm")
    val osmMap = OsmMap(osmData)
    AntMap()
//    Thread.sleep(10000)
  }
*/
}