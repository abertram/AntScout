style =
  fillOpacity: 0.5
  pointRadius: 6
  strokeOpacity: 0.5
  strokeWidth:5
directionsStyle = $.extend({}, style,
  fillColor: "lightblue"
  strokeColor: "blue"
)
incomingWaysStyle = $.extend({}, style,
  fillColor: "lightgreen"
  strokeColor: "green"
)
nodesStyle = $.extend({}, style,
  fillColor: "lightblue"
  strokeColor: "blue"
)
outgoingWaysStyle = $.extend({}, style,
  fillColor: "lightcoral"
  strokeColor: "red"
)
waysStyle = $.extend({}, style,
  fillColor: "lightblue"
  strokeColor: "blue"
)

directionsLayer = new OpenLayers.Layer.Vector(
  "Directions"
  {
    renderers: ["Canvas", "SVG", "VML"]
    style: directionsStyle
  }
)
EPSG4326Projection = new OpenLayers.Projection("EPSG:4326")
incomingWaysLayer = new OpenLayers.Layer.Vector(
  "Incoming ways"
  {
    style: incomingWaysStyle
    visibility: false
  }
)
map = null
nodes = null
nodesLayer = new OpenLayers.Layer.Vector(
  "Nodes"
  {
    opacity: 0.5
    style: nodesStyle
    visibility: false
  }
)
nodesLayer.events.on({
  "featureselected": (e) ->
    retrieveNode(e.feature.attributes.node.id)
    alert(e.feature.attributes.node.id)
  "visibilitychanged": (e) ->
    if nodesLayer.getVisibility() and not nodes?
      retrieveNodes()
})
osmLayer = new OpenLayers.Layer.OSM()
outgoingWaysLayer = new OpenLayers.Layer.Vector(
  "Outgoing ways"
  {
    style: outgoingWaysStyle
    visibility: false
  }
)
selectFeatureControl = new OpenLayers.Control.SelectFeature(nodesLayer);
ways = null
waysLayer = new OpenLayers.Layer.Vector(
  "Ways"
  {
    style: waysStyle
    visibility: false
  }
)
waysLayer.events.on({
"visibilitychanged": (e) ->
  if waysLayer.getVisibility() and not ways?
    retrieveWays()
})

$(() ->
  map = new OpenLayers.Map "map"
  map.addControls([new OpenLayers.Control.LayerSwitcher(), selectFeatureControl]);
  map.addLayers([directionsLayer, incomingWaysLayer, nodesLayer, osmLayer, outgoingWaysLayer, waysLayer]);
  selectFeatureControl.activate();
  map.zoomToMaxExtent()


  $("#retrieveDirections").click(retrieveDirections)
)

addWaysToLayer = (ways, layer) ->
  lines = for way in ways
    points = for node in way.nodes
      new OpenLayers.Geometry.Point(node.longitude, node.latitude)
        .transform(EPSG4326Projection, map.getProjectionObject())
    new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString(points))
  layer.addFeatures(lines)

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
  map.zoomToExtent(directionsLayer.getDataExtent())

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

retrieveWays = () ->
  $.get "ways",
    (ws) ->
      ways = ws
      addWaysToLayer(ways, waysLayer)
      map.zoomToExtent(waysLayer.getDataExtent())
