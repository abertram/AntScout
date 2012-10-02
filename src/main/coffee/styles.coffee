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
      color: "#00cc00"
    )
    incomingWaysStyle: $.extend({}, style,
      fillColor: "lightgreen"
      color: "green"
    )
    node: $.extend({}, style,
      fillColor: "#ff7373"
      color: "#ff0000"
      radius: 10
    )
    selectedNode: $.extend({}, style,
      fillColor: "#67e667"
      color: "#00cc00"
      radius: 15
    )
    outgoingWaysStyle: $.extend({}, style,
      fillColor: "lightcoral"
      color: "red"
    )
    waysDefaultStyle: $.extend({}, wayStyle,
      color: "#ff9200"
    )
    waysSelectStyle: $.extend({}, wayStyle,
      color: "#0b61a4"
    )
