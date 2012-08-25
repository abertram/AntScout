package de.fhwedel.antscout
package extensions

import extensions.ExtendedDouble._
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 14.06.12
 * Time: 18:10
 */

class ExtendedDoubleTest extends FunSuite with ShouldMatchers {

  test("~<") {
    ((0.1 ~< 0.11)) should be(true)
    ((0.1 ~< 0.1)) should be(false)
    ((0.11 ~< 0.1)) should be(false)
  }

  test("~=") {
    ((0.1 ~= 0.1)) should be(true)
    ((0.1.~=(0.1001, 0.01))) should be(true)
    ((0.1 ~= 0.2)) should be(false)
  }

  test("round") {
    0.00.round(1) should equal(0)
    0.04.round(1) should equal(0)
    0.05.round(1) should equal(0.1)
    0.09.round(1) should equal(0.1)
    0.010.round(2) should equal(0.01)
    0.014.round(2) should equal(0.01)
    0.015.round(2) should equal(0.02)
    0.019.round(2) should equal(0.02)
    0.0010.round(3) should equal(0.001)
    0.0014.round(3) should equal(0.001)
    0.0015.round(3) should equal(0.002)
    0.0019.round(3) should equal(0.002)
  }
}