package de.fhwedel.antscout
package antnet

import akka.actor.{Cancellable, ActorRef, ActorLogging, Actor}
import akka.util.{Duration, Timeout}
import akka.util.duration._
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
   * Ziele, die von diesem Knoten aus erreichbar sind.
   */
  val destinations = mutable.Set[ActorRef]()
  var lastProcessReceiveTime: Option[Long] = None
  var liftSession: Option[LiftSession] = None
  // TODO pheromoneMatrix sollte vom Datentyp Option[PheromoneMatrix] sein.
  var pheromoneMatrix: PheromoneMatrix = _
  val statistics = new AntNodeStatistics()
  implicit val timeout = Timeout(5 seconds)
  val cancellables = mutable.Set[Cancellable]()
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
    statistics.selectNextNodeDurations += System.currentTimeMillis - startTime
    nextNode ! (ant1, System.currentTimeMillis)
  }

  /**
   * Initialisiert diesen Knoten.
   *
   * @param destinations Erreichbare Ziele.
   * @param pheromones Pheromon-Matrix, mit der die Pheromon-Matrix des Knotens initialisiert werden soll.
   */
  def initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]]) {
    traceBySource("Initializing")
    this.destinations ++= mutable.Set(destinations.toSeq: _*)
    assert(!this.destinations.contains(self))
    if (destinations.nonEmpty) {
      val outgoingWays = AntMap.outgoingWays(AntNode.toNode(self).get)
      pheromoneMatrix = PheromoneMatrix(destinations, outgoingWays)
      val tripTimes = outgoingWays.map(outgoingWay => (outgoingWay -> outgoingWay.tripTime)).toMap
      pheromoneMatrix.initialize(self, pheromones, tripTimes)
      val bestWays = mutable.Map[ActorRef, AntWay]()
      traceBySource("Calculating best ways")
      destinations.foreach(destination => bestWays += (destination -> bestWay(destination)))
      traceBySource("Best ways calculated, result: %s, sending to the routing service" format bestWays)
      system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) !
        RoutingService.InitializeBestWays(self, bestWays)
      trafficModel = Some(TrafficModel(destinations, Settings.Varsigma, Settings.Wmax))
      createAntsLaunchSchedulers(destinations)
    }
    cancellables += context.system.scheduler.schedule(Settings.ProcessStatisticsDelay, Settings.ProcessStatisticsDelay,
      self, ProcessStatistics)
    traceBySource("Initialized")
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
      // sonst Ameisen ohne Log-Ausgaben erzeugen
      Some(destinations.map(destination => Ant(self, destination, false)))
    }
    for {
      ants <- ants
    } yield {
      ants.map { ant =>
        forwardAnt(ant)
        statistics.incrementLaunchedAnts(ant.destination)
      }
    }
    statistics.launchAntsDurations += System.currentTimeMillis - startTime
  }

  /**
   * Event-Handler, der nach dem Stoppen des Aktors augeführt wird.
   */
  override def postStop() {
    // alle schedule-Aktionen stoppen
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
        statistics.antAges += ant.age
        val ant1 = if (ant.isTraceEnabled)
          ant.log(Seq("Destination reached, visited %d nodes, took %d milliseconds" format (ant.memory.size, ant.age),
            "Memory: %s" format ant.memory.items.reverse.mkString("\n\t\t", "\n\t\t", ""), "Updating nodes"))
        else
          ant
        val ant2 = ant1.updateNodes()
        if (ant2.isTraceEnabled)
          log.debug("{}", ant2.prepareLogEntries)
        statistics.incrementArrivedAnts(ant.source)
      } else if (ant.age > Settings.MaxAntAge) {
        // Ameise ist zu alt
        statistics.antAges += ant.age
        val ant1 = if (ant.isTraceEnabled)
          ant.log("Lifetime expired, visited %d nodes, took %s milliseconds, removing ant" format (ant.memory.size,
            ant.age))
        else
          ant
        statistics.incrementMaxAgeExceededAnts()
        if (ant1.isTraceEnabled)
          log.debug("{}", ant1.prepareLogEntries)
      } else if (!(pheromoneMatrix != null && pheromoneMatrix.probabilities.isDefinedAt(ant.destination))) {
        // Wenn die Pheromon-Matrix undefiniert ist, dann ist der Knoten kein gültiger Quell-Knoten (enthält keine
        // ausgehenden Wege.
        // Wenn die Wahrscheinlichkeiten für einen Ziel-Knoten undefiniert sind, dann ist der Ziel-Knoten von diesem
        // Knoten nicht erreichbar.
        // In beiden Fällen wird die Ameise aus dem System entfernt.
        statistics.antAges += ant.age
        val ant1 = if (ant.isTraceEnabled)
          ant.log("Dead end street reached, visited %d nodes, took %d milliseconds, removing ant" format (ant.memory
            .size, ant.age))
        else
          ant
        statistics.incrementDeadEndStreetReachedAnts()
        if (ant1.isTraceEnabled)
          log.debug("{}", ant1.prepareLogEntries)
      } else {
        val ant1 = if (ant.isTraceEnabled)
          ant.log("Visiting node %s".format(AntNode.nodeId(self)))
        else
          ant
        forwardAnt(ant1)
      }
      statistics.processedAnts += 1
    }
    statistics.processAntDurations += time
  }

  /**
   * Verarbeitet die Statistiken.
   */
  def processStatistics() {
    // Statistik aufbereiten und an den Supervisor senden
    context.parent ! statistics.prepare
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
              if (AntNode.nodeId(self) == destination)
                actor.map(_ ! ArrivedAnts(statistics.arrivedAnts.getOrElse(AntNode(source), 0)))
              if (AntNode.nodeId(self) == source)
                actor.map(_ ! LaunchedAnts(statistics.launchedAnts.getOrElse(AntNode(destination), 0)))
              if (AntNode.nodeId(self) == node)
                actor.map(_ ! PassedAnts(statistics.passedAnts.getOrElse(AntNode(destination), 0)))
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
      trafficModel.addSample(destination, tripTime)
      val nodeId = self.path.elements.last
      val node = AntMap.nodes.find(_.id == nodeId).get
      val outgoingWays = AntMap.outgoingWays(node)
      val reinforcement = trafficModel.reinforcement(destination, tripTime, outgoingWays.size)
      trace(self, destination, "Updating pheromones: way: %s, reinforcement: %s" format (way, reinforcement))
      val bestWayBeforeUpdate = bestWay(destination)
      trace(self, destination, "Before update: pheromones: %s, best way: %s" format (pheromoneMatrix
        .pheromones(destination), bestWayBeforeUpdate))
      pheromoneMatrix.updatePheromones(destination, way, reinforcement)
      statistics.updateDataStructuresDurations += System.currentTimeMillis - startTime
      val bestWayAfterUpdate = bestWay(destination)
      trace(self, destination, "After update: pheromones: %s, best way: %s" format (pheromoneMatrix
        .pheromones(destination), bestWayAfterUpdate))
      if (bestWayAfterUpdate != bestWayBeforeUpdate) {
        trace(self, destination, "Sending best way to routing service: %s" format bestWayAfterUpdate)
        system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) !
          RoutingService.UpdateBestWay(self, destination, bestWayAfterUpdate)
      }
      // Anzahl der Ameisen erhöhen, die diesen Knoten auf dem Weg zum Ziel passiert haben.
      statistics.passedAnts += destination -> (statistics.passedAnts.getOrElse(destination, 0) + 1)
    }
  }

  /**
   * Verarbeitet eine empfangene Nachricht.
   */
  protected def receive = {
    case (ant: Ant, sendTime: Long) =>
      statistics.antsIdleTimes += System.currentTimeMillis - sendTime
      lastProcessReceiveTime.map(statistics.idleTimes += System.currentTimeMillis - _)
      lastProcessReceiveTime = Some(System.currentTimeMillis)
      processAnt(ant)
    case Initialize(destinations, pheromones) =>
      lastProcessReceiveTime.map(statistics.idleTimes += System.currentTimeMillis - _)
      lastProcessReceiveTime = Some(System.currentTimeMillis)
      initialize(destinations, pheromones)
    case LaunchAnts(destinations) =>
      lastProcessReceiveTime.map(statistics.idleTimes += System.currentTimeMillis - _)
      lastProcessReceiveTime = Some(System.currentTimeMillis)
      launchAnts(destinations)
    case liftSession: LiftSession =>
      lastProcessReceiveTime.map(statistics.idleTimes += System.currentTimeMillis - _)
      this.liftSession = Some(liftSession)
    case ProcessStatistics =>
      lastProcessReceiveTime.map(statistics.idleTimes += System.currentTimeMillis - _)
      lastProcessReceiveTime = Some(System.currentTimeMillis)
      processStatistics()
      statistics.reset()
    case UpdateDataStructures(destination, way, tripTime) =>
      lastProcessReceiveTime.map(statistics.idleTimes += System.currentTimeMillis - _)
      lastProcessReceiveTime = Some(System.currentTimeMillis)
      updateDataStructures(destination, way, tripTime)
    case m: Any =>
      lastProcessReceiveTime.map(statistics.idleTimes += System.currentTimeMillis - _)
      lastProcessReceiveTime = Some(System.currentTimeMillis)
      log.warning("Unknown message: {}", m)
  }

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

object AntNode {

  import map.Node

  case class ArrivedAnts(arrivedAnts: Int)
  case object DeadEndStreet
  case class Enter(destination: ActorRef)
  case class Initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]])
  case class LaunchAnts(destinations: Set[ActorRef])
  case class LaunchedAnts(arrivedAnts: Int)
  case class PassedAnts(arrivedAnts: Int)
  case class PheromonesAndProbabilities(pheromones: Seq[(AntWay, Double)], probabilities: Map[AntWay, Double])
  case object ProcessStatistics
  case class Statistics(
    antAge: Double,
    antsIdleTime: Double,
    arrivedAnts: Int,
    deadEndStreetReachedAnts: Int,
    idleTimes: mutable.Buffer[Long],
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
   * @param node
   * @return
   */
  def apply(node: Node) = {
    system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, node.id))
  }

  /**
   * Sucht zu einer Knoten-Id den passenden Ant-Knoten.
   *
   * @param nodeId
   * @return
   */
  def apply(nodeId: String) = {
    system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, nodeId))
  }

  /**
   * Extrahiert eine Knoten-Id aus einem Aktor-Pfad.
   *
   * @param antNode
   * @return
   */
  def nodeId(antNode: ActorRef) = antNode.path.elements.last

  /**
   * Sucht den passenden Knoten zu einem Ant-Knoten.
   *
   * @param antNode
   * @return
   */
  def toNode(antNode: ActorRef) = {
    val nodeId = this.nodeId(antNode)
    AntMap.nodes.find(_.id == nodeId)
  }

  implicit def toOsmNode(antNode: ActorRef): OsmNode = OsmMap.nodes(nodeId(antNode))
}
