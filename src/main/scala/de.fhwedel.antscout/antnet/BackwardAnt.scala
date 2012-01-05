package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import collection.mutable.Stack

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 05.01.12
 * Time: 18:55
 */

class BackwardAnt(source: ActorRef, destination: ActorRef, memory: Stack[(ActorRef, ActorRef)]) {

}

object BackwardAnt {

  def apply(source: ActorRef, destination: ActorRef, memory: Stack[(ActorRef, ActorRef)]) = new BackwardAnt(source, destination, memory)
}