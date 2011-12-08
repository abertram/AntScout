package de.fhwedel.antscout
package antnet

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 08.12.11
 * Time: 14:27
 */

class AntOneWay(id: String, startNode: AntNode, endNode: AntNode, length: Double) extends AntWay(id, startNode, endNode, length)

object AntOneWay {
  def apply(id: Int, startNode: AntNode, endNode: AntNode) = new AntOneWay(id.toString, startNode, endNode, 0.0)
}
