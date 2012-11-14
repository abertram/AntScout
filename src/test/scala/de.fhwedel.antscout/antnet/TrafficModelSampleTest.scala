package de.fhwedel.antscout
package antnet

import extensions.ExtendedDouble._
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 06.01.12
 * Time: 16:37
 */

class TrafficModelSampleTest extends FunSuite with ShouldMatchers {
  
  test("+=") {
    val tmi = TrafficModelSample()
    tmi += 1
    tmi.tripTimes should be (Seq(1))
    tmi += 2
    tmi.tripTimes should be (Seq(2, 1))
    tmi += 3
    tmi.tripTimes should be (Seq(3, 2))
  }

  test("transformBySquash") {
    val parameters = for (x <- (0.05 to 0.95 by 0.1); n <- (1 to 5))
      yield (x, n)
    parameters foreach {
      case (x, n) => println("squash(%s, %s) = %s" format (x, n, TrafficModelSample.transformBySquash(x, n)))
    }
  }

  test("transformBySquash(0, 5)") {
    evaluating(TrafficModelSample.transformBySquash(0, 5)) should produce [IllegalArgumentException]
  }

  test("transformBySquash(0.1, 5)") {
    ((TrafficModelSample.transformBySquash(0.1, 5, 10)).~=(0.000000017, 0.1)) should be (true)
  }

  test("transformBySquash(0.5, 5)") {
    (TrafficModelSample.transformBySquash(0.5, 5, 10) ~= (0.150887324, 0.000001)) should be (true)
  }

  test("transformBySquash(1, 5)") {
    (TrafficModelSample.transformBySquash(1, 5, 10) ~= (1, 0.000001)) should be (true)
  }

  test("transformBySquash(1.1, 5)") {
    evaluating(TrafficModelSample.transformBySquash(1.1, 5)) should produce [IllegalArgumentException]
  }
}