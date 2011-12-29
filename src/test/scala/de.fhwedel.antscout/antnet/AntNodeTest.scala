package de.fhwedel.antscout
package antnet

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import akka.actor.Actor

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 28.12.11
 * Time: 23:47
 */

class AntNodeTest extends FunSuite with ShouldMatchers {

  test("apply") {
    // alle Aktoren,die von anderen Tests gestartet wurden, stoppen, um das Testergebnis nicht zu verfälschen
    Actor.registry.shutdownAll()
    AntNode(0)
    Actor.registry.actorsFor("0").size should be (1)
  }
}