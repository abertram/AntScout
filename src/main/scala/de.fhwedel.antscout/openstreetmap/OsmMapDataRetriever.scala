package de.fhwedel.antscout
package openstreetmap

import net.liftweb.common.Logger
import actors.Actor
import java.net.URL
import xml.XML

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 08:09
 */

object OsmMapDataRetriever extends Actor {
    val logger = Logger(getClass)

    def act() {
        logger.debug("act")
        react {
            case OsmMapDataRequest(requestor) =>
                logger.debug("OsmMapDataRequest")
                val osmMapData = retrieveMapData
                requestor ! OsmMapDataResponse(osmMapData)
        }
    }

    def retrieveMapData = {
        val url = new URL("http://www.overpass-api.de/api/xapi?*[bbox=9.94088,53.75444,9.9512,53.75726]")
        XML.load(url.openStream())
    }
}
