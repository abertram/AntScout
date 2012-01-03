package de.fhwedel.antscout

import antnet.{AntLauncher, AntMap}
import net.liftweb.common.Logger
import osm.{OsmMap}
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
    val osmMap = OsmMap(osmData)
    val antMap = AntMap(osmMap)
    Actor.actorOf(AntLauncher(antMap)).start()
  }
}
