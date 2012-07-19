package bootstrap.liftweb

import net.liftweb._

import common._
import http._
import sitemap._
import Loc._
import de.fhwedel.antscout.AntScout
import de.fhwedel.antscout.rest.Rest

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Logger {

  def boot {
    // where to search snippet
    LiftRules.addToPackages("de.fhwedel.antscout")

    // AntScout-Initialisierung anstoßen
    AntScout.init()

    // Build SiteMap
    val entries = List(
      Menu.i("Home") / "index", // the simple way to declare a menu

      // more complex because this menu allows anything in the
      // /static path to be visible
      Menu(Loc("Static", Link(List("static"), true, "/static/index"),
        "Static Content"))
    )

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries: _*))

    // Use jQuery
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQueryArtifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    LiftRules.statelessDispatch.append(Rest)

    LiftRules.unloadHooks.append(() => {
      // System runterfahren
      info("Shutting down")
      AntScout.shutDown()
    })
  }
}
