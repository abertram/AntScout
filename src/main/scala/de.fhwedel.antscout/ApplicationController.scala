package de.fhwedel.antscout

import antnet.{AntLauncher, AntMap}
import net.liftweb.common.Logger
import osm.{OsmMap}
import routing.RoutingService
import xml.XML
import akka.actor.{FSM, Actor}
import net.liftweb.util.Props

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:20
 */

sealed trait ApplicationControllerMessage
case object Uninitialized extends ApplicationControllerMessage
case object Initialize extends ApplicationControllerMessage
// case object InitializeRoutingService extends ApplicationControllerMessage
case object RoutingServiceInitializing extends ApplicationControllerMessage
case object RoutingServiceInitialized extends ApplicationControllerMessage

class ApplicationController extends Actor with FSM[ApplicationControllerMessage, Unit] with Logger {

  startWith(Uninitialized, Unit)

  when(Uninitialized) {
    case Ev(Initialize) =>
      info("Initializing OsmMap")
      val map = Props.get("map")
      assert(map isDefined)
      OsmMap(map get)
      info("Initializing AntMap")
      AntMap()
      info("Initializing RoutingService")
      RoutingService
      goto(RoutingServiceInitializing)
  }

  when(RoutingServiceInitializing) {
    case Ev(RoutingServiceInitialized) =>
      info("RoutingService initialized")
      info("Initializing AntLauncher")
      AntLauncher
      stop
  }

  initialize
}

object ApplicationController {

  val instance = Actor.actorOf[ApplicationController].start
  instance ! Initialize
}