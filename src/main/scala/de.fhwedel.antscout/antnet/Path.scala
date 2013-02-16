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
package antnet

import net.liftweb.common.Box
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST.JArray
import akka.actor.ActorRef

/**
 * Repräsentiert einen Pfad.
 *
 * @param source Quelle
 * @param destination Ziel
 * @param ways Wege
 */
case class Path(source: ActorRef, destination: ActorRef, ways: Seq[AntWay]) {

  /**
   * Länge und Reise-Zeit
   */
  lazy val (length, tripTime) = ways.foldLeft(0.0, 0.0) {
    case ((lengthAcc, tripTimeAcc), way) => (way.length + lengthAcc, way.tripTime + tripTimeAcc)
  }

  /**
   * Erzeugt eine Json-Repräsentation.
   *
   * @return Json-Repräsentation
   */
  def toJson = {
    ("destination" -> AntNode.toOsmNode(destination).toJson) ~
    ("length" -> "%.4f".format(length / 1000)) ~
    ("lengths" -> JArray(List(
      ("unit" -> "m") ~
        ("value" -> "%.4f".format(length))))) ~
    ("source" -> AntNode.toOsmNode(source).toJson) ~
    ("tripTime" -> "%.4f".format(tripTime / 60)) ~
    ("tripTimes" -> JArray(List(
      ("unit" -> "s") ~
        ("value" -> "%.4f".format(tripTime)),
      ("unit" -> "h") ~
        ("value" -> "%.4f".format(tripTime / 3600))))) ~
    ("ways" ->  ways.map(_.toJson))
  }
}

/**
 * Path-Factory.
 */
object Path {

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.antnet.Path]]-Instanz.
   *
   * @param ways Wege
   * @param source Quelle
   * @param destination Ziel
   * @return [[de.fhwedel.antscout.antnet.Path]]-Instanz
   */
  def apply(source: ActorRef, destination: ActorRef, ways: Box[Seq[AntWay]]) = {
    ways map { new Path(source, destination, _) }
  }
}
