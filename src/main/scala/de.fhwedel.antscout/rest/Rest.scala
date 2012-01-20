package de.fhwedel.antscout
package rest

import net.liftweb.http.rest.RestHelper
import antnet.AntMap
import net.liftweb.json.Extraction
import net.liftweb.json.JsonAST.{JArray, JObject, JString, JField}
import osm.OsmMap
import net.liftweb.json.JsonDSL._
import net.liftweb.http.{S, Req}
import routing.RoutingService
import akka.actor.{Actor, ActorRef}
import net.liftweb.common.{Logger, Full, Box}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 12.01.12
 * Time: 18:32
 */

object Rest extends Logger with RestHelper {

  serve {
    case Get(List("antnodes"), _) => {
      JArray(AntMap.nodes.keys.map(k => {
        val osmNode = OsmMap.nodes(k)
        ("id" -> k) ~
        ("latitude" -> osmNode.geographicCoordinate.latitude) ~
        ("longitude" -> osmNode.geographicCoordinate.longitude)
      }).toList)
    }
    case Req("directions" :: Nil, _, _) =>
      for {
        sourceId <- S.param("source") ?~ "Source is missing" ~> 400
        destinationId <- S.param("destination") ?~ "Destination is missing" ~> 400
      } yield {
        val source = Actor.registry.actorsFor(sourceId).head
        val destination = Actor.registry.actorsFor(destinationId).head
        val path = RoutingService.findPath(source, destination)
        JArray(path.map(w => JString("%s (%s)".format(OsmMap.ways(w.id.split("-").head).name, w.id))))
      }
    case Get(List("osmnodes"), _) => {
      JArray(OsmMap.nodes.values.map(n => {
        ("id" -> n.id) ~
        ("latitude" -> n.geographicCoordinate.latitude) ~
        ("longitude" -> n.geographicCoordinate.longitude)
      }).toList)
    }

  }
}