$(() ->

  map = new OpenLayers.Map "map"
  osmLayer = new OpenLayers.Layer.OSM()
  nodes = null
  nodesLayer = new OpenLayers.Layer.Vector(
    "Nodes"
    {
      opacity: 0.5
      visibility: false
    }
  )
  nodesLayer.events.on({
    "featureselected": (e) ->
      alert(e.feature.attributes.node.id)
    "visibilitychanged": (e) ->
      if nodesLayer.getVisibility() and not nodes?
        retrieveNodes()
  })
  directionsLayer = new OpenLayers.Layer.Vector(
    "Directions"
    renderers: ["Canvas", "SVG", "VML"]
  )
  EPSG4326Projection = new OpenLayers.Projection("EPSG:4326")
  selectFeatureControl = new OpenLayers.Control.SelectFeature(nodesLayer);

  map.addControls([new OpenLayers.Control.LayerSwitcher(), selectFeatureControl]);
  map.addLayers([osmLayer, nodesLayer, directionsLayer]);
  selectFeatureControl.activate();
  map.zoomToMaxExtent()

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

  retrieveNodes = (successFunction) ->
    $.get "nodes",
      (ns) ->
        nodes = ns
        features = for node in nodes
          (() ->
            point = new OpenLayers.Geometry.Point(node.longitude, node.latitude)
              .transform(EPSG4326Projection, map.getProjectionObject())
            feature = new OpenLayers.Feature.Vector(
              point
              {
                node: node
              }
            ))()
        nodesLayer.addFeatures(features)
        map.zoomToExtent(nodesLayer.getDataExtent())

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
