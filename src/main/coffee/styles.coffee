define ->
  style =
    fillOpacity: 0.6
    pointRadius: 8
  wayStyle = $.extend({}, style,
    strokeOpacity: 0.6
    strokeWidth: 7
  )
  styles =
    directionsDefaultStyle: $.extend({}, wayStyle,
      strokeColor: "#00cc00"
    )
    incomingWaysStyle: $.extend({}, style,
      fillColor: "lightgreen"
      strokeColor: "green"
    )
    nodesDefaultStyle: $.extend({}, style,
      fillColor: "#ff7373"
      strokeColor: "#ff0000"
    )
    nodesSelectStyle: $.extend({}, style,
      fillColor: "#67e667"
      strokeColor: "#00cc00"
    )
    outgoingWaysStyle: $.extend({}, style,
      fillColor: "lightcoral"
      strokeColor: "red"
    )
    waysDefaultStyle: $.extend({}, wayStyle,
      strokeColor: "#ff9200"
    )
    waysSelectStyle: $.extend({}, wayStyle,
      strokeColor: "#0b61a4"
    )
