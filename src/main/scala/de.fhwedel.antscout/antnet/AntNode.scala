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

  def bestWay(destination: ActorRef) = {
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
   * Initialisiert diesen Knoten.
   *
   * @param destinations Erreichbare Ziele.
   * @param pheromones Pheromon-Matrix, mit der die Pheromon-Matrix des Knotens initialisiert werden soll.
   */
  def initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]]) {
    traceBySource("Initializing")
    this.destinations ++= mutable.Set(destinations.toSeq: _*)
    assert(this.destinations.nonEmpty)
    assert(!this.destinations.contains(self))
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
    cancellables += context.system.scheduler.schedule(Settings.ProcessStatisticsDelay, Settings.ProcessStatisticsDelay,
      self, ProcessStatistics)
    createAntsLaunchSchedulers(destinations)
    traceBySource("Initialized")
  }

  /**
   * Erzeugt die Ameisen.
   *
   * @param destinations Ziele der Ameisen.
   */
  def launchAnts(destinations: Set[ActorRef]) {
//    if (ShouldLog) {
//      for (traceDestinationId <- TraceDestinationId if destinations.contains(AntNode(traceDestinationId))) {
//        traceBySource("Launching ants, destinations: %s" format destinations)
//      }
//    }
    val startTime = System.currentTimeMillis
    for (destination <- destinations) {
      val ant = if (Ant.ShouldLog)
        Ant(self, destination, Ant.logEntry("Visiting node %s".format(AntNode.nodeId(self))))
      else
        Ant(self, destination)
      val probabilities = pheromoneMatrix.probabilities(destination).toMap
      val startTime = System.currentTimeMillis
      val (nextNode, ant1) = ant.nextNode(self, probabilities)
      statistics.selectNextNodeDurations += System.currentTimeMillis - startTime
      nextNode ! (ant1, System.currentTimeMillis)
    }
    statistics.launchAntsDurations += System.currentTimeMillis - startTime
    statistics.incrementLaunchedAnts(destinations.size)
  }

  /**
   * Event-Handler, der nach dem Stoppen des Aktors augeführt wird.
   */
  override def postStop() {
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
        val ant1 = if (Ant.ShouldLog)
          ant.log(Seq("Destination reached, visited %d nodes, took %d milliseconds" format (ant.memory.size, ant.age),
            "Memory: %s" format ant.memory.items.reverse.mkString("\n\t\t", "\t\t", ""), "Updating nodes"))
        else
          ant
        statistics.incrementDestinationReachedAnts()
        val ant2 = ant1.updateNodes()
        if (ShouldLog)
          trace(ant2.source, ant2.destination, ant2.prepareLogEntries)
      } else if (ant.age > Settings.MaxAntAge) {
        // Ameise ist zu alt
        statistics.antAges += ant.age
        val ant1 = if (Ant.ShouldLog)
          ant.log("Lifetime expired, visited %d nodes, took %s milliseconds, removing ant" format (ant.memory.size,
            ant.age))
        else
          ant
        statistics.incrementMaxAgeExceededAnts()
        if (ShouldLog)
          trace(ant1.source, ant1.destination, ant1.prepareLogEntries)
      } else if (!(pheromoneMatrix != null && pheromoneMatrix.probabilities.isDefinedAt(ant.destination))) {
        // Wenn die Pheromon-Matrix undefiniert ist, dann ist der Knoten kein gültiger Quell-Knoten (enthält keine
        // ausgehenden Wege.
        // Wenn die Wahrscheinlichkeiten für einen Ziel-Knoten undefiniert sind, dann ist der Ziel-Knoten von diesem
        // Knoten nicht erreichbar.
        // In beiden Fällen wird die Ameise aus dem System entfernt.
        statistics.antAges += ant.age
        val ant1 = if (Ant.ShouldLog)
          ant.log("Dead end street reached, visited %d nodes, took %d milliseconds, removing ant" format (ant.memory
            .size, ant.age))
        else
          ant
        statistics.incrementDeadEndStreetReachedAnts()
        if (ShouldLog)
          trace(ant1.source, ant1.destination, ant1.prepareLogEntries)
      } else {
        val ant1 = if (Ant.ShouldLog)
          ant.log("Visiting node %s".format(AntNode.nodeId(self)))
        else
          ant
        val probabilities = pheromoneMatrix.probabilities(ant1.destination).toMap
        val startTime = System.currentTimeMillis
        val (nextNode, ant2) = ant1.nextNode(self, probabilities)
        statistics.selectNextNodeDurations += System.currentTimeMillis - startTime
        nextNode ! (ant2, System.currentTimeMillis)
      }
      statistics.processedAnts += 1
    }
    statistics.processAntDurations += time
  }

  def updateDataStructures(destination: ActorRef, way: AntWay, tripTime: Double) = {
    if (ShouldLog)
      trace(self, destination, ("Updating data structures, source: %s, destination: %s, way: %s, trip time: %s")
        .format(self, destination, way, tripTime))
    assert(trafficModel.isDefined, "Traffic model is undefined, node: %s, destination: %s, way: %s".format(self,
      destination, way))
    for (trafficModel <- this.trafficModel) {
      assert(trafficModel.samples.isDefinedAt(destination), "Node: %s, destination: %s".format(self, destination))
      val startTime = System.currentTimeMillis
      trafficModel.addSample(destination, tripTime)
      val nodeId = self.path.elements.last
      val node = AntMap.nodes.find(_.id == nodeId).get
      val outgoingWays = AntMap.outgoingWays(node)
      val reinforcement = trafficModel.reinforcement(destination, tripTime, outgoingWays.size)
      if (ShouldLog)
        trace(self, destination, "Updating pheromones: way: %s, reinforcement: %s" format (way, reinforcement))
      val bestWayBeforeUpdate = bestWay(destination)
      if (ShouldLog)
        trace(self, destination, "Before update: pheromones: %s, best way: %s" format (pheromoneMatrix
          .pheromones(destination), bestWayBeforeUpdate))
      pheromoneMatrix.updatePheromones(destination, way, reinforcement)
      for {
        liftSession <- liftSession
      } yield {
        S.initIfUninitted(liftSession) {
          for {
            node <- Node
            selectedDestination <- Destination
            if (AntNode.nodeId(self) == node && AntNode.nodeId(destination) == selectedDestination)
          } yield {
            NamedCometListener.getDispatchersFor(Full("userInterface")) foreach { actor =>
              actor.map(_ ! PheromonesAndProbabilities(node, selectedDestination, pheromoneMatrix
                .pheromones(destination).toSeq, pheromoneMatrix.probabilities(destination).toSeq))
            }
          }
        }
      }
      statistics.updateDataStructuresDurations += System.currentTimeMillis - startTime
      val bestWayAfterUpdate = bestWay(destination)
      if (ShouldLog)
        trace(self, destination, "After update: pheromones: %s, best way: %s" format (pheromoneMatrix
          .pheromones(destination), bestWayAfterUpdate))
      if (bestWayAfterUpdate != bestWayBeforeUpdate) {
        if (ShouldLog)
          trace(self, destination, "Sending best way to routing service: %s" format bestWayAfterUpdate)
        system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) !
          RoutingService.UpdateBestWay(self, destination, bestWayAfterUpdate)
      }
    }
  }

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
      this.liftSession = Some(liftSession)
    case ProcessStatistics =>
      lastProcessReceiveTime.map(statistics.idleTimes += System.currentTimeMillis - _)
      lastProcessReceiveTime = Some(System.currentTimeMillis)
      context.parent ! statistics.prepare
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

  def trace(source: ActorRef, destination: ActorRef, message: String) {
    for {
      traceSourceId <- TraceSourceId
      traceDestinationId <- TraceDestinationId
      if (AntNode.nodeId(source).matches(traceSourceId) && AntNode.nodeId(destination).matches(traceDestinationId))
    } yield
      log.info("{}", message)
  }

  def traceByDestination(destination: ActorRef, message: String) {
    for {
      traceDestinationId <- TraceDestinationId
      if (AntNode.nodeId(destination).matches(traceDestinationId))
    } yield
      log.info("{}", message)
  }

  def traceBySource(message: String, source: ActorRef = self) {
    for {
      traceSourceId <- TraceSourceId
      if (AntNode.nodeId(source).matches(traceSourceId))
    } yield
      log.info("{}", message)
  }
}

object AntNode {

  import map.Node

  val ShouldLog = false

  case object DeadEndStreet
  case class Enter(destination: ActorRef)
  case class Initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]])
  case class LaunchAnts(destinations: Set[ActorRef])
  case class PheromonesAndProbabilities(source: String, destination: String, pheromones: Seq[(AntWay, Double)],
    probabilities: Seq[(AntWay, Double)])
  case object ProcessStatistics
  case class Statistics(
    antAge: Double,
    antsIdleTime: Double,
    deadEndStreetReachedAnts: Int,
    destinationReachedAnts: Int,
    idleTimes: mutable.Buffer[Long],
    launchAntsDuration: Double,
    launchedAnts: Int,
    maxAgeExceededAnts: Int,
    processAntDuration: Double,
    processedAnts: Int,
    selectNextNodeDuration: Double,
    totalDeadEndStreetReachedAnts: Int,
    totalDestinationReachedAnts: Int,
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
