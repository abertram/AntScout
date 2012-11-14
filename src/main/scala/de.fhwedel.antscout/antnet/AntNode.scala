package de.fhwedel.antscout
package antnet

import akka.actor.{Cancellable, ActorRef, ActorLogging, Actor}
import akka.util.Duration
import collection.mutable
import pheromoneMatrix.PheromoneMatrix
import routing.RoutingService
import java.util.concurrent.TimeUnit
import osm.{OsmNode, OsmMap}
import net.liftweb.util.TimeHelpers
import net.liftweb.http.{S, LiftSession, NamedCometListener}
import net.liftweb.common.Full

/**
 * Repräsentiert einen Knoten aus dem Graphen, auf dem der AntNet-Algorithmus operiert.
 */
class AntNode extends Actor with ActorLogging {

  import AntNode._

  /**
   * Cancellabeles werden beim Erzeugen von Schedulern zurückgegeben und erlauben es diese zu stoppen.
   */
  val cancellables = mutable.Set[Cancellable]()
  /**
   * Lift-Session
   */
  var liftSession: Option[LiftSession] = None
  // TODO pheromoneMatrix sollte vom Datentyp Option[PheromoneMatrix] sein.
  /**
   * Pheromon-Matrix
   */
  var pheromoneMatrix: PheromoneMatrix = _
  /**
   * Statistiken
   */
  val statistics = new AntNodeStatistics()
  /**
   * Lokales statistisches Modell
   */
  var trafficModel: Option[TrafficModel] = None

  /**
   * Berechnet anhand der Wahrscheinlichkeiten den aktuell besten ausgehenden Weg bezüglich eines Ziels.
   *
   * @param destination Ziel-Knoten, zu dem der ausgehende Weg berechnet werden soll.
   * @return Ein Tupel (way, probabiltiy), das den aktuell besten ausgehenden Weg zum Ziel-Knoten repräsentiert.
   */
  def bestWay(destination: ActorRef) = {
    // TODO Prüfen, ob nicht eine andere Datenstruktur verwendet werden kann, damit nicht ständig sortiert werden muss.
    val (bestWay, _) = pheromoneMatrix.probabilities(destination).toSeq.sortBy {
      case (way, probability) => probability
    }.last
    bestWay
  }

  /**
   * Erzeugt die Scheduler zum Erzeugen der Ameisen. Die Ziele werden nach Entfernung gruppiert und für jede Gruppe
   * ein eigener Scheduler erzeugt.
   *
   * @param destinations Erreichbare Ziele dieses Knotens.
   */
  def createAntsLaunchSchedulers(destinations: Set[ActorRef]) {
    val distances = destinations.map { destination =>
      // Enfernungen zu allen Zielen berechnen
      (destination, self.distanceTo(destination).round)
    }.groupBy {
      // gruppieren
      case (_, distance) => (distance / Settings.AntsLaunchGroupDistance).toInt
    }.map {
      // Gruppen-Id und Ziel beibehalten
      case (key, distances) =>
        key -> distances.map {
          case (destination, _) => destination
        }
    }
    // nötig für die Prüfung, ob alle Ziele durch die Scheduler abgedeckt sind
//    val processedDestinations = mutable.Set[ActorRef]()
    // Gruppen-Ids von groß nach klein verarbeiten
    (0 to distances.keys.max).foldRight(Settings.AntsLaunchInitialDelay) {
      case (i, delay) => {
        // Gruppen-Id definiert?
        if (distances.isDefinedAt(i)) {
          // Scheduler erzeugen
          cancellables += context.system.scheduler.schedule(Duration.Zero, Duration(delay,
            TimeUnit.MILLISECONDS), self, LaunchAnts(distances(i)))
//          processedDestinations ++= distances(i)
        }
        delay + Settings.AntsLaunchDelayIncrement
      }
    }
//    assert(this.destinations == processedDestinations)
  }

  /**
   * Leitet eine Ameise an den nächsten Knoten weiter.
   *
   * @param ant Ameise, die weitergeleitet werden soll.
   */
  def forwardAnt(ant: Ant) {
    val probabilities = pheromoneMatrix.probabilities(ant.destination)
    val startTime = System.currentTimeMillis
    val (nextNode, ant1) = ant.nextNode(self, probabilities)
    if (Settings.IsStatisticsEnabled) {
      statistics.selectNextNodeDurations += System.currentTimeMillis - startTime
    }
    nextNode ! (ant1, System.currentTimeMillis)
  }

  /**
   * Initialisiert diesen Knoten.
   *
   * @param destinations Erreichbare Ziele.
   * @param pheromones Pheromon-Matrix, mit der die Pheromon-Matrix des Knotens initialisiert werden soll.
   */
  def initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]]) {
    if (log.isDebugEnabled)
      log.debug("Initializing {}", self)
    if (destinations.nonEmpty) {
      // Lokales statischtisches Modell erzeugen und initialisieren
      trafficModel = Some(TrafficModel(destinations))
      // Ausgehende Wege
      val outgoingWays = AntMap.outgoingWays(AntNode.toNode(self).get)
      // Pheromon-Matrix erzeugen
      pheromoneMatrix = PheromoneMatrix(destinations, outgoingWays)
      // Reise-Zeite im statischen Fall
      val tripTimes = outgoingWays.map(outgoingWay => (outgoingWay -> outgoingWay.tripTime)).toMap
      // Pheromon-Matrix initialisieren
      pheromoneMatrix.initialize(pheromones, tripTimes)
      // Beste Wege berechnen
      val bestWays = mutable.Map[ActorRef, AntWay]()
      traceBySource("Calculating best ways")
      destinations.foreach(destination => bestWays += (destination -> bestWay(destination)))
      traceBySource("Best ways calculated, result: %s, sending to the routing service" format bestWays)
      // Beste Wege an der Routing-Service-Aktor schicken
      system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) !
        RoutingService.InitializeBestWays(bestWays)
      // Scheduler zum Erzeugen der Ameisen erzeugen
      createAntsLaunchSchedulers(destinations)
    }
    if (Settings.IsStatisticsEnabled) {
      // Scheduler zum Verarbeiten der Statistiken erzeugen
      cancellables += context.system.scheduler.schedule(Settings.ProcessStatisticsDelay,
        Settings.ProcessStatisticsDelay, self, ProcessStatistics)
    }
    if (log.isDebugEnabled)
      log.debug("{} initialized", self)
  }

  /**
   * Erzeugt die Ameisen.
   *
   * @param destinations Ziele der Ameisen.
   */
  def launchAnts(destinations: Set[ActorRef]) {
    val startTime = System.currentTimeMillis
    val ants = if (Settings.IsTraceEnabled) {
      // Wenn Tracing eingeschaltet ist, wird anhand der Session immer geprüft, ob die erzeugte Ameise detaillierte
      // Log-Ausgaben erzeugen soll.
      for {
        liftSession <- liftSession
        // Ameisen erzeugen
        ants = S.initIfUninitted(liftSession) {
          destinations.map { destination =>
            (for {
              isTraceEnabled <- IsTraceEnabled
              if isTraceEnabled
              node <- Node
              traceDestination <- Destination
              if AntNode.nodeId(self) == node && AntNode.nodeId(destination) == traceDestination
            } yield {
              Ant(self, destination, Ant.logEntry("Visiting node %s".format(AntNode.nodeId(self))), true)
            }) getOrElse(Ant(self, destination, false))
          }
        }
      } yield
        ants
    } else {
      // Sonst Ameisen ohne Log-Ausgaben erzeugen
      Some(destinations.map(destination => Ant(self, destination, false)))
    }
    // Ameisen gleich zum nächsten Knoten weiterleiten
    for {
      ants <- ants
    } yield {
      ants.map { ant =>
        forwardAnt(ant)
        if (Settings.IsStatisticsEnabled) {
          statistics.incrementLaunchedAnts(ant.destination)
        }
      }
    }
    if (Settings.IsStatisticsEnabled) {
      statistics.launchAntsDurations += System.currentTimeMillis - startTime
    }
  }

  /**
   * Event-Handler, der nach dem Stoppen des Aktors augeführt wird.
   */
  override def postStop() {
    // Alle schedule-Aktionen stoppen
    for (cancellable <- cancellables)
      cancellable.cancel()
  }

  /**
   * Verarbeitet eine Ameise.
   *
   * @param ant Die zu verarbeitende Ameise.
   */
  def processAnt(ant: Ant) {
    val (time, _) = TimeHelpers.calcTime {
      if (self == ant.destination) {
        // Ziel erreicht
        if (Settings.IsStatisticsEnabled) {
          statistics.antAges += ant.age
        }
        // Ameise evtl. um Log-Ausgaben erweitern
        val ant1 = if (ant.isTraceEnabled)
          ant.log(Seq("Destination reached, visited %d nodes, took %d milliseconds" format (ant.memory.size, ant.age),
            "Memory: %s" format ant.memory.items.reverse.mkString("\n\t\t", "\n\t\t", ""), "Updating nodes"))
        else
          ant
        // Knoten aktualisieren
        val ant2 = ant1.updateNodes()
        if (ant2.isTraceEnabled)
          log.debug("{}", ant2.prepareLogEntries)
        if (Settings.IsStatisticsEnabled) {
          statistics.incrementArrivedAnts(ant.source)
        }
      } else if (ant.age > Settings.MaxAntAge) {
        // Ameise ist zu alt
        if (Settings.IsStatisticsEnabled) {
          statistics.antAges += ant.age
        }
        // Ameise evtl. um Log-Ausgaben erweitern
        val ant1 = if (ant.isTraceEnabled)
          ant.log("Lifetime expired, visited %d nodes, took %s milliseconds, removing ant" format (ant.memory.size,
            ant.age))
        else
          ant
        if (Settings.IsStatisticsEnabled) {
          statistics.incrementMaxAgeExceededAnts()
        }
        if (ant1.isTraceEnabled)
          log.debug("{}", ant1.prepareLogEntries)
      } else if (!(pheromoneMatrix != null && pheromoneMatrix.probabilities.isDefinedAt(ant.destination))) {
        // Wenn die Pheromon-Matrix undefiniert ist, dann ist der Knoten kein gültiger Quell-Knoten (enthält keine
        // ausgehenden Wege.
        // Wenn die Wahrscheinlichkeiten für einen Ziel-Knoten undefiniert sind, dann ist der Ziel-Knoten von diesem
        // Knoten nicht erreichbar.
        // In beiden Fällen wird die Ameise aus dem System entfernt.
        if (Settings.IsStatisticsEnabled) {
          statistics.antAges += ant.age
        }
        // Ameise evtl. um Log-Ausgaben erweitern
        val ant1 = if (ant.isTraceEnabled)
          ant.log("Dead end street reached, visited %d nodes, took %d milliseconds, removing ant" format (ant.memory
            .size, ant.age))
        else
          ant
        if (Settings.IsStatisticsEnabled) {
          statistics.incrementDeadEndStreetReachedAnts()
        }
        if (ant1.isTraceEnabled)
          log.debug("{}", ant1.prepareLogEntries)
      } else {
        // Sonst Ameise evtl. um Log-Ausgaben erweitern und weiterleiten
        val ant1 = if (ant.isTraceEnabled)
          ant.log("Visiting node %s".format(AntNode.nodeId(self)))
        else
          ant
        forwardAnt(ant1)
      }
      if (Settings.IsStatisticsEnabled) {
        statistics.processedAnts += 1
      }
    }
    if (Settings.IsStatisticsEnabled) {
      statistics.processAntDurations += time
    }
  }

  /**
   * Verarbeitet die Statistiken.
   */
  def processStatistics() {
    // Statistik aufbereiten und an den Supervisor senden
    context.parent ! statistics.prepare
    // Wenn Tracing eingeschaltet ist, lokale Statistiken im UserInterface anzeigen
    if (Settings.IsTraceEnabled) {
      for {
        liftSession <- liftSession
      } yield {
        S.initIfUninitted(liftSession) {
          for {
            source <- Source
            node <- Node
            destination <- Destination
          } yield {
            NamedCometListener.getDispatchersFor(Full("userInterface")) foreach { actor =>
              // Anzahl der Ameisen, die an diesem Knoten als Ziel angekommen sind
              if (AntNode.nodeId(self) == destination)
                actor.map(_ ! ArrivedAnts(statistics.arrivedAnts.getOrElse(AntNode(source), 0)))
              // Anzahl der Ameisen, die an diesem Knoten erzeugt wurden
              if (AntNode.nodeId(self) == source)
                actor.map(_ ! LaunchedAnts(statistics.launchedAnts.getOrElse(AntNode(destination), 0)))
              // Anzahl der Ameisen, die diesen Knoten auf ihrem Weg zum Ziel passiert haben
              if (AntNode.nodeId(self) == node)
                actor.map(_ ! PassedAnts(statistics.passedAnts.getOrElse(AntNode(destination), 0)))
              // Ausschnitt der Pheromon-Matrix anzeigen
              if (AntNode.nodeId(self) == node && AntNode.nodeId(self) != destination && pheromoneMatrix != null)
                actor.map(_ ! PheromonesAndProbabilities(pheromoneMatrix.pheromones(AntNode(destination)).toSeq,
                  pheromoneMatrix.probabilities(AntNode(destination)).toMap))
            }
          }
        }
      }
    }
  }

  /**
   * Aktualisiert die Datenstrukturen dieses Knotens.
   *
   * @param destination Ziel-Knoten, dessen Daten aktualisiert werden sollen.
   * @param way Weg, den die Ameise gewählt hat, um das Ziel zu erreichen.
   * @param tripTime Reisezeit von diesem Knoten aus zum Ziel.
   */
  def updateDataStructures(destination: ActorRef, way: AntWay, tripTime: Double) = {
    trace(self, destination, ("Updating data structures, source: %s, destination: %s, way: %s, trip time: %s")
      .format(self, destination, way, tripTime))
    // für Debug-Zwecke
//    assert(trafficModel.isDefined, "Traffic model is undefined, node: %s, destination: %s, way: %s".format(self,
//      destination, way))
    for (trafficModel <- this.trafficModel) {
      // für Debug-Zwecke
//      assert(trafficModel.samples.isDefinedAt(destination), "Node: %s, destination: %s".format(self, destination))
      val startTime = System.currentTimeMillis
      // Eintrag zum lokalen statistischen Modell hinzufügen
      trafficModel.addSample(destination, tripTime)
      // Ausgehende Wege berechnen
      val outgoingWays = AntMap.outgoingWays(toNode(self).get)
      // Verstärkung berechnen
      val reinforcement = trafficModel.reinforcement(destination, tripTime, outgoingWays.size)
      trace(self, destination, "Updating pheromones: way: %s, reinforcement: %s" format (way, reinforcement))
      // Bester Weg vor dem Update
      val bestWayBeforeUpdate = bestWay(destination)
      trace(self, destination, "Before update: pheromones: %s, best way: %s" format (pheromoneMatrix
        .pheromones(destination), bestWayBeforeUpdate))
      // Pheromon-Matrix aktualisieren
      pheromoneMatrix.updatePheromones(destination, way, reinforcement)
      if (Settings.IsStatisticsEnabled) {
        // Dauer des Updates für die Statistik berechnen
        statistics.updateDataStructuresDurations += System.currentTimeMillis - startTime
      }
      // Bester Weg nach dem Update
      val bestWayAfterUpdate = bestWay(destination)
      trace(self, destination, "After update: pheromones: %s, best way: %s" format (pheromoneMatrix
        .pheromones(destination), bestWayAfterUpdate))
      // Wenn sich der beste Weg verändert hat
      if (bestWayAfterUpdate != bestWayBeforeUpdate) {
        trace(self, destination, "Sending best way to routing service: %s" format bestWayAfterUpdate)
        // Besten Weg an den Routing-Service schicken
        system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) !
          RoutingService.UpdateBestWay(destination, bestWayAfterUpdate)
      }
      if (Settings.IsStatisticsEnabled) {
        // Anzahl der Ameisen erhöhen, die diesen Knoten auf dem Weg zum Ziel passiert haben.
        statistics.passedAnts += destination -> (statistics.passedAnts.getOrElse(destination, 0) + 1)
      }
    }
  }

  /**
   * Verarbeitet eine empfangene Nachricht.
   */
  protected def receive = {
    // Ameise verarbeiten
    case (ant: Ant, sendTime: Long) =>
      if (Settings.IsStatisticsEnabled) {
        statistics.antsIdleTimes += System.currentTimeMillis - sendTime
      }
      processAnt(ant)
    // Initialisieren
    case Initialize(destinations, pheromones) =>
      initialize(destinations, pheromones)
    // Ameisen erzeugen
    case LaunchAnts(destinations) =>
      launchAnts(destinations)
    // Lift-Session
    case liftSession: LiftSession =>
      this.liftSession = Some(liftSession)
    // Statistiken verarbeiten und zurücksetzen
    case ProcessStatistics =>
      processStatistics()
      statistics.reset()
    // Daten-Strukturen initialisieren
    case UpdateDataStructures(destination, way, tripTime) =>
      updateDataStructures(destination, way, tripTime)
  }

  /**
   * Erzeugt Debug-Ausgaben, wenn Quell- und Ziel-Knoten mit denen übereinstimmen, die getraced werden sollen.
   *
   * @param source Quelle
   * @param destination Ziel
   * @param message Log-Eintrag
   */
  def trace(source: => ActorRef, destination: => ActorRef, message: => String) {
    if (Settings.IsTraceEnabled) {
      for {
        liftSession <- liftSession
      } yield {
        S.initIfUninitted(liftSession) {
          for {
            isTraceEnabled <- IsTraceEnabled
            if isTraceEnabled
            traceSource <- Node
            traceDestination <- Destination
            if (AntNode.nodeId(source) == traceSource && AntNode.nodeId(destination) == traceDestination)
          } yield
            log.debug("{}", message)
        }
      }
    }
  }

  /**
   * Erzeugt Debug-Ausgaben, wenn der Ziel-Knoten mit dem übereinstimmt, der getraced werden soll.
   *
   * @param destination Ziel
   * @param message Log-Eintrag
   */
  def traceByDestination(destination: => ActorRef, message: => String) {
    if (Settings.IsTraceEnabled) {
      for {
        isTraceEnabled <- IsTraceEnabled
        if isTraceEnabled
        traceDestination <- Destination
        if (AntNode.nodeId(destination) == traceDestination)
      } yield
        log.debug("{}", message)
    }
  }

  /**
   * Erzeugt Debug-Ausgaben, wenn der aktuelle Knoten mit dem übereinstimmt, der getraced werden soll.
   *
   * @param message
   * @param source
   */
  def traceBySource(message: => String, source: => ActorRef = self) {
    if (Settings.IsTraceEnabled) {
      for {
        isTraceEnabled <- IsTraceEnabled
        if isTraceEnabled
        traceSource <- Source
        if (AntNode.nodeId(source).matches(traceSource))
      } yield
        log.debug("{}", message)
    }
  }
}

/**
 * AntNode-Factory.
 */
object AntNode {

  import map.Node

  /**
   * Angekommene Ameisen.
   *
   * @param arrivedAnts Angekommene Ameisen
   */
  case class ArrivedAnts(arrivedAnts: Int)

  /**
   * Initialisierung.
   *
   * @param destinations Ziele
   * @param pheromones Initiale Pheromone
   */
  case class Initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]])

  /**
   * Ameisen erzeugen.
   *
   * @param destinations Ziele
   */
  case class LaunchAnts(destinations: Set[ActorRef])

  /**
   * Erzeugte Ameisen.
   *
   * @param launchedAnts Erzeugte Ameisen
   */
  case class LaunchedAnts(launchedAnts: Int)

  /**
   * Passierte Ameisen.
   *
   * @param passedAnts Passierte Ameisen
   */
  case class PassedAnts(passedAnts: Int)

  /**
   * Pheromone und Wahrscheinlichkeiten.
   *
   * @param pheromones Pheromone
   * @param probabilities Wahrscheinlichkeiten
   */
  case class PheromonesAndProbabilities(pheromones: Seq[(AntWay, Double)], probabilities: Map[AntWay, Double])

  /**
   * Statistiken verarbeiten.
   */
  case object ProcessStatistics

  /**
   * Statistiken.
   *
   * @param antAge Ameisen-Alter
   * @param antsIdleTime Ameisen-Leerlauf-Zeit
   * @param arrivedAnts Angekommen Ameisen pro Statistik-Zyklus
   * @param deadEndStreetReachedAnts Aufgrund von Sackgassen entfernte Ameisen pro Statistik-Zyklus
   * @param launchAntsDuration Dauer der Erzeugung von Ameisen
   * @param launchedAnts Erzeugte Ameisen pro Statistik-Zyklus
   * @param maxAgeExceededAnts Aufgrund des Alters entfernte Ameisen pro Statistik-Zyklus
   * @param processAntDuration Dauer der Verarbeitung einer Ameise
   * @param processedAnts Verarbeitete Ameisen
   * @param selectNextNodeDuration Dauer der Auswahl des nächsten Knotens
   * @param totalArrivedAnts Insgesamt angekommene Ameisen
   * @param totalDeadEndStreetReachedAnts Insgesamt aufgrund von Sackgassen entfernte Ameisen
   * @param totalLaunchedAnts Insgesamt erzeugte Ameisen
   * @param totalMaxAgeExceededAnts Insgesamt aufgrund des Alters entfernte Ameisen
   * @param updateDataStructuresDuration Dauer der Aktualisierung der Daten-Strukturen
   */
  case class Statistics(
    antAge: Double,
    antsIdleTime: Double,
    arrivedAnts: Int,
    deadEndStreetReachedAnts: Int,
    launchAntsDuration: Double,
    launchedAnts: Int,
    maxAgeExceededAnts: Int,
    processAntDuration: Double,
    processedAnts: Int,
    selectNextNodeDuration: Double,
    totalArrivedAnts: Int,
    totalDeadEndStreetReachedAnts: Int,
    totalLaunchedAnts: Int,
    totalMaxAgeExceededAnts: Int,
    updateDataStructuresDuration: Double)
  case class UpdateDataStructures(destination: ActorRef, way: AntWay, tripTime: Double)

  /**
   * Sucht zu einem Knoten den passenden Ant-Knoten.
   *
   * @param node Knoten
   * @return Ant-Knoten
   */
  def apply(node: Node) = {
    system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, node.id))
  }

  /**
   * Sucht zu einer Knoten-Id den passenden Ant-Knoten.
   *
   * @param nodeId Knoten-Id
   * @return Ant-Knoten
   */
  def apply(nodeId: String) = {
    system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, nodeId))
  }

  /**
   * Extrahiert eine Knoten-Id aus einem Aktor-Pfad.
   *
   * @param antNode Ant-Knoten
   * @return Knoten-Id
   */
  def nodeId(antNode: ActorRef) = antNode.path.elements.last

  /**
   * Sucht den passenden Knoten zu einem Ant-Knoten.
   *
   * @param antNode Ant-Knoten
   * @return Knoten
   */
  def toNode(antNode: ActorRef) = {
    val nodeId = this.nodeId(antNode)
    AntMap.nodes.find(_.id == nodeId)
  }

  /**
   * Wandelt implizit einen Ant-Knoten in einen Osm-Knoten um.
   *
   * @param antNode Ant-Knoten
   * @return Osm-Knoten
   */
  implicit def toOsmNode(antNode: ActorRef): OsmNode = OsmMap.nodes(nodeId(antNode))
}
