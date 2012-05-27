package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import akka.actor.{PoisonPill, Actor}
import java.util.concurrent.TimeUnit
import net.liftweb.util.Props
import util.Random
import extensions.ExtendedDouble._
import akka.util.Duration

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 25.12.11
 * Time: 15:21
 */

class ForwardAnt(val source: AntNode, val destination: AntNode) extends Actor with Logger {
  
  val DefaultAntLifetime = 30
  val DefaultTimeUnit = "SECONDS"

  var currentNode: AntNode = _
  var currentWay: AntWay = _
  val memory = AntMemory()
  val random = Random
  var startTime: Long = _

  override def debug(msg: => AnyRef) {
    super.debug("#%s: %s".format(self.path, msg))
  }

  override def info(msg: => AnyRef) {
    super.info("#%s: %s".format(self.path, msg))
  }

  def launchBackwardAnt() {
    BackwardAnt(source, destination, memory)
  }

  override def preStart() {
    val antLifetime = Props.getInt("antLifetime", DefaultAntLifetime)
    val timeUnit = TimeUnit.valueOf(Props.get("timeUnit", DefaultTimeUnit))
    context.system.scheduler.scheduleOnce(Duration(antLifetime, timeUnit)) {
//      info("Committing suicide!")
      self ! PoisonPill
    }
    startTime = System.currentTimeMillis
    visitNode(source)
  }

  protected def receive = {
    case AntNode.Propabilities(propabilities) =>
//      debug("Propabilities")
      val nextWay = selectWay(propabilities)
      val (nextNode, tripTime) = nextWay cross currentNode
      memory.memorize(currentNode, currentWay, tripTime)
      visitNode(nextNode)
    case m: Any =>
      warn("Unknown message: %s".format(m))
  }

  def selectWay(propabilities: Map[AntWay, Double]) = {
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
    super.trace("#%s: %s".format(self.path, msg))
  }

  def visitNode(node: AntNode) {
//    debug("Visiting node #%s".format(node id))
    currentNode = node
    // Der Knoten hat keine ausgehenden Wege, wir sind in einer Sackgasse angekommen.
    if (!AntMap.outgoingWays.contains(node))
      self ! PoisonPill
    else if (node == destination) {
//      debug("Destination reached, needed %s ms" format(System.currentTimeMillis - startTime))
      launchBackwardAnt()
      self ! PoisonPill
    } else {
      if (memory.containsNode(node)) {
//        debug("Circle detected")
        memory.removeCircle(node)
      }
      node.enter(destination, self)
    }
  }
}
