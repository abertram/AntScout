import com.github.siasia.{WebPlugin, PluginKeys}
import sbt._
import Keys._

object AntScoutBuild extends Build {

  val mapDirectory = "maps"
  val mapName = "hamburg.osm"
  val mapExtension = ".pbf"
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
    (originalMap, (9.69, 53.5856, 9.892, 53.5464), file("%s/%s" format (mapDirectory, "Altona-Wedel.osm"))),
    (originalMap, (9.693, 53.595, 9.78, 53.56), file("%s/%s" format (mapDirectory, "Wedel.osm")))
  )

  val downloadMap = TaskKey[Unit]("download-map")
  val preprocessMap = TaskKey[Unit]("preprocess-map")

  val downloadMapTask = downloadMap <<= streams.map { s: TaskStreams =>
    if (!originalMap.exists) {
      val mapUrl = url("http://download.geofabrik.de/openstreetmap/europe/germany/%s%s" format (mapName, mapExtension))
      s.log.info("Downloading map from %s to %s" format (mapUrl, originalMap))
      sbt.IO.download(mapUrl, originalMap)
      if (originalMap.exists)
        s.log.success("Map downloaded")
      else
        s.log.error("Downloading map failed")
    }
  }

  val preprocessMapTask = preprocessMap <<= streams.map { s: TaskStreams =>
    if (preprocessedMaps exists { case (_, _, preprocessedMap) => !preprocessedMap.exists }) {
      s.log.info("Preprocessing maps")
      val command = "maps/osmosis-0.40.1/bin/osmosis" + (if (sys.props("os.name").startsWith("Win")) ".bat" else "")
      file(command).setExecutable(true, true)
      preprocessedMaps foreach {
        case (originalMap, boundingBox, preprocessedMap) => {
          if (!preprocessedMap.exists) {
            s.log.info("Preprocessing map %s" format preprocessedMap)
            val (left, top, right, bottom) = boundingBox
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
            (Process(command, arguments) !) match {
              case 0 => s.log.success("Map %s preprocessed" format preprocessedMap)
              case _ => s.log.error("Preprocessing map %s failed" format preprocessedMap)
            }
          }
        }
      }
    }
  }

  lazy val antScout = Project("AntScout", file("."), settings = Project.defaultSettings ++ WebPlugin.webSettings ++
    Seq(downloadMapTask, preprocessMapTask, PluginKeys.start in WebPlugin.container.Configuration <<= (PluginKeys.start
      in WebPlugin.container.Configuration).dependsOn(preprocessMap), preprocessMap <<= preprocessMap.dependsOn
      (downloadMap)))
}
