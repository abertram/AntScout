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

/**
 * Rest-Schnittstelle für Knoten, Wege und Pfade.
 */
object Rest extends Logger with RestHelper {

  implicit val timeout = Timeout(10 seconds)

  serve {
    // Anfrage nach den Daten eines bestimmten Knotens
    case Get(List("node", id), _) =>
      Node.send(Full(id))
      // Eingehende Wege
      val incomingWays = AntMap.incomingWays.find {
        case (node, ways) => node.id == id
      } map {
        case (node, ways) => ways
      } getOrElse Set[AntWay]()
      // Ausgehende Wege
      val outgoingWays = AntMap.outgoingWays.find {
        case (node, ways) => node.id == id
      } map {
        case (node, ways) => ways
      } getOrElse Set[AntWay]()
      ("incomingWays" -> incomingWays.map(_.toJson)) ~
      ("outgoingWays" -> outgoingWays.map(_.toJson))
    // Anfrage nach Knoten
    case Get(List("nodes"), _) =>
      AntMap.nodes.map(node => OsmMap.nodes(node.id)).map(_.toJson): JArray
    // Anfrage nach einem Pfad
    case Get(List("path", source, destination), _) =>
      // Quelle in einem globalen Objekt speichern
      Source.send(Full(source))
      // Ziel in einem globalen Objekt speichern
      Destination.send(Full(destination))
      // Anfrage an den RoutingService nach dem Pfad
      val pathFuture = (system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) ?
        RoutingService.FindPath(AntNode(source), AntNode(destination)))
      for {
        // Auf die Antwort vom RoutingService warten
        path <- Await.result(pathFuture, 5 seconds).asInstanceOf[Box[antnet.Path]] ?~ "No path found" ~> 404
      } yield {
        path toJson
      }
    // Anfrage nach den OSM-Knoten
    case Get(List("osmnodes"), _) => {
      OsmMap.nodes.values.map(_.toJson): JArray
    }
    // Anfrage nach den Wegen
    case Get(List("ways"), _) => {
      AntMap.ways.map(_.toJson): JArray
    }
    // Anfrage nach den Daten eines bestimmten Weges
    case Get(List("way", id), _) =>
      AntMap.ways.find(_.id == id).map(_.toJson)
    // Weg-Änderungs-Anfrage
    case JsonPut(List("way", id), json -> _) =>
      // Weg suchen, der aktualisiert werden soll
      val way = AntMap.ways.find(_.id == id)
      // Aktualisierte Daten extrahieren
      val wayUpdate = json.extract[AntWay.Update]
      way.map { way =>
        // Aktualisierung durchführen
        way.update(wayUpdate)
        // Warten, bis die Aktualisierung durchgeführt wurde
        way.maxSpeed(true)
        // Pfad aktualisieren, falls notwendig
        for {
          path <- Path.get
        } yield {
          // Ist der aktualisierte Weg Teil des aktuellen Pfades?
          if (path.ways.contains(way)) {
            // Weg im Pfad ersetzen
            val newPath = antnet.Path(path.source, path.destination, (path.ways.takeWhile(_ != way) :+ way) ++
              path.ways.dropWhile(_ != way).tail)
            // Neuen Pfad an den User-Interface-Aktor senden
            NamedCometListener.getDispatchersFor(Full("userInterface")) foreach { actor =>
              actor.map(_ ! RoutingService.Path(Full(newPath)))
            }
            // Neuen Pfad in einem globalen Objekt speichern
            Path.send(Full(newPath))
          }
        }
        // Aktualisierten Weg zurückgeben
        way.toJson
      }
  }
}
