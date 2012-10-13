package de.fhwedel.antscout
package comet

import antnet.AntNode
import net.liftweb.common.Logger
import net.liftweb.http.js.JE.Call
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.NamedCometActorTrait
import xml.Text

class UserInterface extends Logger with NamedCometActorTrait {

  override def lowPriority = {
    case AntNode.ArrivedAnts(arrivedAnts) =>
      partialUpdate(SetHtml("arrived-ants", Text(arrivedAnts.toString)))
    case AntNode.LaunchedAnts(launchedAnts) =>
      partialUpdate(SetHtml("launched-ants", Text(launchedAnts.toString)))
    case AntNode.PassedAnts(passedAnts) =>
      partialUpdate(SetHtml("passed-ants", Text(passedAnts.toString)))
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
    case m: Any =>
      warn("Unknown message: %s" format m)
  }

  def render = Call("console.log", "UserInterface").cmd
}
