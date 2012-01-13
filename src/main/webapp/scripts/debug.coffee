$(() ->

  map = new OpenLayers.Map("map");
  osmLayer = new OpenLayers.Layer.OSM();
  antNodes = null
  antNodesLayer = new OpenLayers.Layer.Markers("Ant nodes", {opacity: 0.5, visibility: false});
  osmNodes = null
  osmNodesLayer = new OpenLayers.Layer.Markers("OSM nodes", {opacity: 0.5, visibility: false});

  map.addControl(new OpenLayers.Control.LayerSwitcher());
  map.addLayers([antNodesLayer, osmLayer, osmNodesLayer]);
  map.zoomToMaxExtent()

  addMarkers = (iconName, size, offset, nodes, nodesLayer) ->
    icon = new OpenLayers.Icon("/scripts/openlayers/img/" + iconName + ".png", size, offset)
    epsg4326Projection = new OpenLayers.Projection("EPSG:4326");
    for node in nodes
      lonLat = new OpenLayers.LonLat(node.longitude, node.latitude).transform(epsg4326Projection, map.getProjectionObject())
      nodesLayer.addMarker(new OpenLayers.Marker(lonLat, icon.clone()))
    map.zoomToExtent(nodesLayer.getDataExtent())

  initMap = () ->

  map.events.on({
    "changelayer": (event) ->
      if shouldRetrieveNodes(event, "Ant nodes", antNodes)
        antNodes = retrieveNodes("/antnodes", antNodes, memorizeAntNodesAndAddAntNodeMarkers)
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
    $.ajax({
      url: url,
      success: (ns) ->
        successFunction(ns)
    })

  shouldRetrieveNodes = (event, layerName, nodes) ->
    event.layer.name is layerName and event.property is "visibility" and event.layer.visibility and not nodes?
)
