require(["jquery", "styles", "bootstrap", "leaflet-src", "underscore"], ($, styles) ->

  # globales AntScout-Object erstellen
  @AntScout = {}

  cloudMadeUrl = 'http://{s}.tile.cloudmade.com/{apiKey}/{styleId}/256/{z}/{x}/{y}.png'
  cloudMadeAttribution = 'Map data &copy; 2011 OpenStreetMap contributors, Imagery &copy; 2011 CloudMade';

  cloudMadeLayer = L.tileLayer(cloudMadeUrl,
    attribution: cloudMadeAttribution
    apiKey: "396d772f0ece43f49d3801843fd86fbc"
    styleId: 998
  )

  destination = null
  destinationMarker = null
  pathLayer = new L.LayerGroup()
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
  selectedNode = null
  selectedWay = null
  source = null
  sourceAndDestinationLayer = L.layerGroup()
  sourceMarker = null

  clearNodeData = () ->
    $("#nodeId").html("")
    $("#nodeLongitude").html("")
    $("#nodeLatitude").html("")

  clearWayData = () ->
    $("#wayId").html("")
    $("#wayLength").html("")
    $("#wayMaxSpeed").html("")
    $("#wayTripTime").html("")
    $("#way-nodes").html("")

  deselectNode = () ->
    if selectedNode?
      clearNodeData()
      incomingWaysLayer.clearLayers()
      outgoingWaysLayer.clearLayers()
      selectedNode.setRadius(styles.node.radius)
      selectedNode.setStyle(styles.node)
      selectedNode = null

  deselectWay = () ->
    if selectedWay?
      clearWayData()
      selectedWay.setStyle(selectedWay.style)
      selectedWay = null

  displayNodeData = (node) ->
    $("#nodeId").html("<a href=\"http://www.openstreetmap.org/browse/node/#{ node.id }\">#{ node.id }</a>")
    $("#nodeLongitude").html(node.longitude)
    $("#nodeLatitude").html(node.latitude)

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
        addWaysToLayer(ways, waysLayer, styles.way, styles.selectedWay)
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
      displayNodeData(selectedNode.node)
      retrieveNode(selectedNode.node.id)

  selectWay = (way) ->
    shouldSelect = !selectedWay? or way != selectedWay
    deselectWay()
    if shouldSelect
      selectedWay = way
      selectedWay.setStyle(selectedWay.selectedStyle)
      displayWayData(way.way)

  $(() ->
    map = L.map("map",
      layers: [pathLayer, nodesLayer, osmLayer, sourceAndDestinationLayer, waysLayer]
    ).fitWorld()
    baseLayers =
      "CloudMade": cloudMadeLayer
      "OpenStreetMap": osmLayer
    overlayLayers =
      "Incoming ways": incomingWaysLayer
      "Outgoing ways": outgoingWaysLayer
      "Ways": waysLayer
      "Path": pathLayer
      "Nodes": nodesLayer
      "Source and destination": sourceAndDestinationLayer
    L.control.layers(baseLayers, overlayLayers).addTo(map)
    L.control.scale().addTo(map)
    retrieveNodes()
    retrieveWays()
    $("#setNodeAsSource").click -> setNodeAsSource()
    $("#setNodeAsDestination").click -> setNodeAsDestination()
    $("#wayEditMaxSpeed, #waySaveMaxSpeed, #wayCancelEditMaxSpeed").click -> toggleWayEditMaxSpeedControls()
    $("#wayEditMaxSpeed").click -> $("#wayMaxSpeed").select()
    $("#waySaveMaxSpeed").click ->
      maxSpeed = parseFloat($("#wayMaxSpeedInput").val().replace(",", "."))
      $.ajax({
        contentType: "application/json"
        type: "PUT"
        url: "way/#{ selectedWay.way.id }"
        data: JSON.stringify(
          maxSpeed: maxSpeed
        )
      }).done (way) ->
        # in allen Weg-Layern den entsprechenden Weg aktualisieren
        _.each([incomingWaysLayer, outgoingWaysLayer, pathLayer, waysLayer], (layerGroup) ->
          layerGroup.eachLayer((layer) ->
            if layer.way.id is way.id
              layer.way = way
          )
        )
        # selektierten Weg aktualisieren
        selectWay.way = way
        # Daten des aktualisierten Weges anzeigen
        displayWayData(way)
    $("#pathLength, #pathTripTime, #wayLength, #wayMaxSpeed, #wayMaxSpeedInput, #wayTripTime").each(() ->
      $(this).tooltip({trigger: 'hover'})
    )
    $("#trace").click ->
      $this = $(this)
      if !$this.hasClass("active")
        $.ajax({
          contentType: "application/json"
          type: "PUT"
          url: "debug/trace"
        })
      else
        $.ajax({
          contentType: "application/json"
          type: "DELETE"
          url: "debug/trace"
        })
  )

  addWaysToLayer = (ways, layer, style, selectedStyle) ->
    for way in ways
      polyline = L.polyline((for node in way.nodes
        [node.latitude, node.longitude]), style)
      polyline.selectedStyle = selectedStyle
      polyline.style = style
      polyline.way = way
      polyline.on("click", (e) -> selectWay(e.target)).addTo(layer)

  disable = (elementId) ->
    $("##{ elementId }").prop("disabled", true)

  displayWayData = (way) ->
    $("#wayId").html(way.id)
    lengths = for length in way.lengths
      "#{ length.value } #{ length.unit }"
    $("#wayLength").attr("data-original-title", lengths.join("<br>")).html(way.length)
    maxSpeeds = for maxSpeed in way.maxSpeeds
      "#{ maxSpeed.value } #{ maxSpeed.unit }"
    $("#wayMaxSpeed").attr("data-original-title", maxSpeeds.join("<br>")).html(way.maxSpeed)
    tripTimes = for tripTime in way.tripTimes
      "#{ tripTime.value } #{ tripTime.unit }"
    $("#wayTripTime").attr("data-original-title", tripTimes.join("<br>")).html(way.tripTime)
    nodes = for node in way.nodes
      "<a href=\"http://www.openstreetmap.org/browse/node/#{ node.id }\">#{ node.id }</a>"
    $("#way-nodes").html("<ul><li>" + nodes.join("</li><li>") + "</li></ul>")

  drawPath = (path) ->
    pathLayer.clearLayers()
    if path and path.ways? and path.ways.length > 0
      lengths = for length in path.lengths
        "#{ length.value } #{ length.unit }"
      $("#pathLength").attr("data-original-title", lengths.join("<br>")).html(path.length)
      tripTimes = for tripTime in path.tripTimes
        "#{ tripTime.value } #{ tripTime.unit }"
      $("#pathTripTime").attr("data-original-title", tripTimes.join("<br>")).html(path.tripTime)
      addWaysToLayer(path.ways, pathLayer, styles.path, styles.selectedPath)

  AntScout.drawPath = (path) ->
    console.debug("Drawing path - path: " + JSON.stringify(path))
    drawPath(path)

  enable = (elementId) ->
    $("##{ elementId }").prop("disabled", false)

  retrieveNode = (id) ->
    $.get "node/#{ id }",
      (nodeData) ->
        incomingWaysLayer.clearLayers()
        addWaysToLayer(nodeData.incomingWays, incomingWaysLayer, styles.incomingWay, styles.selectedIncomingWay)
        outgoingWaysLayer.clearLayers()
        addWaysToLayer(nodeData.outgoingWays, outgoingWaysLayer, styles.outgoingWay, styles.selectedOutgoingWay)

  retrievePath = (source, destination) ->
    $.get "/path/#{ source.id }/#{ destination.id }",
      (path) ->
        drawPath(path)

  setNodeAsDestination = () ->
    destination = selectedNode.node
    if not destinationMarker?
      destinationMarker ?= L.marker([destination.latitude, destination.longitude],
        icon: L.icon(
          iconAnchor: [14, 43]
          iconSize: [28, 43]
          iconUrl: "images/markers/green/B.png")).addTo(sourceAndDestinationLayer)
    else
      destinationMarker.setLatLng([destination.latitude, destination.longitude])
      destinationMarker.update()
    retrievePath(source, destination) if source? and destination?

  setNodeAsSource = () ->
    source = selectedNode.node
    if not sourceMarker?
      sourceMarker ?= L.marker([source.latitude, source.longitude],
        icon: L.icon(
          iconAnchor: [14, 43]
          iconSize: [28, 43]
          iconUrl: "images/markers/green/A.png")).addTo(sourceAndDestinationLayer)
    else
      sourceMarker.setLatLng([source.latitude, source.longitude])
      sourceMarker.update()
    retrievePath(source, destination) if source? and destination?

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
    if $("#wayMaxSpeed").is(":visible")
      $("#wayMaxSpeed").hide()
      $("#wayMaxSpeedInput")
        .attr("data-original-title", $("#wayMaxSpeed").attr("data-original-title"))
        .show()
        .val($("#wayMaxSpeed").html())
        .select()
    else
      $("#wayMaxSpeedInput").hide()
      $("#wayMaxSpeed").show()
    toggleDisabledProperty("wayEditMaxSpeed")
    toggleDisabledProperty("waySaveMaxSpeed")
    toggleDisabledProperty("wayCancelEditMaxSpeed")
)
