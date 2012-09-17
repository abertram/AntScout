package de.fhwedel.antscout

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

/**
 * Konfiguration.
 */
object Settings {

  val config = ConfigFactory.load

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
   * Anzahl der Ameisen, die pro Knoten erzeugt werden.
   */
  val AntCountPerSource = getInt("ant-scout.ant-count-per-source")

  /**
   * Intervall in Millisekunden, in dem die Ameisen erzeugt werden.
   */
  val AntLaunchDelay = getInt("ant-scout.ant-net.ant-launch-delay")

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
   * Karte, die verwendet werden soll.
   */
  val Map = getString("ant-scout.map")

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