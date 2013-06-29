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

import com.earldouglas.xsbtwebplugin.{WebPlugin, PluginKeys}
import sbt._
import Keys._

/**
 * AntScout-Build-Definition.
 *
 * Sorgt dafür, dass eine OpenStreetMap-Karte heruntergeladen wird und aus dieser definierte Karten extrahiert werden.
 * Es wird zusätzlich dafür gesorgt, dass diese Anforderungen vor dem Start der Anwendung ausgeführt werden, sodass die
 * extrahierten Karten rechtzeitig zur Verfügung stehen.
 */
object AntScoutBuild extends Build {

  /**
   * Karten-Verzeichnis.
   */
  val mapDirectory = "maps"

  /**
   * Karten-Name.
   */
  val mapName = "hamburg.osm"

  /**
   * Karten-Erweiterung.
   */
  val mapExtension = ".pbf"

  /**
   * Original-Karte.
   */
  val originalMap = file("%s/%s%s" format (mapDirectory, mapName, mapExtension))

  /**
   * Karten, die erzeugt werden sollen.
   *
   * Die Sequenz besteht aus einzelnen Tupeln. Die Tupeln selbst bestehen aus drei Elementen:
   *
   * 1. Original-Karte, aus der die einzelnen Karten erzeugt werden.
   * 2. Koordinaten für den Ausschnitt aus der Original-Karte.
   * 3. Speicherort und Dateiname der erzeugten Karte.
   */
  val preprocessedMaps = Seq(
    (originalMap, (9.8865, 53.5683, 9.93, 53.545), file("%s/%s" format (mapDirectory,
      "Bahrenfeld-Gross-Flottbek-Othmarschen-Ottensen.osm"))),
    (originalMap, (9.693, 53.595, 9.8275, 53.56), file("%s/%s" format (mapDirectory, "Blankenese-Wedel.osm"))),
    (originalMap, (9.926, 53.5518, 9.9559, 53.54), file("%s/%s" format (mapDirectory, "Altona-50-Knoten.osm"))),
    (originalMap, (9.9362, 53.55131, 9.949, 53.5468), file("%s/%s" format (mapDirectory, "Altona-Kreis.osm"))),
    (originalMap, (9.69, 53.5856, 9.892, 53.5464), file("%s/%s" format (mapDirectory, "Othmarschen-Wedel.osm"))),
    (originalMap, (9.693, 53.595, 9.78, 53.56), file("%s/%s" format (mapDirectory, "Wedel.osm")))
  )

  /**
   * TaskKey zum Download der Original-Karte.
   */
  val downloadMap = TaskKey[Unit]("download-map")

  /**
   * TaskKey zum Vorverarbeiten der Karte.
   */
  val preprocessMap = TaskKey[Unit]("preprocess-map")

  /**
   * Task zum Donwload der Original-Karte.
   */
  val downloadMapTask = downloadMap <<= streams.map { s: TaskStreams =>
    // Nur ausführen, wenn die Original-Karte nicht existiert
    if (!originalMap.exists) {
      // Karten-URL erzeugen
      val mapUrl = url("http://download.geofabrik.de/openstreetmap/europe/germany/%s%s" format (mapName, mapExtension))
      s.log.info("Downloading map from %s to %s" format (mapUrl, originalMap))
      // Karte runterladen
      sbt.IO.download(mapUrl, originalMap)
      // Status-Meldung ausgeben
      if (originalMap.exists)
        s.log.success("Map downloaded")
      else
        s.log.error("Downloading map failed")
    }
  }

  /**
   * Task zum Vorverarbeiten der Karten.
   */
  val preprocessMapTask = preprocessMap <<= streams.map { s: TaskStreams =>
    // Nur ausführen, wenn eine der Karten, die vorverarbeitet werden sollen, nicht existiert
    if (preprocessedMaps exists { case (_, _, preprocessedMap) => !preprocessedMap.exists }) {
      s.log.info("Preprocessing maps")
      // Pfad zu Osmosis, mit dem die Karten vorverarbeitet werden
      val command = "maps/osmosis/bin/osmosis" + (if (sys.props("os.name").startsWith("Win")) ".bat" else "")
      // Osmosis als ausführbar setzen
      file(command).setExecutable(true, true)
      // Über die zu vorberarbeitende Karten iterieren
      preprocessedMaps foreach {
        case (originalMap, boundingBox, preprocessedMap) => {
          // Nur ausführen, wenn die Karte nicht existiert
          if (!preprocessedMap.exists) {
            s.log.info("Preprocessing map %s" format preprocessedMap)
            // Grenzen extrahieren
            val (left, top, right, bottom) = boundingBox
            // Argumente für Osmosis erzeugen
            val arguments = Seq[String](
              "-q",
              "--read-pbf", "file=" + originalMap,
              "--bounding-box", "left=" + left, "top=" + top, "right=" + right, "bottom=" + bottom,
              "--tag-filter", "accept-ways", "highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link," +
                "secondary,secondary_link,tertiary,tertiary_link,residential,living_street,unclassified",
              "--tag-filter", "reject-relations",
              "--used-node",
              "--write-xml", preprocessedMap.toString
            )
            // Kommando ausführen und Status-Meldung ausgeben
            (Process(command, arguments) !) match {
              case 0 => s.log.success("Map %s preprocessed" format preprocessedMap)
              case _ => s.log.error("Preprocessing map %s failed" format preprocessedMap)
            }
          }
        }
      }
    }
  }

  /**
   * AntScout-Projekt.
   */
  lazy val antScout = Project(
    // Name
    "AntScout",
    // Verzeichnis
    file("."),
    // Konfiguration
    settings =
      // Standard-Konfiguration
      Project.defaultSettings ++
      // WebPlugin-Konfiguration
      WebPlugin.webSettings ++
      // Eigene Tasks und deren Abhängigkeiten
      Seq(
        downloadMapTask,
        preprocessMapTask,
        // container:start hängt von preprocessMap ab
        PluginKeys.start in WebPlugin.container.Configuration <<= (PluginKeys.start in WebPlugin.container
          .Configuration).dependsOn(preprocessMap),
        // preprocessMap hängt von downloadMap ab
        preprocessMap <<= preprocessMap.dependsOn(downloadMap)
      )
  )
}
