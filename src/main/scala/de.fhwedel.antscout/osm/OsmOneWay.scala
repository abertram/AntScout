/*
 * Copyright 2012 Alexander Bertram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fhwedel.antscout
package osm

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.12.11
 * Time: 15:49
 */

class OsmOneWay(highway: String, id: String, name: String, nodes: List[OsmNode], maxSpeed: Double) extends OsmWay(highway, id, name, nodes, maxSpeed) {

  override def toString = "#%s #%s -> #%s".format(id, nodes.head.id, nodes.last.id)
}

object OsmOneWay {
  
  def apply(id: Int, nodes: List[OsmNode]) = new OsmOneWay("", id.toString, "", nodes, 0)

  def apply(highway: String, id: Int, name: String, nodes: List[OsmNode], maxSpeed: Double) =
    new OsmOneWay(highway: String, id.toString, name, nodes, maxSpeed)

  def apply(id: String, name: String, nodes: List[OsmNode]) = new OsmOneWay("", id, name, nodes, 0)
  
  def apply(id: String, nodes: List[OsmNode]) = new OsmOneWay("", id, "", nodes, 0)

  def apply(highway: String, id: String, name: String, nodes: List[OsmNode], maxSpeed: Double) =
    new OsmOneWay(highway, id, name, nodes, maxSpeed)
}
                                                                               