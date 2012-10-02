require(["jquery", "styles", "bootstrap", "leaflet-src", "openlayers/OpenLayers", "underscore"], ($, styles) ->

  # globales AntScout-Object erstellen
  @AntScout = {}

  cloudMadeUrl = 'http://{s}.tile.cloudmade.com/{apiKey}/{styleId}/256/{z}/{x}/{y}.png'
  cloudMadeAttribution = 'Map data &copy; 2011 OpenStreetMap contributors, Imagery &copy; 2011 CloudMade';

  cloudMadeLayer = L.tileLayer(cloudMadeUrl,
    attribution: cloudMadeAttribution
    apiKey: "396d772f0ece43f49d3801843fd86fbc"
    styleId: 997
  )

  destination = null
  destinationMarker = null
  directionsLayer = new L.LayerGroup()
  EPSG4326Projection = new OpenLayers.Projection("EPSG:4326")
  incomingWaysLayer = new L.LayerGroup()
  map = null
  nodes = null
  nodesLayer = new L.LayerGroup()
  osmLayer = L.tileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    attribution: cloudMadeAttribution
  )

  outgoingWaysLayer = new L.LayerGroup()
  ways = null
  waysLayer = new L.LayerGroup()
  selectFeatureControl = new OpenLayers.Control.SelectFeature([nodesLayer, waysLayer]);
  selectedNode = null
  source = null
  sourceMarker = null

  deselectNode = () ->
    if selectedNode?
      incomingWaysLayer.clearLayers()
      outgoingWaysLayer.clearLayers()
      selectedNode.setRadius(styles.node.radius)
      selectedNode.setStyle(styles.node)
      selectedNode = null

  retrieveNodes = () ->
    $.get "nodes",
      (nodes) ->
        this.nodes = this
        for node in nodes
          layer = L.circleMarker([node.latitude, node.longitude], styles.node)
          layer.node = node
          layer.addTo(nodesLayer).on("click", (e) -> selectNode(e.target))
        map.fitBounds(for node in nodes
          [node.latitude, node.longitude])

  retrieveWays = () ->
    $.get "ways",
      (ways) ->
        addWaysToLayer(ways, waysLayer, styles.waysDefaultStyle)
        map.fitBounds(for way in ways
          for node in way.nodes
            [node.latitude, node.longitude]
        )

  selectNode = (nodePath) ->
    shouldSelect = !selectedNode? or nodePath != selectedNode
    deselectNode()
    if shouldSelect
      nodePath.setRadius(styles.selectedNode.radius)
      nodePath.setStyle(styles.selectedNode)
      selectedNode = nodePath
      $("#nodeId").html("<a href=\"http://www.openstreetmap.org/browse/node/#{ selectedNode.node.id }\">#{ selectedNode.node.id }</a>")
      $("#nodeLongitude").html(selectedNode.node.longitude)
      $("#nodeLatitude").html(selectedNode.node.latitude)
      retrieveNode(selectedNode.node.id)

  $(() ->
    map = L.map("map",
      layers: [cloudMadeLayer, incomingWaysLayer, osmLayer, outgoingWaysLayer, waysLayer, directionsLayer, nodesLayer]
    ).fitWorld()
    baseLayers =
      "CloudMade": cloudMadeLayer
      "OpenStreetMap": osmLayer
    overlayLayers =
      "Incoming ways": incomingWaysLayer
      "Outgoing ways": outgoingWaysLayer
      "Ways": waysLayer
      "Directions": directionsLayer
      "Nodes": nodesLayer
    L.control.layers(baseLayers, overlayLayers).addTo(map)
    L.control.scale().addTo(map)
    retrieveNodes()
    retrieveWays()
    $("#retrieveDirections").click -> retrieveDirections()
    $("#setNodeAsSource").click -> setNodeAsSource()
    $("#setNodeAsDestination").click -> setNodeAsDestination()
    $("#wayEditMaxSpeed, #waySaveMaxSpeed, #wayCancelEditMaxSpeed").click -> toggleWayEditMaxSpeedControls()
    $("#wayEditMaxSpeed").click -> $("#wayMaxSpeed").select()
    $("#waySaveMaxSpeed").click ->
      id = waysLayer.selectedFeatures[0].attributes.way.id
      maxSpeed = parseFloat($("#wayMaxSpeed").val().replace(",", "."))
      $.ajax({
        contentType: "application/json"
        type: "PUT"
        url: "way/#{ id }"
        data: JSON.stringify(
          maxSpeed: maxSpeed
        )
      }).done (way) ->
        wayFeature = _.find(waysLayer.features, (feature) -> feature.attributes.way.id == id)
        wayFeature.attributes.way = way
        displayWayData(way)
    $(document).ajaxError((event, jqXHR, ajaxSettings, thrownError) ->
      showErrorMessage(jqXHR.responseText)
    )
  )

  addWaysToLayer = (ways, layer, style) ->
    for way in ways
      polyline = new L.Polyline((for node in way.nodes
        [node.latitude, node.longitude]), style)
      polyline.way = way
      polyline.on("click", (e) -> displayWayData(this.way)).addTo(layer)

  disable = (elementId) ->
    $("##{ elementId }").prop("disabled", true)

  displayWayData = (way) ->
    id = way.id
    $("#wayId").html(id)
    $("#wayLength").val(way.length)
    $("#wayMaxSpeed").val(way.maxSpeed)
    $("#wayTripTime").val(way.tripTime)
    nodes = for node in way.nodes
      "<a href=\"http://www.openstreetmap.org/browse/node/#{ node.id }\">#{ node.id }</a>"
    $("#way-nodes").html("<ul><li>" + nodes.join("</li><li>") + "</li></ul>")

  drawDirections = (directions) ->
    if directions? and directions.ways.length > 0
      $("#pathLength").html(directions.length)
      $("#pathTripTime").html(directions.tripTime)
      addWaysToLayer(directions.ways, directionsLayer)

  AntScout.drawPath = (path) ->
    console.debug("Drawing path - path: " + path)
    drawDirections(path)

  enable = (elementId) ->
    $("##{ elementId }").prop("disabled", false)

  retrieveNode = (id) ->
    $.get "node/#{ id }",
      (nodeData) ->
        incomingWaysLayer.clearLayers()
        addWaysToLayer(nodeData.incomingWays, incomingWaysLayer, styles.incomingWaysStyle)
        outgoingWaysLayer.clearLayers()
        addWaysToLayer(nodeData.outgoingWays, outgoingWaysLayer, styles.outgoingWaysStyle)

  retrieveDirections = (source, destination) ->
    $.get "/directions", {
      source: source.id
      destination: destination.id
    }, (directions) ->
      drawDirections(directions)

  setNodeAsDestination = () ->
    destination = selectedNode.node
    if not destinationMarker?
      destinationMarker ?= L.marker([destination.latitude, destination.longitude],
        icon: L.icon(
          iconAnchor: [14, 43]
          iconSize: [28, 43]
          iconUrl: "images/markers/green/B.png")).addTo(directionsLayer)
    else
      destinationMarker.setLatLng([destination.latitude, destination.longitude])
      destinationMarker.update()
    retrieveDirections(source, destination) if source? and destination?

  setNodeAsSource = () ->
    source = selectedNode.node
    if not sourceMarker?
      sourceMarker ?= L.marker([source.latitude, source.longitude],
        icon: L.icon(
          iconAnchor: [14, 43]
          iconSize: [28, 43]
          iconUrl: "images/markers/green/A.png")).addTo(directionsLayer)
    else
      sourceMarker.setLatLng([source.latitude, source.longitude])
      sourceMarker.update()
    retrieveDirections(source, destination) if source? and destination?

  showErrorMessage = (message) ->
    $("#error > p").html(message)
    $("#error").show().delay(5000).fadeOut("slow")
    $("#error > p").html()

  toggleDisabledProperty = (elementId) ->
    if $("##{ elementId }").prop("disabled") is true
      enable elementId
    else
      disable elementId

  toggleWayEditMaxSpeedControls = () ->
    toggleDisabledProperty("wayMaxSpeed")
    toggleDisabledProperty("wayEditMaxSpeed")
    toggleDisabledProperty("waySaveMaxSpeed")
    toggleDisabledProperty("wayCancelEditMaxSpeed")
)
