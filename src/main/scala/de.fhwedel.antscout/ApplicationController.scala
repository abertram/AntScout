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

// TODO Application-Controller evtl als State-Machine implementieren
object ApplicationController extends Logger {

  Actor.spawn {
    val osmData = XML loadFile("./maps/Hamburg.osm")
//    val osmData = XML loadFile("./maps/Altona-Zoomstufe-17-preprocessed.osm")
    OsmMap(osmData)
    AntMap()
    RoutingService(AntMap sources, AntMap.ways.values)
    AntLauncher
  }
}
