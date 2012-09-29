require(["jquery", "styles", "bootstrap", "leaflet", "openlayers/OpenLayers", "underscore"], ($, styles) ->

  # globales AntScout-Object erstellen
  @AntScout = {}

  cloudmadeUrl = 'http://{s}.tile.cloudmade.com/{apiKey}/{styleId}/256/{z}/{x}/{y}.png'
  cloudmadeAttribution = 'Map data &copy; 2011 OpenStreetMap contributors, Imagery &copy; 2011 CloudMade';

  destination = null
  directionsLayer = L.tileLayer(cloudmadeUrl,
    attribution: cloudmadeAttribution
    apiKey: "396d772f0ece43f49d3801843fd86fbc"
    styleId: 997
  )
#    "Directions"
#    {
#      renderers: ["Canvas", "SVG", "VML"]
#      style: styles.directionsDefaultStyle
#    }
#  )
  EPSG4326Projection = new OpenLayers.Projection("EPSG:4326")
  incomingWaysLayer = L.tileLayer(cloudmadeUrl,
    attribution: cloudmadeAttribution
    apiKey: "396d772f0ece43f49d3801843fd86fbc"
    styleId: 997
  )
#    "Incoming ways"
#    {
#      style: styles.incomingWaysStyle
#      visibility: false
#    }
#  )
  map = null
  nodes = null
  nodesLayer = L.tileLayer(cloudmadeUrl,
    attribution: cloudmadeAttribution
    apiKey: "396d772f0ece43f49d3801843fd86fbc"
    styleId: 997
  )
#    new OpenLayers.Layer.Vector(
#    "Nodes"
#    {
#      styleMap: new OpenLayers.StyleMap(
#        default: styles.nodesDefaultStyle
#        select: styles.nodesSelectStyle
#      )
#      visibility: false
#    }
#  )
#  nodesLayer.events.on({
#    "featureselected": (e) ->
#      id = e.feature.attributes.node.id
#      $("#nodeId").html("<a href=\"http://www.openstreetmap.org/browse/node/#{ id }\">#{ id }</a>")
#      $("#nodeLongitude").html(e.feature.attributes.node.longitude)
#      $("#nodeLatitude").html(e.feature.attributes.node.latitude)
#      retrieveNode(id)
#    "visibilitychanged": (e) ->
#      if nodesLayer.getVisibility() and not nodes?
#        retrieveNodes()
#  })
#  osmLayer = new OpenLayers.Layer.OSM()
  outgoingWaysLayer = L.tileLayer(cloudmadeUrl,
    attribution: cloudmadeAttribution
    apiKey: "396d772f0ece43f49d3801843fd86fbc"
    styleId: 997
  )
#    "Outgoing ways"
#    {
#      style: styles.outgoingWaysStyle
#      visibility: false
#    }
#  )
  ways = null
  waysLayer = L.tileLayer(cloudmadeUrl,
    attribution: cloudmadeAttribution
    apiKey: "396d772f0ece43f49d3801843fd86fbc"
    styleId: 997
  )
#      "Ways"
#    {
#      styleMap: new OpenLayers.StyleMap(
#        default: styles.waysDefaultStyle
#        select: styles.waysSelectStyle
#      )
#      visibility: false
#    }
#  )
#  waysLayer.events.on({
#    "featureselected": (e) ->
#      displayWayData(e.feature.attributes.way)
#    "visibilitychanged": (e) ->
#      if waysLayer.getVisibility() and waysLayer.features.length is 0
#        retrieveWays()
#  })
  selectFeatureControl = new OpenLayers.Control.SelectFeature([nodesLayer, waysLayer]);
  source = null

  $(() ->
    map = L.map('map',
      layers: [incomingWaysLayer, outgoingWaysLayer, waysLayer, directionsLayer, nodesLayer]
    ).fitWorld()
    L.control.layers({},
      "Incoming ways": incomingWaysLayer
      "Outgoing ways": outgoingWaysLayer
      "Ways": waysLayer
      "Directions": directionsLayer
      "Nodes": nodesLayer
    ).addTo(map)
#    map.addControls([new OpenLayers.Control.LayerSwitcher(), selectFeatureControl]);
#    map.addLayers([incomingWaysLayer, osmLayer, outgoingWaysLayer, waysLayer, directionsLayer, nodesLayer]);
#    selectFeatureControl.activate();
#    map.zoomToMaxExtent()
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

  addWaysToLayer = (ways, layer) ->
    lines = for way in ways
      points = for node in way.nodes
        new OpenLayers.Geometry.Point(node.longitude, node.latitude)
          .transform(EPSG4326Projection, map.getProjectionObject())
      new OpenLayers.Feature.Vector(
        new OpenLayers.Geometry.LineString(points)
        {
          way: way
        }
      )
    layer.addFeatures(lines)

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
    directionsLayer.removeAllFeatures()
    if directions? and directions.ways.length > 0
      $("#pathLength").html(directions.length)
      $("#pathTripTime").html(directions.tripTime)
      sourceNode = directions.ways[0].nodes[0]
      sourcePoint = new OpenLayers.Geometry.Point(sourceNode.longitude, sourceNode.latitude)
        .transform(EPSG4326Projection, map.getProjectionObject())
      sourceFeature = new OpenLayers.Feature.Vector(
        sourcePoint
        null
        {
          fillColor: "green"
          pointRadius: 10
          strokeColor: "green"
        }
      )
      lastDirection = directions.ways[directions.ways.length - 1]
      targetNode = lastDirection.nodes[lastDirection.nodes.length - 1]
      targetPoint = new OpenLayers.Geometry.Point(targetNode.longitude, targetNode.latitude)
        .transform(EPSG4326Projection, map.getProjectionObject())
      targetFeature = new OpenLayers.Feature.Vector(
        targetPoint
        null
        {
          fillColor: "red"
          pointRadius: 10
          strokeColor: "red"
        }
      )
      directionsLayer.addFeatures([sourceFeature, targetFeature])
    addWaysToLayer(directions.ways, directionsLayer)

  AntScout.drawPath = (path) ->
    console.debug("Drawing path - path: " + path)
    drawDirections(path)

  enable = (elementId) ->
    $("##{ elementId }").prop("disabled", false)

  selectedNodeId = () ->
    nodesLayer.selectedFeatures.length > 0 and nodesLayer.selectedFeatures[0].attributes.node.id

  retrieveNode = (id) ->
    $.get "node/#{ id }",
      (nodeData) ->
        incomingWaysLayer.removeAllFeatures()
        addWaysToLayer(nodeData.incomingWays, incomingWaysLayer)
        outgoingWaysLayer.removeAllFeatures()
        addWaysToLayer(nodeData.outgoingWays, outgoingWaysLayer)

  retrieveNodes = () ->
    $.get "nodes",
      (ns) ->
        nodes = ns
        features = for node in nodes
          do ->
            point = new OpenLayers.Geometry.Point(node.longitude, node.latitude)
              .transform(EPSG4326Projection, map.getProjectionObject())
            feature = new OpenLayers.Feature.Vector(
              point
              {
                node: node
              }
            )
        nodesLayer.addFeatures(features)
        map.zoomToExtent(nodesLayer.getDataExtent())

  retrieveDirections = (source, destination) ->
    $.get "/directions", {
      source
      destination
      error: directionsLayer.removeAllFeatures()
      },
      (directions) ->
        drawDirections(directions)

  retrieveWays = () ->
    $.get "ways",
      (ways) ->
        addWaysToLayer(ways, waysLayer)
        map.zoomToExtent(waysLayer.getDataExtent())

  setNodeAsDestination = () ->
    destination = selectedNodeId()
    retrieveDirections(source, destination) if source? and destination?

  setNodeAsSource = () ->
    source = selectedNodeId()
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
