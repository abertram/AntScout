package de.fhwedel.antscout.antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 31.12.11
 * Time: 16:20
 */

class AntWayTest extends FunSuite with ShouldMatchers {

  test("tripTime") {
    val startNode = AntNode("1")
    val endNode = AntNode("2")
    val antWay = AntWay("1", startNode, endNode, 2, 1)
    antWay.tripTime should be (2)
  }

  test("maxSpeed") {
    val startNode = AntNode("1")
    val endNode = AntNode("2")
    val antWay = AntWay("1", startNode, endNode, 1, 1)
    antWay.maxSpeed should be (1)
  }

  test("change maxSpeed and read without await") {
    val startNode = AntNode("1")
    val endNode = AntNode("2")
    val antWay = AntWay("1", startNode, endNode, 1, 1)
    (1 to 100).map(i => antWay.maxSpeed = i)
    antWay.maxSpeed should not be (100)
  }

  test("change maxSpeed and read with await") {
    val startNode = AntNode("1")
    val endNode = AntNode("2")
    val antWay = AntWay("1", startNode, endNode, 1, 1)
    (1 to 100).map(i => antWay.maxSpeed = i)
    antWay.maxSpeed(true) should be (100)
  }
}
