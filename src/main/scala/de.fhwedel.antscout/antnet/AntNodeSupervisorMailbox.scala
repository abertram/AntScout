package de.fhwedel.antscout
package antnet

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

/**
 * Priorisierte Mailbox für [[de.fhwedel.antscout.antnet.AntNodeSupervisor]].
 *
 * @param settings
 * @param config
 */
class AntNodeSupervisorMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox(
  PriorityGenerator {
    // Verarbeitung der Statistiken wird mit höchster Priorität behandelt.
    case AntNodeSupervisor.ProcessStatistics => 0
    case _ => 1
  }
)
