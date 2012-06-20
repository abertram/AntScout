package de.fhwedel.antscout
package util

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 13.06.12
 * Time: 13:01
 */

class StatisticsTest extends FunSuite with ShouldMatchers {

  test("selectByProbability") {
    val result = (1 to 10000).map(i => Statistics.selectByProbability(Map(1 -> 0.5, 2 -> 0.6))).groupBy(i => i)
    Console.println(result(1).size.toDouble / (result(1).size + result(2).size))
    Console.println(result(2).size.toDouble / (result(1).size + result(2).size))
  }
}