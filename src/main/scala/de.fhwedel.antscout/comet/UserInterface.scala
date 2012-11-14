package de.fhwedel.antscout
package comet

import antnet.{AntWay, AntNode}
import net.liftweb.common.{Full, Logger}
import net.liftweb.http.js.JE.Call
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.NamedCometActorTrait
import net.liftweb.json.JsonDSL._
import routing.RoutingService
import xml.Text
import net.liftweb.json.JsonAST.JArray

/**
 * Zuständig für die Manipulation des User-Interfaces.
 */
class UserInterface extends Logger with NamedCometActorTrait {

  override def lowPriority = {
    // Angekommene Ameisen
    case AntNode.ArrivedAnts(arrivedAnts) =>
      partialUpdate(SetHtml("arrived-ants", Text(arrivedAnts.toString)))
    // Erzeugte Ameisen
    case AntNode.LaunchedAnts(launchedAnts) =>
      partialUpdate(SetHtml("launched-ants", Text(launchedAnts.toString)))
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
      // Pfad als Json
      val pathAsJson = path match {
        case Full(path) =>
          val (length, tripTime) = path.foldLeft(0.0, 0.0) {
            case ((lengthAcc, tripTimeAcc), way) => (way.length + lengthAcc, way.tripTime + tripTimeAcc)
          }
          ("length" -> "%.4f".format(length / 1000)) ~
          ("lengths" ->
            JArray(List(("unit" -> "m") ~
            ("value" -> "%.4f".format(length))))) ~
          ("tripTime" -> "%.4f".format(tripTime / 60)) ~
          ("tripTimes" ->
            JArray(List(
              ("unit" -> "s") ~
              ("value" -> "%.4f".format(tripTime)),
              ("unit" -> "h") ~
              ("value" -> "%.4f".format(tripTime / 3600))))) ~
          ("ways" ->  path.map(_.toJson))
        case _ => JArray(List[AntWay]().map(_.toJson))
      }
      // Pfad an das Front-End senden
      partialUpdate(Call("AntScout.path", pathAsJson).cmd)
    }
    case m: Any =>
      warn("Unknown message: %s" format m)
  }

  def render = Call("console.log", name.toString).cmd
}
