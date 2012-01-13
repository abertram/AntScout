package de.fhwedel.antscout
package rest

import net.liftweb.http.rest.RestHelper
import antnet.AntMap
import net.liftweb.json.Extraction
import net.liftweb.json.JsonAST.{JArray, JObject, JString, JField}
import osm.OsmMap
import net.liftweb.json.JsonDSL._

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 12.01.12
 * Time: 18:32
 */

object Rest extends RestHelper {

  serve {
    case Get(List("antnodes"), _) => {
      JArray(AntMap.nodes.keys.map(k => {
        val osmNode = OsmMap.nodes(k)
        ("id" -> k) ~
        ("latitude" -> osmNode.geographicCoordinate.latitude) ~
        ("longitude" -> osmNode.geographicCoordinate.longitude)
      }).toList)
    }
    case Get(List("osmnodes"), _) => {
      JArray(OsmMap.nodes.values.map(n => {
        ("id" -> n.id) ~
        ("latitude" -> n.geographicCoordinate.latitude) ~
        ("longitude" -> n.geographicCoordinate.longitude)
      }).toList)
    }
  }
}