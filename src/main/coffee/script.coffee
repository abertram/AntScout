# Verwaltet das Front-End.
require(["jquery", "styles", "bootstrap", "leaflet-src", "underscore"], ($, styles) ->

  # Globales AntScout-Objekt. Wird benötigt, um zur Laufzeit JavaScript-Funktionen aus dem Backend per Comet aufzurufen.
  @AntScout = {}

  # URL zum Holen der CloudMade-Kacheln
  cloudMadeUrl = 'http://{s}.tile.cloudmade.com/{apiKey}/{styleId}/256/{z}/{x}/{y}.png'
  # Text, der rechts unten auf der Karte angezeigt wird.
  cloudMadeAttribution = 'Map data &copy; 2011 OpenStreetMap contributors, Imagery &copy; 2011 CloudMade';
  # CloudMade-Layer
  cloudMadeLayer = L.tileLayer(cloudMadeUrl,
    attribution: cloudMadeAttribution
    apiKey: "396d772f0ece43f49d3801843fd86fbc"
    styleId: 998
  )
  # Aktueller Ziel-Knoten
  destination = null
  # Aktueller Ziel-Knoten-Marker
  destinationMarker = null
  # Pfad-Layer
  pathLayer = new L.LayerGroup()
  # Layer zur Anzeige der einkommenden Wege
  incomingWaysLayer = new L.LayerGroup()
  # Karte
  map = null
  # Knoten
  nodes = null
  # Layer zur Anzeige der Knoten
  nodesLayer = new L.LayerGroup()
  # OpenStreetMap-Layer
  osmLayer = L.tileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    attribution: cloudMadeAttribution
  )
  # Layer zum Anzeigen der ausgehenden Wege
  outgoingWaysLayer = new L.LayerGroup()
  # Wege
  ways = null
  # Layer zum Anzeigen der Wege
  waysLayer = new L.LayerGroup()
  # aktuell selektierter Knoten
  selectedNode = null
  # aktuell selektierter Weg
  selectedWay = null
  # aktueller Quell-Knoten
  source = null
  # Layer zum Anzeigen des Quell- und Ziel-Knotens
  sourceAndDestinationLayer = L.layerGroup()
  # Quell-Knoten-Marker
  sourceMarker = null

  # Löscht die angezeigten Knoten-Daten.
  clearNodeData = () ->
    $("#nodeId").html("")
    $("#nodeLongitude").html("")
    $("#nodeLatitude").html("")

  # Löscht die angezeigten Weg-Daten.
  clearWayData = () ->
    $("#wayId").html("")
    $("#wayLength").html("")
    $("#wayMaxSpeed").html("")
    $("#wayTripTime").html("")
    $("#way-nodes").html("")

  # Deselektiert einen Knoten.
  deselectNode = () ->
    # Knoten selektiert?
    if selectedNode?
      # Knoten-Daten löschen
      clearNodeData()
      # Weg-Layer zurücksetzen
      incomingWaysLayer.clearLayers()
      outgoingWaysLayer.clearLayers()
      # Radius zurücksetzen
      selectedNode.setRadius(styles.node.radius)
      selectedNode.setStyle(styles.node)
      # Selektierten Knoten löschen
      selectedNode = null

  # Deselektiert einen Weg.
  deselectWay = () ->
    # Weg selektiert?
    if selectedWay?
      # Angezeigte Weg-Daten löschen
      clearWayData()
      # Stile zurücksetzen
      selectedWay.setStyle(selectedWay.style)
      # Selektierten Weg löschen
      selectedWay = null

  # Zeigt Knoten-Daten an.
  displayNodeData = (node) ->
    $("#nodeId").html("<a href=\"http://www.openstreetmap.org/browse/node/#{ node.id }\">#{ node.id }</a>")
    $("#nodeLongitude").html(node.longitude)
    $("#nodeLatitude").html(node.latitude)

  # Ruft Knoten aus dem Back-End ab und zeigt diese als Kreise auf der Karte an.
  retrieveNodes = () ->
    # Ajax-GET-Request an das Back-End
    $.get "nodes",
      (nodes) ->
        # Knoten merken
        this.nodes = this
        # Über die Knoten iterieren
        for node in nodes
          # Marker erstellen
          marker = L.circleMarker([node.latitude, node.longitude], styles.node)
          # Marker um aktuellen Knoten erweitern
          marker.node = node
          # Marker zum Knoten-Layer hinzufügen
          marker.addTo(nodesLayer)
          # Beim Klick auf den Marker den im Marker gespeicherten Knoten selektieren
          marker.on("click", (e) -> selectNode(e.target))
        # Aktuelle Karten-Ansicht an die Knoten anpassen
        map.fitBounds(for node in nodes
          [node.latitude, node.longitude])

  # Ruft Wege aus dem Back-End ab und zeigt diese als Linien auf der Karte an.
  retrieveWays = () ->
    # Ajax-GET-Request an das Back-End
    $.get "ways",
      (ways) ->
        # Wege zum Wege-Layer hinzufügen
        addWaysToLayer(ways, waysLayer, styles.way, styles.selectedWay)
        # Aktuelle Karten-Ansicht an die Wege anpassen
        map.fitBounds(for way in ways
          for node in way.nodes
            [node.latitude, node.longitude]
        )

  # Selektiert einen Knoten und hebt diesen optisch hervor.
  selectNode = (nodePath) ->
    # Soll ein neuer Knoten selektiert werden?
    shouldSelect = !selectedNode? or nodePath != selectedNode
    # Aktuellen Knoten deselektieren
    deselectNode()
    # Wenn ein neuer Knoten selektiert werden soll
    if shouldSelect
      # Stile setzen
      nodePath.setRadius(styles.selectedNode.radius)
      nodePath.setStyle(styles.selectedNode)
      # Aktuell selektierten Knoten setzen
      selectedNode = nodePath
      # Knoten-Daten anzeigen
      displayNodeData(selectedNode.node)
      # Weitere Knoten-Daten abrufen
      retrieveNode(selectedNode.node.id)

  # Selektiert einen Weg und hebt diesen optisch hervor.
  selectWay = (way) ->
    # Soll ein neuer Weg selektiert werden?
    shouldSelect = !selectedWay? or way != selectedWay
    # Aktuellen Weg deselektieren
    deselectWay()
    # Wenn ein neuer Weg selektiert werden soll
    if shouldSelect
      # Aktuell selektierten Weg setzen
      selectedWay = way
      # Stile setzen
      selectedWay.setStyle(selectedWay.selectedStyle)
      # Weg-Daten anzeigen
      displayWayData(way.way)

  # Wird ausgeführt, wenn die Seite vollständig geladen wurde.
  $(() ->
    # Karte mit den am Anfang sichtbaren Layern erzeugen
    map = L.map("map",
      layers: [pathLayer, nodesLayer, osmLayer, sourceAndDestinationLayer, waysLayer]
    ).fitWorld()
    # Basis-Layer definieren
    baseLayers =
      "CloudMade": cloudMadeLayer
      "OpenStreetMap": osmLayer
    # Overlay-Layer definieren
    overlayLayers =
      "Incoming ways": incomingWaysLayer
      "Outgoing ways": outgoingWaysLayer
      "Ways": waysLayer
      "Path": pathLayer
      "Nodes": nodesLayer
      "Source and destination": sourceAndDestinationLayer
    # Basis- und Overlay-Layer zur Karte hinzufügen
    L.control.layers(baseLayers, overlayLayers).addTo(map)
    # Skalierungs-Control zur Karte hinzufügen
    L.control.scale().addTo(map)
    # Knoten abrufen
    retrieveNodes()
    # Wege abrufen
    retrieveWays()
    # Aktuell selektierten Knoten als Quelle setzen
    $("#setNodeAsSource").click -> setNodeAsSource()
    # Aktuell selektierten Knoten als Ziel setzen
    $("#setNodeAsDestination").click -> setNodeAsDestination()
    # Weg-Geschwindigkeit-Veränderungs-Controls ein- und ausschalten
    $("#wayEditMaxSpeed, #waySaveMaxSpeed, #wayCancelEditMaxSpeed").click -> toggleWayEditMaxSpeedControls()
    # Fokus in das Weg-Geschwindigkeit-Input-Feld setzen
    $("#wayEditMaxSpeed").click -> $("#wayMaxSpeed").select()
    # Speichert die veränderte Weg-Geschwindigkeit
    $("#waySaveMaxSpeed").click ->
      # Versuchen, den eingegebenen Wert in eine Gleit-Komma-Zahl umzuwandeln
      maxSpeed = parseFloat($("#wayMaxSpeedInput").val().replace(",", "."))
      # PUT-Ajax-Request an das Back-End
      $.ajax({
        contentType: "application/json"
        type: "PUT"
        url: "way/#{ selectedWay.way.id }"
        data: JSON.stringify(
          maxSpeed: maxSpeed
        )
      }).done (way) ->
        # In allen Weg-Layern den entsprechenden Weg aktualisieren
        _.each([incomingWaysLayer, outgoingWaysLayer, pathLayer, waysLayer], (layerGroup) ->
          layerGroup.eachLayer((layer) ->
            if layer.way.id is way.id
              layer.way = way
          )
        )
        # Aktuelle selektierten Weg aktualisieren
        selectWay.way = way
        # Daten des aktualisierten Weges anzeigen
        displayWayData(way)
    # Tool-Tips auf diesen Controls beim Überfahren mit der Maus anzeigen
    $("#pathLength, #pathTripTime, #wayLength, #wayMaxSpeed, #wayMaxSpeedInput, #wayTripTime").each(() ->
      $(this).tooltip({trigger: 'hover'})
    )
    # Schaltet den Trace-Modus im Back-End ein und aus
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
    # Zusätzlichen Knoten-Daten anzeigen
    $("#show-node-additional-data").click -> $("#node-additional-data").collapse("toggle")
    # Zusätzlichen Pfad-Daten anzeigen
    $("#show-path-additional-data").click -> $("#path-additional-data").collapse("toggle")
  )

  # Fügt Wege zu einem Layer hinzu.
  addWaysToLayer = (ways, layer, style, selectedStyle) ->
    # Über die Wege iterieren
    for way in ways
      # Linie erzeugen
      polyline = L.polyline((for node in way.nodes
        [node.latitude, node.longitude]), style)
      # Stil für selektierte Wege setzen
      polyline.selectedStyle = selectedStyle
      # Standard-Stil setzen
      polyline.style = style
      # Linie um den aktuellen Weg erweitern
      polyline.way = way
      # Linie zum Layer hinzufügen
      polyline.addTo(layer)
      # Beim Selektieren den Stil umschalten
      polyline.on("click", (e) -> selectWay(e.target))

  # Deaktiviert ein Html-Element.
  disable = (elementId) ->
    $("##{ elementId }").prop("disabled", true)

  # Zeigt Weg-Daten an.
  displayWayData = (way) ->
    # Id
    $("#wayId").html(way.id)
    # Länge
    lengths = for length in way.lengths
      "#{ length.value } #{ length.unit }"
    $("#wayLength").attr("data-original-title", lengths.join("<br>")).html(way.length)
    # Maximale Geschwindkigkeit
    maxSpeeds = for maxSpeed in way.maxSpeeds
      "#{ maxSpeed.value } #{ maxSpeed.unit }"
    $("#wayMaxSpeed").attr("data-original-title", maxSpeeds.join("<br>")).html(way.maxSpeed)
    # Passier-Zeit
    tripTimes = for tripTime in way.tripTimes
      "#{ tripTime.value } #{ tripTime.unit }"
    $("#wayTripTime").attr("data-original-title", tripTimes.join("<br>")).html(way.tripTime)
    # Knoten
    nodes = for node in way.nodes
      "<a href=\"http://www.openstreetmap.org/browse/node/#{ node.id }\">#{ node.id }</a>"
    $("#way-nodes").html("<ul><li>" + nodes.join("</li><li>") + "</li></ul>")

  # Zeichnet einen Pfad auf der Karte.
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

  # Schnittstelle zum Back-End. Verarbeitet den Pfad.
  AntScout.path = (path) ->
    # console? && console.debug("Drawing path - path: " + JSON.stringify(path))
    drawPath(path)

  # Aktiviert ein Html-Element.
  enable = (elementId) ->
    $("##{ elementId }").prop("disabled", false)

  # Ruft einen Knoten aus dem Back-End ab.
  retrieveNode = (id) ->
    # Ajax-GET-Request
    $.get "node/#{ id }",
      (nodeData) ->
        # Einkommende und ausgehende Wege löschen und die abgerufenen hinzufügen
        incomingWaysLayer.clearLayers()
        addWaysToLayer(nodeData.incomingWays, incomingWaysLayer, styles.incomingWay, styles.selectedIncomingWay)
        outgoingWaysLayer.clearLayers()
        addWaysToLayer(nodeData.outgoingWays, outgoingWaysLayer, styles.outgoingWay, styles.selectedOutgoingWay)

  # Ruft einen Pfad von einem Quell- zu einem Ziel-Knoten aus dem Back-End ab.
  retrievePath = (source, destination) ->
    # Ajax-GET-REquest
    $.get "/path/#{ source.id }/#{ destination.id }",
      (path) ->
        # Pfad zeichnen
        drawPath(path)

  # Setzt den aktuell selektierten Knoten als Ziel.
  setNodeAsDestination = () ->
    # Knoten extrahieren
    destination = selectedNode.node
    # Existiert bereits ein Marker?
    if not destinationMarker?
      # Marker erzeugen
      destinationMarker ?= L.marker([destination.latitude, destination.longitude],
        icon: L.icon(
          iconAnchor: [14, 43]
          iconSize: [28, 43]
          iconUrl: "images/markers/green/B.png")).addTo(sourceAndDestinationLayer)
    else
      # Marker aktualisieren
      destinationMarker.setLatLng([destination.latitude, destination.longitude])
      destinationMarker.update()
    # Pfad abrufen, wenn nötig
    retrievePath(source, destination) if source? and destination?

  # Setzt den aktuell selektierten Knoten als Quelle.
  setNodeAsSource = () ->
    # Knoten extrahieren
    source = selectedNode.node
    # Existiert bereits ein Marker?
    if not sourceMarker?
      # Marker erzeugen
      sourceMarker ?= L.marker([source.latitude, source.longitude],
        icon: L.icon(
          iconAnchor: [14, 43]
          iconSize: [28, 43]
          iconUrl: "images/markers/green/A.png")).addTo(sourceAndDestinationLayer)
    else
      # Marker aktualisieren
      sourceMarker.setLatLng([source.latitude, source.longitude])
      sourceMarker.update()
    # Pfad abrufen, wenn nötig
    retrievePath(source, destination) if source? and destination?

  # Zeigt eine Fehler-Meldung.
  showErrorMessage = (message) ->
    $("#error > p").html(message)
    $("#error").show().delay(5000).fadeOut("slow")
    $("#error > p").html()

  # Schaltet den Zustand der disabled-Eigenschaft um.
  toggleDisabledProperty = (elementId) ->
    if $("##{ elementId }").prop("disabled") is true
      enable elementId
    else
      disable elementId

  # Schaltet den Zustand der Weg-Geschwindigkeit-Veränderungs-Controls um.
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
