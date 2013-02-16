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
package comet

import antnet.AntNode
import net.liftweb.common.{Box, Logger}
import net.liftweb.http.js.JE.Call
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.NamedCometActorTrait
import routing.RoutingService
import xml.Text
import net.liftweb.json.JsonAST.{JField, JNothing, JObject}
import net.liftweb.http.js.JsExp

/**
 * Zuständig für die Manipulation des User-Interfaces.
 */
class UserInterface extends Logger with NamedCometActorTrait {

  override def lowPriority = {
    // Angekommene Ameisen
    case AntNode.ArrivedAnts(arrivedAnts) =>
      partialUpdate(SetHtml("path-additional-data-arrived-ants", Text(arrivedAnts.toString)))
    // Erzeugte Ameisen
    case AntNode.LaunchedAnts(launchedAnts) =>
      partialUpdate(SetHtml("path-additional-data-launched-ants", Text(launchedAnts.toString)))
    // Passierte Ameisen
    case AntNode.PassedAnts(passedAnts) =>
      partialUpdate(SetHtml("passed-ants", Text(passedAnts.toString)))
    // Pheromone und Wahrscheinlichkeiten
    case AntNode.PheromonesAndProbabilities(pheromones, probabilities) =>
      val (bestWay, _) = probabilities.maxBy { case (_, probability) => probability }
      partialUpdate(SetHtml("pheromones-and-probabilities",
        <thead>
          <th>#</th>
          <th>Pheromone</th>
          <th>Probability</th>
        </thead> ++ {
          pheromones.sortBy {
            case (way, _) => way.id
          }.map {
            case (way, pheromone) =>
              <tr class={ if (way == bestWay) "success" else "" }>
                <td> { way.id } </td>
                <td> { "%.4f" format pheromone } </td>
                <td> { "%.4f" format probabilities(way) } </td>
              </tr>
          }
        }))
    // Pfad
    case RoutingService.Path(path) => {
      val path1: Box[JsExp] = path.map(_.toJson)
      val path2: JsExp = JObject(List(JField("", JNothing)))
      // Pfad an das Front-End senden
      partialUpdate(Call("AntScout.path", path1.getOrElse(path2)).cmd)
    }
    case m: Any =>
      warn("Unknown message: %s" format m)
  }

  def render = Call("console.log", name.toString).cmd
}
