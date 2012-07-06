package de.fhwedel.antscout
package comet

import net.liftweb.http.NamedCometActorTrait
import net.liftweb.common.{Full, Box}
import net.liftweb.http.js.JE.Call
import antnet.AntWay
import net.liftweb.json.JsonAST.JArray

class OpenLayers extends NamedCometActorTrait {

  import OpenLayers._

  override def lowPriority = {
    case DrawPath(path) => {
      val pathAsJson = path match {
        case Full(path) => JArray(path.map(_.toJson).toList)
        case _ => JArray(List[AntWay]().map(_.toJson))
      }
      partialUpdate(Call("AntScout.drawPath", pathAsJson).cmd)
    }
    case m: Any => logger.warn("Unknown message")
  }

  def render = Call("console.log", ".").cmd
}

object OpenLayers {

  case class DrawPath(path: Box[Seq[AntWay]])
}
