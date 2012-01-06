package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 06.01.12
 * Time: 16:37
 */

class TrafficModelItemTest extends FunSuite with ShouldMatchers {
  
  test("+=") {
    val tmi = TrafficModelItem(0, 2)
    tmi += 1
    tmi.tripTimes should be (Seq(1))
    tmi += 2
    tmi.tripTimes should be (Seq(2, 1))
    tmi += 3
    tmi.tripTimes should be (Seq(3, 2))
  }
}