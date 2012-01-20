package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import akka.actor.{PoisonPill, Scheduler, ActorRef, Actor}
import java.util.concurrent.TimeUnit
import net.liftweb.util.Props
import util.Random
import extensions.ExtendedDouble._

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 25.12.11
 * Time: 15:21
 */

class ForwardAnt(val sourceNode: ActorRef, val destinationNode: ActorRef) extends Actor with Logger {
  
  val DefaultAntLifetime = 30
  val DefaultTimeUnit = "SECONDS"

  var currentNode: ActorRef = _
  var currentWay: ActorRef = _
  val memory = AntMemory()
  val random = Random

  override def debug(msg: => AnyRef) {
    super.debug("#%s: %s".format(self id, msg))
  }

  override def info(msg: => AnyRef) {
    super.info("#%s: %s".format(self id, msg))
  }

  def launchBackwardAnt() {
    BackwardAnt(sourceNode, destinationNode, memory)
  }

  override def preStart() {
    if (sourceNode == destinationNode) {
//      warn("Source node equals destination node, exit!")
      self.stop()
    } else {
      self id = "%s-%s".format(sourceNode id, destinationNode id)
      val antLifetime = Props.getInt("antLifetime", DefaultAntLifetime)
      val timeUnit = TimeUnit.valueOf(Props.get("timeUnit", DefaultTimeUnit))
      Scheduler.scheduleOnce(() => {
//        info("Committing suicide!")
        self ! PoisonPill
      }, antLifetime, timeUnit)
      visitNode(sourceNode)
    }
  }

  protected def receive = {
    case EndNode(n, tt) => {
//      debug("EndNode(%s)".format(n id))
      memory.memorize(currentNode, currentWay, tt)
      visitNode(n)
    }
    case Propabilities(ps) => selectWay(ps) ! Cross(currentNode)
    case m: Any => warn("Unknown message: %s".format(m))
  }

  def selectWay(propabilities: Map[ActorRef, Double]) = {
//    debug("Memory: %s".format(memory.items))
    val notVisitedWays = propabilities.filter { case (w, p) => !memory.containsWay(w) }
//    debug("Not visited ways: %s".format(notVisitedWays.map(_._1.id).mkString(", ")))
    currentWay = if (!notVisitedWays.isEmpty) {
      // Höchste Wahrscheinlichkeit bestimmen
      val maxPropability = notVisitedWays.maxBy(_._2)._2
      // Noch nicht besuchte Wege mit der höchsten Wahrscheinlichkeit bestimmen
      val maxPropabilityNotVisitedWays = notVisitedWays.filter(_._2 ~= maxPropability)
      if (maxPropabilityNotVisitedWays.size == 1)
        // Wenn nur ein Weg gefunden wurde, diesen Weg verwenden
        maxPropabilityNotVisitedWays.head._1
      else
        // Wenn mehrere Wege gefunden werden, zufällig einen bestimmen
        maxPropabilityNotVisitedWays.toSeq(random.nextInt(maxPropabilityNotVisitedWays.size))._1
    } else
      propabilities.toSeq(random.nextInt(propabilities.size))._1
//    debug("Selected way: #%s".format(currentWay id))
//    Thread.sleep(30000)
    currentWay
  }

  override def trace(msg: => AnyRef) {
    super.trace("#%s: %s".format(self id, msg))
  }

  def visitNode(node: ActorRef) {
//    debug("Visiting node #%s".format(node id))
    currentNode = node
    if (node != destinationNode) {
      if (memory.containsNode(node)) {
//        debug("Circle detected")
        memory.removeCircle(node)
      }
      node ! Enter(destinationNode)
    } else {
      trace("Destination reached")
      launchBackwardAnt()
      self.stop()
    }
  }
}

object ForwardAnt {
  def apply(sourceNode: ActorRef, destinationNode: ActorRef) = new ForwardAnt(sourceNode, destinationNode)
}

case class EndNode(node: ActorRef, tripTime: Double)
case class Propabilities(propabilities: Map[ActorRef, Double])
