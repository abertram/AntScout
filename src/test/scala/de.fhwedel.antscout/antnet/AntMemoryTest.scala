package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 06.01.12
 * Time: 00:10
 */

class AntMemoryTest extends FunSuite with ShouldMatchers {
/*
  test("containsNode") {
    val am = AntMemory()
    val node = Node(0)
    val wayNodes = (1 to 2).map(Node(_))
    val way = AntWay("0", wayNodes(0), wayNodes(1), 0, 0)
    am.memorize(node, way, 0)
    am.containsNode(node) should be (true)
    am.containsNode(wayNodes(0)) should be (false)
  }

  test("containsWay") {
    val am = AntMemory()
    val node = Node(0)
    val wayNodes = (1 to 4).map(Node(_))
    val way1 = AntWay("1", wayNodes(0), wayNodes(1), 0, 0)
    val way2 = AntWay("2", wayNodes(2), wayNodes(3), 0, 0)
    am.memorize(node, way1, 0)
    am.containsWay(way1) should be (true)
    am.containsWay(way2) should be (false)
  }

  test("memorize") {
    val am = AntMemory()
    val node1 = Node(1)
    val node2 = Node(2)
    val wayNodes = (3 to 6).map(Node(_))
    val way1 = AntWay("1", wayNodes(0), wayNodes(1), 0, 0)
    val way2 = AntWay("2", wayNodes(2), wayNodes(3), 0, 0)
    am.memorize(node1, way1, 0)
    am.memorize(node2, way2, 0)
    am.items should be (Iterable[AntMemoryItem](AntMemoryItem(node2, way2, 0), AntMemoryItem(node1, way1, 0)))
  }

  test("removeCircle") {
    val am = AntMemory()
    val node1 = Node(1)
    val node2 = Node(2)
    val wayNodes = (3 to 6).map(Node(_))
    val way1 = AntWay("1", wayNodes(0), wayNodes(1), 0, 0)
    val way2 = AntWay("2", wayNodes(2), wayNodes(3), 0, 0)
    am.memorize(node1, way1, 0)
    am.memorize(node2, way2, 0)
    am.removeCircle(node1)
    am.items should be ('empty)
  }
*/
}