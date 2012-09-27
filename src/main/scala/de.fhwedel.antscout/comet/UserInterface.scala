package de.fhwedel.antscout
package comet

import antnet.AntNode
import net.liftweb.common.Logger
import net.liftweb.http.js.JE.Call
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.NamedCometActorTrait

class UserInterface extends Logger with NamedCometActorTrait {

  override def lowPriority = {
    case AntNode.PheromonesAndProbabilities(source, destination, pheromones, probabilities) =>
      partialUpdate(SetHtml("pheromones",
        <ol> {
          pheromones.sortBy {
            case (_, pheromone) =>
              pheromone
          }.reverse.map {
            case (way, pheromone) =>
              <li> { "%s: %.4f" format (way.id, pheromone) } </li>
          }
        } </ol>))
      partialUpdate(SetHtml("probabilities",
        <ol> {
          probabilities.sortBy {
            case (_, probability) =>
              probability
          }.reverse.map {
            case (way, probability) =>
              <li> { "%s: %.4f" format (way.id, probability) } </li>
          }
        } </ol>))
    case m: Any =>
      warn("Unknown message: %s" format m)
  }

  def render = Call("console.log", "UserInterface").cmd
}
