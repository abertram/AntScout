package de.fhwedel.antscout
package routing

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 15.01.12
 * Time: 10:23
 */

class RoutingServiceTest extends FunSuite with ShouldMatchers {

/*
  test("apply") {
    val nodes = (1 to 5).map(Node(_))
    val ways = (1 to 4).map(id => AntWay(id.toString, nodes(id - 1), nodes(id), 0, 0))
    RoutingService(nodes, ways)
    RoutingService.routingTable.keySet should be (nodes.toSet)
    awaitCond(RoutingService.ways.size > 0)
    RoutingService.ways.toSet should be ((0 to ways.size - 1).map(i => (ways(i) -> (nodes(i) -> nodes(i + 1)))).toSet)
  }
  
  test("updatePropabilities") {
    val nodes = (1 to 5).map(Node(_))
    val ways = (1 to 4).map(id => AntWay(id.toString, nodes(id - 1), nodes(id), 0, 0))
    RoutingService(nodes, ways)
    awaitCond(RoutingService.ways.size > 0)
    RoutingService.updatePropabilities(nodes(0), nodes(1), ways.toList)
    RoutingService.routingTable(nodes(0))(nodes(1)) should  be (ways.toList)
    RoutingService.updatePropabilities(nodes(0), nodes(1), ways.toList.reverse)
    RoutingService.routingTable(nodes(0))(nodes(1)) should  be (ways.toList.reverse)
  }
*/
}
