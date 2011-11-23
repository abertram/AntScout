package de.fhwedel.antscout
package openstreetmap

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 15:13
 */

class Way(val id: Int, val name: String, val nodes: Vector[Node]) {
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
}
