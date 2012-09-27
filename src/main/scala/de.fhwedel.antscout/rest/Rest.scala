package de.fhwedel.antscout
package rest

import akka.pattern.ask
import akka.util.duration._
import net.liftweb.http.rest.RestHelper
import net.liftweb.json.JsonDSL._
import osm.OsmMap
import routing.RoutingService
import net.liftweb.common.{Box, Logger}
import antnet.{AntNodeSupervisor, AntWay, AntMap}
import akka.dispatch.Await
import akka.util.Timeout
import net.liftweb.json.JsonAST.JArray
import net.liftweb.http.S

object Rest extends Logger with RestHelper {

  implicit val timeout = Timeout(10 seconds)

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
    case Get(List("nodes"), _) =>
      AntMap.nodes.map { n =>
        val osmNode = OsmMap.nodes(n.id)
        ("id" -> n.id) ~
          ("latitude" -> osmNode.geographicCoordinate.latitude) ~
          ("longitude" -> osmNode.geographicCoordinate.longitude)
      }: JArray
    case Get(List("directions"), _) =>
      for {
        sourceId <- S.param("source") ?~ "Source is missing" ~> 400
        destinationId <- S.param("destination") ?~ "Destination is missing" ~> 400
        source = AntScout.system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName,
          sourceId))
        destination = AntScout.system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName,
          destinationId))
        pathFuture = (AntScout.system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) ?
          RoutingService.FindPath(source, destination))
        path <- Await.result(pathFuture, 5 seconds).asInstanceOf[Box[Seq[AntWay]]] ?~ "No path found" ~> 404
      } yield {
        selectedSource send Some(sourceId)
        selectedDestination send  Some(destinationId)
        val (length, tripTime) = path.foldLeft(0.0, 0.0) {
          case ((lengthAcc, tripTimeAcc), way) => (way.length + lengthAcc, way.tripTime + tripTimeAcc)
        }
        ("length" -> length) ~
        ("tripTime" -> tripTime) ~
        ("ways" -> path.map(_.toJson))
      }
    case Get(List("osmnodes"), _) => {
      OsmMap.nodes.values.map(_.toJson): JArray
    }
    case Get(List("ways"), _) => {
      AntMap.ways.map(_.toJson): JArray
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
