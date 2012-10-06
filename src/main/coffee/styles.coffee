define ->
  defaultStyle = {}
  defaultWayStyle = $.extend({}, defaultStyle,
    weight: 5
  )
  defaultSelectedWay = $.extend({}, defaultWayStyle,
    weight: 8
  )
  styles =
    incomingWay: $.extend({}, defaultWayStyle,
      color: "#ff0000"
    )
    node: $.extend({}, defaultStyle,
      radius: 10
    )
    outgoingWay: $.extend({}, defaultWayStyle,
      color: "#00ff00"
    )
    path: $.extend({}, defaultWayStyle,
      color: "#ff7400"
      opacity: 0.7
    )
    selectedIncomingWay: $.extend({}, defaultSelectedWay)
    selectedNode: $.extend({}, defaultStyle,
      radius: 15
    )
    selectedOutgoingWay: $.extend({}, defaultSelectedWay)
    selectedPath: $.extend({}, defaultSelectedWay)
    selectedWay: $.extend({}, defaultSelectedWay)
    way: $.extend({}, defaultWayStyle,
      color: "#0000ff"
    )
