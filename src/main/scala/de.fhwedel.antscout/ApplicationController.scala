package de.fhwedel.antscout

import antnet.{AntLauncher, AntMap}
import net.liftweb.common.Logger
import osm.{OsmMap}
import routing.RoutingService
import xml.XML
import akka.actor.Actor

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:20
 */

object ApplicationController extends Logger {

  Actor.spawn {
    val osmData = XML loadFile("./maps/Hamburg.osm")
//    val osmData = XML loadFile("./maps/Ellerau-Zoomstufe-16-preprocessed.osm")
    OsmMap(osmData)
    AntMap()
    RoutingService(AntMap.nodes.values, AntMap.ways.values)
    Actor.actorOf[AntLauncher].start()
  }
}
