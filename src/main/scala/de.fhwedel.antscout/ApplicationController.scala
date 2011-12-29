package de.fhwedel.antscout

import antnet.{AntController, AntMap}
import net.liftweb.common.Logger
import osm.{OsmMap, OsmMapDataResponse, OsmMapDataRequest, OsmMapDataRetriever}
import xml.XML
import akka.actor.Actor

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:20
 */

object ApplicationController extends Logger {

//  def act() {
//    debug("Starting OsmMapDataRetriever")
//    OsmMapDataRetriever.start()
//    OsmMapDataRetriever ! OsmMapDataRequest(this)
//    val osmData = XML loadFile("./maps/Fasanenweg-Zoom-15.osm")
    val osmData = XML loadFile("./maps/Hamburg.osm")
    val osmMap = OsmMap(osmData)
    val antMap = AntMap(osmMap)
    Actor.actorOf(AntController(antMap)).start()
//    react {
//      case OsmMapDataResponse(osmMapData) => {
//        logger.debug("OsmMapDataResponse")
//        val osmMap = OsmMap(osmMapData)
//        AntMap(osmMap)
//      }
//    }
//  }
}
