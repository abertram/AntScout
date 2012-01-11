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

  test("travelTimeRequest") {
    val startNode = AntNode("1")
    val endNode = AntNode("2")
    val antWay = AntWay("1-1", startNode, endNode, 2, 1)
    val travelTime = (antWay ? TravelTimeRequest).mapTo[(String, Double)].get
    travelTime should be ((antWay, 2))
  }
}