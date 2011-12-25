package de.fhwedel.antscout

import antnet.AntMap
import net.liftweb.common.Logger
import actors.Actor
import osm.{OsmMap, OsmMapDataResponse, OsmMapDataRequest, OsmMapDataRetriever}
import xml.XML

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:20
 */

object ApplicationController extends Actor with Logger {

  def act() {
//    debug("Starting OsmMapDataRetriever")
//    OsmMapDataRetriever.start()
//    OsmMapDataRetriever ! OsmMapDataRequest(this)
    val osmData = XML.loadFile("./maps/Fasanenweg-Zoom-15.osm")
    val osmMap = OsmMap(osmData)
    AntMap(osmMap)
//    react {
//      case OsmMapDataResponse(osmMapData) => {
//        logger.debug("OsmMapDataResponse")
//        val osmMap = OsmMap(osmMapData)
//        AntMap(osmMap)
//      }
//    }
  }
}
