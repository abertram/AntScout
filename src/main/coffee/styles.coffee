define ->
  defaultStyle = {}
  defaultWayStyle = $.extend({}, defaultStyle,
    weight: 5
  )
  defaultSelectedWay = $.extend({}, defaultWayStyle,
    weight: 10
  )
  styles =
    directionsDefaultStyle: $.extend({}, defaultWayStyle,
      color: "#00cc00"
    )
    incomingWay: $.extend({}, defaultWayStyle,
      color: "#ff0000"
    )
    node: $.extend({}, defaultStyle,
      radius: 10
    )
    outgoingWay: $.extend({}, defaultWayStyle,
      color: "#00ff00"
    )
    selectedWay: $.extend({}, defaultSelectedWay,
      color: "#0000a6"
    )
    selectedIncomingWay: $.extend({}, defaultSelectedWay,
      color: "#a60000"
    )
    selectedNode: $.extend({}, defaultStyle,
      radius: 15
    )
    selectedOutgoingWay: $.extend({}, defaultSelectedWay,
      color: "#00a600"
    )
    way: $.extend({}, defaultWayStyle,
      color: "#0000ff"
    )
