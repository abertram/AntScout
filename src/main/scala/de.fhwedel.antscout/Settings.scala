package de.fhwedel.antscout

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._
import akka.util.Duration
import java.util.concurrent.TimeUnit

/**
 * Konfiguration.
 */
object Settings {

  /**
   * Konfiguration.
   */
  private val config = ConfigFactory.load

  import config._

  /**
   * Parameter a für die Squash-Funktion.
   */
  val A = getInt("ant-scout.ant-net.a")

  /**
   * Relatives Gewicht der heuristischen Information, die in die Berechnung der Wahrscheinlichkeiten einfließt.
   */
  val Alpha = getDouble("ant-scout.ant-net.alpha")

  /**
   * Die von einem Knoten aus erreichbaren Ziele werden in Gruppen unterteilt. Dieser Parameter (Angabe in Metern)
   * entscheidet, in welchen Abständen eine neue Gruppe erzeugt wird.
   */
  val AntsLaunchDistance = getInt("ant-scout.ant-net.ants-launch.distance")

  /**
   * Intervall in Millisekunden, in dem Ameisen mit Zielen aus der am weitesten entfernten Gruppe erzeugt werden.
   */
  val AntsLaunchInterval = getInt("ant-scout.ant-net.ants-launch.interval")

  /**
   * Mit abnehmender Entfernung wird pro Gruppe dieser Wert (Angabe in Millisekunden) zum AntsLaunchInterval hinzu
   * addiert.
   */
  val AntsLaunchIntervalIncrement = getInt("ant-scout.ant-net.ants-launch.interval-increment")

  /**
   * Maximales Alter einer Ameise in Millisekunden. Wenn die Ameise ihr Ziel nicht innerhalb dieser Zeit erreicht hat,
   * wird sie aus dem System entfernt.
   */
  val MaxAntAge = getMilliseconds("ant-scout.ant-net.max-ant-age")

  /**
   * Mit diesem Wert wird der beste Weg in der Pheromon-Matrix initialisiert, der als nächstes auf dem Weg zum Ziel
   * besucht werden sollte.
   */
  val BestWayPheromone = getDouble("ant-scout.ant-net.best-way-pheromone")

  /**
   * Gewichtungsfaktor, der den Einfluss des Verhältnisses der besten Fahrzeit zur aktuellen Fahrzeit bei der
   * Berechnung der Verstärkung angibt.
   */
  val C1 = getDouble("ant-scout.ant-net.c1")

  /**
   * Gewichtungsfaktor, der den Einfluss der Vertrauenswürdigkeit der aktuellen Fahrzeit bei der Berechnung der
   * Verstärkung angibt.
   */
  val C2 = getDouble("ant-scout.ant-net.c2")

  val DefaultSpeed = getDouble("ant-scout.default-speeds.default")

  /**
   * Schwellwert für den Vergleich von zwei Double-Werten.
   */
  val Epsilon = getDouble("ant-scout.epsilon")

  /**
   * Flag, das angibt, ob das Monitoring eingeschaltet ist.
   */
  val IsMonitoringEnabled = getBoolean("ant-scout.monitoring.enabled")

  /**
   * Flag, ob detaillierte (Log-)Ausgaben erzeugt werden sollen.
   *
   * Senkt die Performance und sollte nur zur Fehlersuche eingeschaltet werden!
   */
  val IsTraceEnabled = getBoolean("ant-scout.trace-is-enabled")

  /**
   * Karte, die verwendet werden soll.
   */
  val Map = getString("ant-scout.map")

  /**
   * Maximale Pfad-Länge.
   *
   * Der Routing-Service bricht die Suche nach einem Pfad ab, wenn der Pfad diese Länge erreicht.
   */
  val MaxPathLength = getInt("ant-scout.max-path-length")

  /**
   * Begrenzung der Buffer-Größen, in denen die Monitroing-Daten gespeichert werden. Diese Daten werden genutzt,
   * um z.B. Mittelwerte zu berechnen.
   * 0 schaltet die Buffer-Begrenzung aus.
   */
  val MonitoringBufferSize = getInt("ant-scout.monitoring.buffer-size")

  /**
   * Intervall in Sekunden, in dem Monitoring-Daten aufbereitet und im Front-End angezeigt werden.
   * Bei 0 werden die Monitoring-Daten nicht aufbereitet und auch nicht im Front-End angezeigt.
   */
  val MonitoringDataProcessingInterval = Duration(getInt("ant-scout.monitoring.data-processing-interval"),
    TimeUnit.SECONDS)

  /**
   * Weg-Klassen, die für den AntNet-Algorithmus berücksichtigt werden sollen.
   */
  val RelevantHighWays = getStringList("ant-scout.relevant-highways").asScala

  /**
   * Größe des gleitendes Beobachtungsfensters des lokalen statistischen Modells.
   */
  val Wmax = getInt("ant-scout.ant-net.w-max")

  /**
   * Faktor, der die Anzahl der Messungen bestimmt, die zum Berechnen des Mittelwertes und der Varianz des lokalen
   * statistischen Modells verwendet werden.
   */
  val Varsigma = getDouble("ant-scout.ant-net.varsigma")

  /**
   * Parameter z für die Berechnung der Verstärkung.
   */
  val Z = getDouble("ant-scout.ant-net.z")

  /**
   * Prüft, ob eine Standard-Geschwindigkeit für eine Weg-Klasse definiert ist und gibt diese zurück.
   *
   * @param highway Weg-Klasse.
   * @return Geschwindigkeit als Option-Klasse. None, wenn die Standard-Geschwindigkeit für die Weg-Klasse nicht
   *         definiert ist
   */
  def defaultSpeed(highway: String) = {
    val highwayPath = "ant-scout.default-speeds.%s" format highway
    if (config.hasPath(highwayPath))
      Some(config.getDouble(highwayPath))
    else
      None
  }
}
