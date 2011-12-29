package de.fhwedel.antscout
package osm

import net.liftweb.common.Logger
import actors.Actor
import java.net.URL
import xml.XML
import net.liftweb.util.TimeHelpers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 08:09
 */

object OsmMapDataRetriever extends Actor with Logger {

  def act() {
    debug("act")
    react {
      case OsmMapDataRequest(requestor) =>
        debug("OsmMapDataRequest")
        val osmMapData = retrieveMapData
        requestor ! OsmMapDataResponse(osmMapData)
    }
  }

  def retrieveMapData = {
    info("Retrieving OSM map data")
    // Ellerau, Zoomstufe 18
    // val boundingBox = "9.9141,53.748862,9.919233,53.751206"
    // Ellerau, Zoomstufe 17
    // val boundingBox = "9.91151,53.74784,9.92183,53.75223"
    // Ellerau, Zoomstufe 16
    // val boundingBox = "9.90635,53.74564,9.92699,53.75442"
    // Ellerau, Zoomstufe 15
    // val boundingBox = "9.89602,53.74125,9.93731,53.75881"
    // Ellerau, Zoomstufe 14
    // val boundingBox = "9.8754,53.7325,9.958,53.7676"
    // Ellerau, Zoomstufe 13
    val boundingBox = "9.8341,53.7149,9.9992,53.7851"
    // Ellerau
    // val url = new URL("http://www.overpass-api.de/api/xapi?way[bbox=9.875,53.7313,9.9584,53.7688][highway=motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|tertiary|residential|service|track]")
    // Tanneneck
    // val url = new URL("http://www.overpass-api.de/api/xapi?way[bbox=9.92966,53.75092,9.95019,53.76029][highway=motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|tertiary|residential|service|track]")
    val url = new URL("http://www.overpass-api.de/api/xapi?way[bbox=%s][highway=motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|tertiary|residential|service|track]".format(boundingBox))
    val (time, mapData) = TimeHelpers.calcTime(XML.load(url.openStream()))
    info("OSM map data retrieved in %d milliseconds".format(time))
    mapData
  }
}

