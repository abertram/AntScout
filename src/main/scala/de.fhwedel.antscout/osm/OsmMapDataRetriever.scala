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
    val url = new URL("http://www.overpass-api.de/api/xapi?way[bbox=9.6559,53.5274,9.9896,53.678][highway=motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|tertiary|residential|service|track]")
    val (time, mapData) = TimeHelpers.calcTime(XML.load(url.openStream()))
    info("OSM map data retrieved in %d milliseconds".format(time))
    mapData
  }
}

