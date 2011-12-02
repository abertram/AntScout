package de.fhwedel.antscout

import net.liftweb.common.Logger
import actors.Actor
import osm.{OsmMap, OsmMapDataResponse, OsmMapDataRequest, OsmMapDataRetriever}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:20
 */

object ApplicationController extends Actor {
  val logger = Logger(getClass)

  def act() {
    logger.debug("Starting OsmMapDataRetriever")
    OsmMapDataRetriever.start()
    OsmMapDataRetriever ! OsmMapDataRequest(this)
    react {
      case OsmMapDataResponse(osmMapData) => {
        logger.debug("OsmMapDataResponse")
        new OsmMap(osmMapData)
      }
    }
  }
}
