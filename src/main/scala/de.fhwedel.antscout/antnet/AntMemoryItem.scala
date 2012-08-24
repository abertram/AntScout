package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef

case class AntMemoryItem(node: ActorRef, way: AntWay, tripTime: Double)
