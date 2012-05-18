$(() ->

  map = new OpenLayers.Map "map"
  osmLayer = new OpenLayers.Layer.OSM()
  antNodes = null
  antNodesLayer = new OpenLayers.Layer.Markers("Ant nodes", {opacity: 0.5, visibility: false})
  directionsLayer = new OpenLayers.Layer.Vector("Directions",
    renderers: ["Canvas", "SVG", "VML"]
  )
  EPSG4326Projection = new OpenLayers.Projection("EPSG:4326")
  nodesLayer = new OpenLayers.Layer.Vector("Nodes")
  osmNodes = null
  osmNodesLayer = new OpenLayers.Layer.Markers("OSM nodes", {opacity: 0.5, visibility: false});

  map.addControl(new OpenLayers.Control.LayerSwitcher());
  map.addLayers([osmLayer, antNodesLayer, osmNodesLayer, directionsLayer]);
  map.zoomToMaxExtent()

  addMarkers = (iconName, size, offset, nodes, nodesLayer) ->
    icon = new OpenLayers.Icon("/scripts/openlayers/img/" + iconName + ".png", size, offset)
    for node in nodes
      (() ->
        lonLat = new OpenLayers.LonLat(node.longitude, node.latitude)
          .transform(EPSG4326Projection, map.getProjectionObject())
        marker = new OpenLayers.Marker(lonLat, icon.clone())
        marker.events.register("mousedown", node, (e) ->
          alert(this.id)
          OpenLayers.Event.stop(e);
        )
        nodesLayer.addMarker(marker))()
    map.zoomToExtent(nodesLayer.getDataExtent())

  displayDirections = (directions) ->
    $("#directions").html("<ol><li>" + directions.join("</li><li>") + "</li></ol>")

  drawDirections = (directions) ->
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
    lines = for direction in directions
      points = for node in direction.nodes
        new OpenLayers.Geometry.Point(node.longitude, node.latitude)
          .transform(EPSG4326Projection, map.getProjectionObject())
      new OpenLayers.Feature.Vector(
        new OpenLayers.Geometry.LineString(points)
        null
        {
          fillColor: "lightblue"
          fillOpacity: 0.5
          strokeColor: "blue"
          strokeOpacity: 0.5
          strokeWidth: 10
        }
      )
    directionsLayer.addFeatures(lines)
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
    map.zoomToExtent(directionsLayer.getDataExtent())

  map.events.on({
    "changelayer": (event) ->
      if shouldRetrieveNodes(event, "Ant nodes", antNodes)
        retrieveNodes("/antnodes", antNodes, memorizeAntNodesAndAddAntNodeMarkers)
      else if shouldRetrieveNodes(event, "OSM nodes", osmNodes)
        retrieveNodes("/osmnodes", osmNodes, memorizeOsmNodesAndAddOsmNodeMarkers)
  })

  memorizeAntNodesAndAddAntNodeMarkers = (nodes) ->
    antNodes = nodes
    size = new OpenLayers.Size(21, 25)
    offset = new OpenLayers.Pixel(-(size.w / 2) - 3, -size.h - 3)
    addMarkers("marker-blue", size, offset, antNodes, antNodesLayer)

  memorizeOsmNodesAndAddOsmNodeMarkers = (nodes) ->
    osmNodes = nodes
    size = new OpenLayers.Size(21, 25)
    offset = new OpenLayers.Pixel(-(size.w / 2), -size.h)
    addMarkers("marker-green", size, offset, osmNodes, osmNodesLayer)

  retrieveNodes = (url, nodes, successFunction) ->
    $.get  url,
      (ns) ->
        successFunction(ns)

  shouldRetrieveNodes = (event, layerName, nodes) ->
    event.layer.name is layerName and event.property is "visibility" and event.layer.visibility and not nodes?

  retrieveDirections = () ->
    destination = $("#destination").val()
    source = $("#source").val()
    $.get "/directions", {
      destination
      source
      },
      (directions) ->
        displayDirections(directions)
        drawDirections(directions)

  $("#retrieveDirections").click(retrieveDirections)
)
