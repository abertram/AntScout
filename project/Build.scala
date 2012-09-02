import com.github.siasia.{WebPlugin, PluginKeys}
import sbt._
import Keys._

object AntScoutBuild extends Build {

  val mapDirectory = "maps"
  val mapName = "hamburg.osm"
  val mapExtension = ".pbf"
  val originalMap = file("%s/%s%s" format (mapDirectory, mapName, mapExtension))
  val preprocessedMaps = Seq(
    (originalMap, (9.9362, 53.55131, 9.94756, 53.5468), file("%s/%s" format (mapDirectory, "Altona-Kreis.osm"))),
    (originalMap, (9.69, 53.585, 9.95, 53.545), file("%s/%s" format (mapDirectory, "Altona-Wedel.osm")))
  )

  val downloadMap = TaskKey[Unit]("download-map")
  val preprocessMap = TaskKey[Unit]("preprocess-map")

  val downloadMapTask = downloadMap <<= streams.map { s: TaskStreams =>
    if (!originalMap.exists) {
      val mapUrl = url("http://download.geofabrik.de/osm/europe/germany/%s%s" format (mapName, mapExtension))
      s.log.info("Downloading map from %s to %s" format (mapUrl, originalMap))
      sbt.IO.download(mapUrl, originalMap)
      s.log.success("Map downloaded")
    }
  }

  val preprocessMapTask = preprocessMap <<= streams.map { s: TaskStreams =>
    preprocessedMaps foreach {
      case (originalMap, boundingBox, preprocessedMap) =>
        if (!preprocessedMap.exists) {
          s.log.info("Preprocessing map %s" format preprocessedMap)
          val command = "maps/osmosis-0.40.1/bin/osmosis" + (if (sys.props("os.name").startsWith("Win")) ".bat" else "")
          file(command).setExecutable(true)
          val (left, top, right, bottom) = boundingBox
          val arguments = Seq[String](
            "-q",
            "--read-pbf", "file=" + originalMap,
            "--bounding-box", "left=" + left, "top=" + top, "right=" + right, "bottom=" + bottom, "completeWays=yes",
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

  lazy val antScout = Project("AntScout", file("."), settings = Project.defaultSettings ++ WebPlugin.webSettings ++
    Seq(downloadMapTask, preprocessMapTask, PluginKeys.start in WebPlugin.container.Configuration <<= (PluginKeys.start
      in WebPlugin.container.Configuration).dependsOn(preprocessMap), preprocessMap <<= preprocessMap.dependsOn
      (downloadMap)))
}
