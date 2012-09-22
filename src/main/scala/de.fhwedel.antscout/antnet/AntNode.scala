package de.fhwedel.antscout
package antnet

import akka.actor.{Cancellable, ActorRef, ActorLogging, Actor}
import akka.util.{Duration, Timeout}
import akka.util.duration._
import collection.mutable
import pheromoneMatrix.PheromoneMatrix
import routing.RoutingService
import map.Node
import java.util.concurrent.TimeUnit
import osm.{OsmNode, OsmMap}

class AntNode extends Actor with ActorLogging {

  import AntNode._

  /**
   * Ziele, die von diesem Knoten aus erreichbar sind.
   */
  val destinations = mutable.Set[ActorRef]()
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

  def initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]]) {
    log.debug("Initializing")
    this.destinations ++= mutable.Set(destinations.toSeq: _*)
    assert(this.destinations.nonEmpty)
    assert(!this.destinations.contains(self))
    val outgoingWays = AntMap.outgoingWays(AntNode.toNode(self).get)
    pheromoneMatrix = PheromoneMatrix(destinations, outgoingWays)
    val tripTimes = outgoingWays.map(outgoingWay => (outgoingWay -> outgoingWay.tripTime)).toMap
    pheromoneMatrix.initialize(self, pheromones, tripTimes)
    val bestWays = mutable.Map[ActorRef, AntWay]()
    log.debug("Calculating best ways")
    destinations.foreach(destination => bestWays += (destination -> bestWay(destination)))
    log.debug("Best ways calculated, result: {}, sending to the routing service", bestWays)
    AntScout.system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) !
      RoutingService.InitializeBestWays(self, bestWays)
    trafficModel = Some(TrafficModel(destinations, Settings.Varsigma, Settings.Wmax))
    cancellables += context.system.scheduler.schedule(Settings.ProcessStatisticsDelay, Settings.ProcessStatisticsDelay,
      self, ProcessStatistics)
    createAntsLaunchSchedulers(destinations)
    log.debug("Initialized")
  }

  def launchAnts(destinations: Set[ActorRef]) {
    log.debug("Launching ants, destinations")
    val startTime = System.currentTimeMillis
    for (destination <- destinations) {
      val ant = Ant(self, destination, "Visiting node %s".format(AntNode.nodeId(self)))
      val probabilities = pheromoneMatrix.probabilities(destination).toMap
      val startTime = System.currentTimeMillis
      val (nextNode, ant1) = ant.nextNode(self, probabilities)
      statistics.selectNextNodeDurations += System.currentTimeMillis - startTime
      nextNode ! ant1
    }
    statistics.launchAntsDurations += System.currentTimeMillis - startTime
    statistics.incrementLaunchedAnts(destinations.size)
  }

  override def postStop() {
    for (cancellable <- cancellables)
      cancellable.cancel()
  }

  def processAnt(ant: Ant) {
    if (self == ant.destination) {
      // Ziel erreicht
      statistics.antAges += ant.age
      val ant1 = ant.log("Destination reached, updating nodes")
//      log.info(ant1.prepareLogEntries)
      statistics.incrementDestinationReachedAnts()
      ant1.updateNodes()
    } else if (ant.age > Settings.MaxAntAge) {
      // Ameise ist zu alt
      statistics.antAges += ant.age
      val ant1 = ant.log("Lifetime expired, removing ant")
      statistics.incrementMaxAgeExceededAnts()
//      log.info(ant1.prepareLogEntries)
    } else if (!(pheromoneMatrix != null && pheromoneMatrix.probabilities.isDefinedAt(ant.destination))) {
      // Wenn die Pheromon-Matrix undefiniert ist, dann ist der Knoten kein gültiger Quell-Knoten (enthält keine
      // ausgehenden Wege.
      // Wenn die Wahrscheinlichkeiten für einen Ziel-Knoten undefiniert sind, dann ist der Ziel-Knoten von diesem
      // Knoten nicht erreichbar.
      // In beiden Fällen wird die Ameise aus dem System entfernt.
      statistics.antAges += ant.age
      val ant1 = ant.log("Dead end street reached, removing ant")
      statistics.incrementDeadEndStreetReachedAnts()
//      log.info(ant1.prepareLogEntries)
    } else {
      val ant1 = ant.log("Visiting node %s".format(AntNode.nodeId(self)))
      val probabilities = pheromoneMatrix.probabilities(ant1.destination).toMap
      val startTime = System.currentTimeMillis
      val (nextNode, ant2) = ant1.nextNode(self, probabilities)
      statistics.selectNextNodeDurations += System.currentTimeMillis - startTime
      nextNode ! ant2
    }
    statistics.processedAnts += 1
  }

  def updateDataStructures(destination: ActorRef, way: AntWay, tripTime: Double) = {
    trace(destination, "Updating data structures, source: %s, destination: %s, way: %s, trip time: %s".format(this,
      destination, way, tripTime))
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
      trace(destination, "Updating pheromones: way: %s, reinforcement: %s" format (way, reinforcement))
      val bestWayBeforeUpdate = bestWay(destination)
      trace(destination, "Before update: pheromones: %s, best way: %s" format (pheromoneMatrix.pheromones(destination),
        bestWayBeforeUpdate))
      pheromoneMatrix.updatePheromones(destination, way, reinforcement)
      statistics.updateDataStructuresDurations += System.currentTimeMillis - startTime
      val bestWayAfterUpdate = bestWay(destination)
      trace(destination, "After update: pheromones: %s, best way: %s" format (pheromoneMatrix.pheromones(destination),
        bestWayAfterUpdate))
      if (bestWayAfterUpdate != bestWayBeforeUpdate) {
        trace(destination, "Sending best way to routing service: %s" format bestWayAfterUpdate)
        AntScout.system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) !
          RoutingService.UpdateBestWay(self, destination, bestWayAfterUpdate)
      }
    }
  }

  protected def receive = {
    case ant: Ant =>
      processAnt(ant)
    case Initialize(destinations, pheromones) =>
      initialize(destinations, pheromones)
    case LaunchAnts(destinations) =>
      launchAnts(destinations)
    case ProcessStatistics =>
      context.parent ! statistics.prepare
      statistics.reset()
    case UpdateDataStructures(destination, way, tripTime) =>
      updateDataStructures(destination, way, tripTime)
    case m: Any =>
      log.warning("Unknown message: {}", m)
  }

  def trace(destination: ActorRef, message: String) {
    for {
      traceSourceId <- AntScout.traceSourceId
      traceDestinationId <- AntScout.traceDestinationId
      if (self.path.elements.last.matches(traceSourceId) && destination.path.elements.last.matches
        (traceDestinationId))
    } yield
      log.debug("{} {}", self.path.elements.last, message)
  }
}

object AntNode {

  case object DeadEndStreet
  case class Enter(destination: ActorRef)
  case class Initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]])
  case class LaunchAnts(destinations: Set[ActorRef])
  case class Probabilities(probabilities: Map[AntWay, Double])
  case object ProcessStatistics
  case class Statistics(antAge: Double, deadEndStreetReachedAnts: Int, destinationReachedAnts: Int,
    launchAntsDuration: Double, launchedAnts: Int, maxAgeExceededAnts: Int, processedAnts: Int,
    selectNextNodeDuration: Double, totalDeadEndStreetReachedAnts: Int, totalDestinationReachedAnts: Int,
    totalLaunchedAnts: Int, totalMaxAgeExceededAnts: Int, updateDataStructuresDuration: Double)
  case class UpdateDataStructures(destination: ActorRef, way: AntWay, tripTime: Double)

  /**
   * Sucht zu einem Knoten den passenden Ant-Knoten.
   *
   * @param node
   * @return
   */
  def apply(node: Node) = {
    AntScout.system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, node.id))
  }

  /**
   * Sucht zu einer Knoten-Id den passenden Ant-Knoten.
   *
   * @param nodeId
   * @return
   */
  def apply(nodeId: String) = {
    AntScout.system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, nodeId))
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
