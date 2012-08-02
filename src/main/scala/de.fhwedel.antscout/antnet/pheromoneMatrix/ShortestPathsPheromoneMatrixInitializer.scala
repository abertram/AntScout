package de.fhwedel.antscout
package antnet.pheromoneMatrix

import akka.actor.ActorRef
import antnet.{AntWay, AntNodeSupervisor, AntNode, AntMap}

class ShortestPathsPheromoneMatrixInitializer(sources: Set[ActorRef], destinations: Set[ActorRef])
    extends PheromoneMatrixInitializer(sources, destinations) {

  def weight(i: ActorRef, j: ActorRef) = {
    AntNode.toNode(i).flatMap { iNode =>
      AntMap.outgoingWays(iNode).find { way =>
        way.endNode(i) == j
      }
    }.map { way =>
      way.tripTime
    }.getOrElse(Double.MaxValue)
  }

  def initPheromones = {
    val antNodes = AntMap.nodes.map { node =>
      AntScout.system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, node.id))
    }
    val weights = antNodes.map { iNode =>
      antNodes.map { jNode =>
        weight(iNode, jNode)
      }
    }
    Map[ActorRef, Map[ActorRef, Map[AntWay, Double]]]()
  }
}
