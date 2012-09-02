package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 05.01.12
 * Time: 01:21
 */

class PheromoneMatrixTest extends FunSuite with ShouldMatchers {
/*
  test("apply") {
    val n = Node(0)
    val ds = (1 to 2).map(Node(_)).toList
    val ows = (1 to 2).map(id => AntWay(id.toString, n, ds(id - 1), 1, 1)).toList
    val tts = ows.map(aw => (aw, 1.0)).toMap
    val pm = PheromoneMatrix(ds.toIterable.view, ows, tts)
    pm.keySet should be (ds.toSet)
    pm.heuristicValues.toSet should be (Map((ows(0) -> 0.5), ows(1) -> 0.5).toSet)
    pm.pheromones.toSet should be (Map(ds(0) -> Map(ows(0) -> 0.5, ows(1) -> 0.5), ds(1) -> Map(ows(0) -> 0.5, ows(1) -> 0.5)).toSet)
    pm(ds(0))((ows(0))) should be (0.615384615 plusOrMinus 0.000000001)
    pm(ds(0))((ows(1))) should be (0.615384615 plusOrMinus 0.000000001)
    pm(ds(1))((ows(0))) should be (0.615384615 plusOrMinus 0.000000001)
    pm(ds(1))((ows(1))) should be (0.615384615 plusOrMinus 0.000000001)
  }

  test("updatePheromones") {
    val n = Node(0)
    val ds = (1 to 2).map(Node(_)).toList
    val ows = (1 to 2).map(id => AntWay(id.toString, n, ds(id - 1), 1, 1)).toList
    val tts = ows.map(aw => (aw, 1.0)).toMap
    val pm = PheromoneMatrix(ds.toIterable.view, ows, tts)
    val oldPheromones = pm.pheromones(ds(0)).toMap
    val oldPropabilities = pm(ds(0)).toMap
    pm updatePheromones (ds(0), ows(0), 1)
    pm.pheromones(ds(0))(ows(0)) should be > (oldPheromones(ows(0)))
    pm.pheromones(ds(0))(ows(1)) should be < (oldPheromones(ows(1)))
    pm(ds(0))(ows(0)) should be > (oldPropabilities(ows(0)))
    pm(ds(0))(ows(1)) should be < (oldPropabilities(ows(1)))
  }
*/
}
