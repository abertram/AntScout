#!/bin/bash

rm hamburg.osm.bz2

wget --timeout=0 http://download.geofabrik.de/osm/europe/germany/hamburg.osm.bz2

boundingBoxes=(\
# hamburg.osm.bz2 Altona-Zoomstufe-18 53.548605 9.932832 53.54625 9.938046 \
# hamburg.osm.bz2 Altona-Zoomstufe-17 53.54978 9.93022 53.54507 9.94065 \
# hamburg.osm.bz2 Altona-Zoomstufe-16 53.55214 9.92501 53.54272 9.94587 \
# hamburg.osm.bz2 Altona-Zoomstufe-15 53.55685 9.91458 53.538 9.9563 \
# hamburg.osm.bz2 Altona-Zoomstufe-14 53.5663 9.8937 53.5286 9.9772 \
# hamburg.osm.bz2 Altona-Zoomstufe-13 53.5851 9.852 53.5097 10.0189 \
# hamburg.osm.bz2 Altona-Zoomstufe-12 53.6227 9.7686 53.472 10.1023 \
# hamburg.osm.bz2 Hohenfelde-Zoomstufe-15 53.57133 9.99961 53.55201 10.04205
# schleswig-holstein.osm.bz2 Ellerau-Zoomstufe-18 53.751206 9.914059 53.748862 9.919274 \
# schleswig-holstein.osm.bz2 Ellerau-Zoomstufe-17 53.75238 9.91145 53.74769 9.92188 \
# schleswig-holstein.osm.bz2 Ellerau-Zoomstufe-16 53.75472 9.90624 53.74535 9.92709 \
# schleswig-holstein.osm.bz2 Ellerau-Zoomstufe-15 53.75941 9.89581 53.74066 9.93752 \
# schleswig-holstein.osm.bz2 Ellerau-Zoomstufe-14 53.7688 9.875 53.7313 9.9584 \
# schleswig-holstein.osm.bz2 Ellerau-Zoomstufe-13 53.7875 9.8332 53.7125 10.0001 \
# schleswig-holstein.osm.bz2 Ellerau-Zoomstufe-12 53.825 9.7498 53.675 10.0835\
# hamburg.osm.bz2 Altona-Wedel 53.6 9.6 53.511 9.92 # OSM: 16295 nodes, Ant-Map: 396 nodes
# hamburg.osm.bz2 Altona-Wedel 53.6 9.6 53.49 9.92 # OSM: ..., nodes, Ant-Map: 409 nodes
# hamburg.osm.bz2 Altona-Wedel 53.6 9.6 53.49 9.94 # OSM: ..., nodes, Ant-Map: 509 nodes
# hamburg.osm.bz2 Altona-Wedel 53.6 9.6 53.49 9.95 # OSM: ..., nodes, Ant-Map: 680 nodes
# hamburg.osm.bz2 Altona-Wedel 53.6 9.6 53.49 9.96 # OSM: ..., nodes, Ant-Map: 630 nodes
# hamburg.osm.bz2 Altona-Wedel 53.6 9.6 53.49 9.97 # OSM: ..., nodes, Ant-Map: 732 nodes
# hamburg.osm.bz2 Altona-Wedel 53.6 9.6 53.49 9.9915 # OSM: ..., nodes, Ant-Map: 9993 nodes
 hamburg.osm.bz2 Altona-Wedel 53.585 9.69 53.545 9.95 # OSM: ..., nodes, Ant-Map: 9993 nodes
#  hamburg.osm.bz2 Altona-Kreis 53.55131 9.9362 53.5468 9.94756
)
boundingBoxParamterCount=6

function filterMap {
  osmosis-0.40.1/bin/osmosis -q\
    --read-xml enableDateParsing=no $1.osm\
    --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary,secondary_link,tertiary,tertiary_link,unclassified,residential,living_street\
    --tf reject-relations\
    --used-node\
    --write-xml $1-preprocessed.osm
}

for ((i=0; i<${#boundingBoxes[*]}; i=$(($i+$boundingBoxParamterCount))))
do
  map=${boundingBoxes[$i]}
  name=${boundingBoxes[$((i+1))]}
  top=${boundingBoxes[$((i+2))]}
  left=${boundingBoxes[$((i+3))]}
  bottom=${boundingBoxes[$((i+4))]}
  right=${boundingBoxes[$((i+5))]}
  echo $name $top $left $bottom $right
  bzcat $map | osmosis-0.40.1/bin/osmosis -q\
    --read-xml enableDateParsing=no file=-\
    --bounding-box top=$top left=$left bottom=$bottom right=$right completeWays=yes\
    --write-xml $name.osm

  filterMap $name
done

bzcat hamburg.osm.bz2 | osmosis-0.40.1/bin/osmosis -q\
  --read-xml enableDateParsing=no file=-\
  --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary,secondary_link,tertiary,tertiary_link,residential,living_street,unclassified\
  --tf reject-relations\
  --used-node\
  --write-xml hamburg.osm
