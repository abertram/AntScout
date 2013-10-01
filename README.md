AntScout applies the Ant Colony Optimization algorithm AntNet for routing in real road maps extracted from OpenStreetMap.

# Quick start guide

Detailed manual under [doc/AntScout.pdf](doc/AntScout.pdf)

## Requirements

* Internet connection
* Current (Oracle) Java version
* The local port `8080` shouldn't be blocked by a concurrent running application.

## Start

* Open a terminal and change to the AntScout directory
* `sbt`
* `container:start`
* Open [http://localhost:8080](http://localhost:8080) or [http://127.0.0.1:8080](http://127.0.0.1:8080) in the browser

## Hints

* The first `sbt` could take some time to download all needed libraries.
* The first `container:start` could take some time to download and preprocess the maps.
* Use a modern browser which supports HTML5 and CSS3 for best user experience.
