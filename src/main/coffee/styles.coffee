# Hier sind die Stile für die Knoten und Wege definiert.
define ->
  # Gemeinsamer Standard-Stil, wird von anderen Stilen erweitern
  defaultStyle = {}
  # Gemeinsamer Standard-Weg-Stil
  defaultWayStyle = $.extend({}, defaultStyle,
    weight: 5
  )
  # Gemeinsamer Standard-Stil eines selektierten Weges
  defaultSelectedWay = $.extend({}, defaultWayStyle,
    weight: 8
  )
  # Weitere Stile
  styles =
    # Eingehender Weg
    incomingWay: $.extend({}, defaultWayStyle,
      color: "#ff0000"
    )
    # Knoten
    node: $.extend({}, defaultStyle,
      radius: 10
    )
    # Ausgehender Weg
    outgoingWay: $.extend({}, defaultWayStyle,
      color: "#00ff00"
    )
    # Pfad
    path: $.extend({}, defaultWayStyle,
      color: "#ff7400"
      opacity: 0.7
    )
    # Selektierter eingehender Weg
    selectedIncomingWay: $.extend({}, defaultSelectedWay)
    # Selektierter Knoten
    selectedNode: $.extend({}, defaultStyle,
      radius: 15
    )
    # Selektierter ausgehender Weg
    selectedOutgoingWay: $.extend({}, defaultSelectedWay)
    # Selektierter Pfad
    selectedPath: $.extend({}, defaultSelectedWay)
    # Selektierter Weg
    selectedWay: $.extend({}, defaultSelectedWay)
    # Weg
    way: $.extend({}, defaultWayStyle,
      color: "#0000ff"
    )
