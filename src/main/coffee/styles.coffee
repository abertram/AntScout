define ->
  style =
    fillOpacity: 0.5
    pointRadius: 6
    strokeOpacity: 0.5
    strokeWidth: 5
  styles =
    directionsStyle: $.extend({}, style,
      fillColor: "lightblue"
      strokeColor: "blue"
    )
    incomingWaysStyle: $.extend({}, style,
      fillColor: "lightgreen"
      strokeColor: "green"
    )
    nodesStyle: $.extend({}, style,
      fillColor: "lightblue"
      strokeColor: "blue"
    )
    outgoingWaysStyle: $.extend({}, style,
      fillColor: "lightcoral"
      strokeColor: "red"
    )
    waysStyle: $.extend({}, style,
      fillColor: "lightblue"
      strokeColor: "blue"
    )
