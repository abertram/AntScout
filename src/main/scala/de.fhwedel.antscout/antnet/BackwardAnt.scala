package de.fhwedel.antscout
package antnet


/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 05.01.12
 * Time: 18:55
 */

class BackwardAnt(source: AntNode, destination: AntNode, memory: AntMemory) {

  memory.items.foreach {
    case AntMemoryItem(node, way, tripTime) => node.updateDataStructures(destination, way, tripTime)
  }
}

object BackwardAnt {

  def apply(source: AntNode, destination: AntNode, memory: AntMemory) = new BackwardAnt(source, destination, memory)
}