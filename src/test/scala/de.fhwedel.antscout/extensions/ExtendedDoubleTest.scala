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
    ((0.1 ~< 0.11)(0.01)) should be(true)
    ((0.1 ~< 0.1)(0.01)) should be(false)
    ((0.11 ~< 0.1)(0.01)) should be(false)
  }

  test("~=") {
    ((0.1 ~= 0.1)(0.01)) should be(true)
    ((0.1 ~= 0.1001)(0.01)) should be(true)
    ((0.1 ~= 0.2)(0.01)) should be(false)
  }
}