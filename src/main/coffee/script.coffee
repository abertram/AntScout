require(["jquery", "styles", "openlayers/OpenLayers", "underscore"], ($, styles) ->

  # globales AntScout-Object erstellen
  @AntScout = {}
  destination = null
  directionsLayer = new OpenLayers.Layer.Vector(
    "Directions"
    {
      renderers: ["Canvas", "SVG", "VML"]
      style: styles.directionsDefaultStyle
    }
  )
  EPSG4326Projection = new OpenLayers.Projection("EPSG:4326")
  incomingWaysLayer = new OpenLayers.Layer.Vector(
    "Incoming ways"
    {
      style: styles.incomingWaysStyle
      visibility: false
    }
  )
  map = null
  nodes = null
  nodesLayer = new OpenLayers.Layer.Vector(
    "Nodes"
    {
      styleMap: new OpenLayers.StyleMap(
        default: styles.nodesDefaultStyle
        select: styles.nodesSelectStyle
      )
      visibility: false
    }
  )
  nodesLayer.events.on({
    "featureselected": (e) ->
      id = e.feature.attributes.node.id
      $("#nodeId").html("<a href=\"http://www.openstreetmap.org/browse/node/#{ id }\">#{ id }</a>")
      $("#nodeLongitude").html(e.feature.attributes.node.longitude)
      $("#nodeLatitude").html(e.feature.attributes.node.latitude)
      retrieveNode(id)
    "visibilitychanged": (e) ->
      if nodesLayer.getVisibility() and not nodes?
        retrieveNodes()
  })
  osmLayer = new OpenLayers.Layer.OSM()
  outgoingWaysLayer = new OpenLayers.Layer.Vector(
    "Outgoing ways"
    {
      style: styles.outgoingWaysStyle
      visibility: false
    }
  )
  ways = null
  waysLayer = new OpenLayers.Layer.Vector(
    "Ways"
    {
      styleMap: new OpenLayers.StyleMap(
        default: styles.waysDefaultStyle
        select: styles.waysSelectStyle
      )
      visibility: false
    }
  )
  waysLayer.events.on({
    "featureselected": (e) ->
      displayWayData(e.feature.attributes.way)
    "visibilitychanged": (e) ->
      if waysLayer.getVisibility() and waysLayer.features.length is 0
        retrieveWays()
  })
  selectFeatureControl = new OpenLayers.Control.SelectFeature([nodesLayer, waysLayer]);
  source = null

  $(() ->
    map = new OpenLayers.Map "map"
    map.addControls([new OpenLayers.Control.LayerSwitcher(), selectFeatureControl]);
    map.addLayers([incomingWaysLayer, osmLayer, outgoingWaysLayer, waysLayer, directionsLayer, nodesLayer]);
    selectFeatureControl.activate();
    map.zoomToMaxExtent()
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

  displayDirections = (directions) ->
    $("#directions").html("<ol><li>" + directions.join("</li><li>") + "</li></ol>")

  displayWayData = (way) ->
    id = way.id
    $("#wayId").html(id)
    $("#wayLength").val(way.length)
    $("#wayMaxSpeed").val(way.maxSpeed)
    $("#wayTripTime").val(way.tripTime)
    nodes = for node in way.nodes
      "<a href=\"http://www.openstreetmap.org/browse/node/#{ node.id }\">#{ node.id }</a>"
    $("#wayNodes").html("<ul><li>" + nodes.join("</li><li>") + "</li></ul>")

  drawDirections = (directions) ->
    directionsLayer.removeAllFeatures()
    sourceNode = directions[0].nodes[0]
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
    addWaysToLayer(directions, directionsLayer)
    lastDirection = directions[directions.length - 1]
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

  AntScout.updatePath = (path) ->
    console.debug("Updating path")
    drawDirections

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
      },
      (directions) ->
        displayDirections(directions)
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
