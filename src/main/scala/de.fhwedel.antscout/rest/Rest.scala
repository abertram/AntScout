package de.fhwedel.antscout
package rest

import akka.pattern.ask
import akka.util.duration._
import net.liftweb.http.rest.RestHelper
import osm.OsmMap
import net.liftweb.json.JsonDSL._
import routing.RoutingService
import net.liftweb.common.Logger
import antnet.{AntWay, AntMap}
import akka.dispatch.Await
import akka.util.Timeout
import net.liftweb.json.JsonAST.JArray
import net.liftweb.http.S

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 12.01.12
 * Time: 18:32
 */

object Rest extends Logger with RestHelper {

  implicit val timeout = Timeout(5 seconds)

  serve {
    case Get(List("node", id), _) =>
      val incomingWays = AntMap.incomingWays.find {
        case (node, ways) => node.id == id
      } map {
        case (node, ways) => ways
      } getOrElse Set[AntWay]()
      val outgoingWays = AntMap.outgoingWays.find {
        case (node, ways) => node.id == id
      } map {
        case (node, ways) => ways
      } getOrElse Set[AntWay]()
      ("incomingWays" -> incomingWays.map(_.toJson)) ~
      ("outgoingWays" -> outgoingWays.map(_.toJson))
    case Get(List("nodes"), _) => {
      JArray(AntMap.nodes.map(n => {
        val osmNode = OsmMap.nodes(n.id)
        ("id" -> n.id) ~
        ("latitude" -> osmNode.geographicCoordinate.latitude) ~
        ("longitude" -> osmNode.geographicCoordinate.longitude)
      }).toList)
    }
    case Get(List("directions"), _) =>
      for {
        sourceId <- S.param("source") ?~ "Source is missing" ~> 400
        destinationId <- S.param("destination") ?~ "Destination is missing" ~> 400
      } yield {
        val source = AntMap.nodes.find(_.id == sourceId).get
        val destination = AntMap.nodes.find(_.id == destinationId).get
        val pathFuture = (AntScout.routingService ? RoutingService.FindPath(source, destination))
        val path = Await.result(pathFuture, 5 seconds).asInstanceOf[Seq[AntWay]]
        JArray(path.map { antWay =>
          ("id" -> antWay.id) ~
          ("nodes" -> antWay.nodes.map { node =>
            ("latitude" -> node.geographicCoordinate.latitude) ~
            ("longitude" -> node.geographicCoordinate.longitude)
          })
        }.toList)
      }
    case Get(List("osmnodes"), _) => {
      JArray(OsmMap.nodes.values.map(n => {
        ("id" -> n.id) ~
        ("latitude" -> n.geographicCoordinate.latitude) ~
        ("longitude" -> n.geographicCoordinate.longitude)
      }).toList)
    }
    case Get(List("ways"), _) => {
      JArray(AntMap.ways.map(_.toJson).toList)
    }
    case Get(List("way", id), _) =>
      AntMap.ways.find(_.id == id).map(_.toJson)
    case JsonPut(List("way", id), json -> _) =>
      // Weg suchen, der aktualisiert werden soll
      val way = AntMap.ways.find(_.id == id)
      // aktualisierte Daten extrahieren
      val wayUpdate = json.extract[AntWay.Update]
      way.map { way =>
        // Aktualisierung durchführen
        way.update(wayUpdate)
        // warten, bis die Aktualisierung durchgeführt wurde
        way.maxSpeed(true)
        // aktualisierten Weg zurückgeben
        way.toJson
      }
  }
}
