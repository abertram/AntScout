define ->
  defaultStyle = {}
  defaultWayStyle = $.extend({}, defaultStyle,
    weight: 5
  )
  defaultSelectedWay = $.extend({}, defaultWayStyle,
    weight: 8
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
    selectedIncomingWay: $.extend({}, defaultSelectedWay)
    selectedNode: $.extend({}, defaultStyle,
      radius: 15
    )
    selectedOutgoingWay: $.extend({}, defaultSelectedWay)
    selectedWay: $.extend({}, defaultSelectedWay)
    way: $.extend({}, defaultWayStyle,
      color: "#0000ff"
    )
