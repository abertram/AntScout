package de.fhwedel.antscout
package rest

import akka.pattern.ask
import akka.util.duration._
import net.liftweb.http.rest.RestHelper
import net.liftweb.json.JsonDSL._
import osm.OsmMap
import routing.RoutingService
import net.liftweb.common.{Full, Box, Logger}
import antnet.{AntNode, AntWay, AntMap}
import akka.dispatch.Await
import akka.util.Timeout
import net.liftweb.json.JsonAST.JArray
import net.liftweb.http.NamedCometListener
import comet.OpenLayers

object Rest extends Logger with RestHelper {

  implicit val timeout = Timeout(10 seconds)

  serve {
    case Get(List("node", id), _) =>
      Node(Full(id))
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
      AntMap.nodes.map(node => OsmMap.nodes(node.id).toJson): JArray
    case Get(List("path", source, destination), _) =>
      Source(Full(source))
      Destination(Full(destination))
      val pathFuture = (system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) ?
        RoutingService.FindPath(AntNode(source), AntNode(destination)))
      for {
        path <- Await.result(pathFuture, 5 seconds).asInstanceOf[Box[Seq[AntWay]]] ?~ "No path found" ~> 404
      } yield {
        val (length, tripTime) = path.foldLeft(0.0, 0.0) {
          case ((lengthAcc, tripTimeAcc), way) => (way.length + lengthAcc, way.tripTime + tripTimeAcc)
        }
        ("length" -> "%.4f".format(length / 1000)) ~
        ("lengths" ->
          JArray(List(("unit" -> "m") ~
          ("value" -> "%.4f".format(length))))) ~
        ("tripTime" -> "%.4f".format(tripTime / 60)) ~
        ("tripTimes" ->
          JArray(List(
            ("unit" -> "s") ~
            ("value" -> "%.4f".format(tripTime)),
            ("unit" -> "h") ~
            ("value" -> "%.4f".format(tripTime / 3600))))) ~
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
        // aktuellen Pfad aktualisieren, falls notwendig
        for {
          path <- Path
        } yield {
          if (path.contains(way)) {
            val newPath = (path.takeWhile(_ != way) :+ way) ++ path.dropWhile(_ != way).tail
            NamedCometListener.getDispatchersFor(Full("openLayers")) foreach { actor =>
              actor.map(_ ! OpenLayers.DrawPath(Full(newPath)))
            }
            Path(Full(newPath))
          }
        }
        // aktualisierten Weg zurückgeben
        way.toJson
      }
  }
}
