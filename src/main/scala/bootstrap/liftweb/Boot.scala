package bootstrap.liftweb

import net.liftweb._

import common._
import http._
import de.fhwedel.antscout.AntScout
import de.fhwedel.antscout.rest.{DebugRest, Rest}

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Logger {

  /**
   * Bootet die Anwendung.
   */
  def boot {
    // where to search snippet
    LiftRules.addToPackages("de.fhwedel.antscout")

    // AntScout-Initialisierung anstoßen
    AntScout.init()

    // Use jQuery
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQueryArtifacts

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    // Rest-Schnittstellen registrieren
    LiftRules.dispatch.append(Rest)
    LiftRules.dispatch.append(DebugRest)

    // Event-Handler registrieren, die beim Beenden der Anwendung ausgeführt werden.
    LiftRules.unloadHooks.append(() => {
      // System runterfahren
      info("Shutting down")
      AntScout.shutDown()
    })
  }
}
