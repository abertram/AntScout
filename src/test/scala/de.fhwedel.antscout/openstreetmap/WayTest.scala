package de.fhwedel.antscout.openstreetmap

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import collection.immutable.IntMap

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 24.11.11
 * Time: 18:20
 */

class WayTest extends FunSuite with ShouldMatchers {
    test("parseWay, valid way") {
        val node1 = new Node(1, new GeographicCoordinate(1.0f, 1.0f))
        val node2 = new Node(2, new GeographicCoordinate(2.0f, 2.0f))
        val nodes = IntMap(
            1 -> node1,
            2 -> node2
        )
        val way = Way.parseWay(
            <way id="1">
                <nd ref="1"/>
                <nd ref="2"/>
                <tag k="highway" v="motorway"/>
                <tag k="maxspeed" v="1"/>
                <tag k="name" v="Test way"/>
            </way>, nodes)
        way.id should be (1)
        way.nodes should have length (2)
        way.nodes(0) should equal (node1)
        way.nodes(1) should equal (node2)
        way.name should  be ("Test way")
        way.speed should be (1.)
    }

    test("parseWay, no name") {
        val nodes = IntMap()
        val way = Way.parseWay(<way id="1"/>, nodes)
        way.name should  be ("")
    }

    test("parseWay, empty name") {
        val nodes = IntMap()
        val way = Way.parseWay(
            <way id="1">
                <tag k="name" v=""/>
            </way>, nodes)
        way.name should  be ("")
    }

    test("parseWay, no maxspeed tag") {
        val nodes = IntMap()
        val way = Way.parseWay(<way id="1"/>, nodes)
        way.speed should  be (Way.DefaultSpeeds(""))
    }

    test("parseWay, empty maxspeed tag") {
        val nodes = IntMap()
        val way = Way.parseWay(
            <way id="1">
                <tag k="maxspeed" v=""/>
            </way>, nodes)
        way.speed should  be (Way.DefaultSpeeds(""))
    }

    test("parseWay, maxspeed is not a number") {
        val nodes = IntMap()
        val way = Way.parseWay(
            <way id="1">
                <tag k="maxspeed" v="maxspeed"/>
            </way>, nodes)
        way.speed should  be (Way.DefaultSpeeds(""))
    }

    test("parseWay, no maxspeed tag, speed from highway tag") {
        val nodes = IntMap()
        val way = Way.parseWay(
            <way id="1">
                <tag k="highway" v="motorway"/>
            </way>, nodes)
        way.speed should  be (Way.DefaultSpeeds("motorway"))
    }

    test("parseWay, empty maxspeed tag, speed from highway tag") {
        val nodes = IntMap()
        val way = Way.parseWay(
            <way id="1">
                <tag k="highway" v="motorway"/>
                <tag k="maxspeed" v=""/>
            </way>, nodes)
        way.speed should  be (Way.DefaultSpeeds("motorway"))
    }

    test("parseWay, maxspeed is not a number, speed from highway tag") {
        val nodes = IntMap()
        val way = Way.parseWay(
            <way id="1">
                <tag k="highway" v="motorway"/>
                <tag k="maxspeed" v="maxspeed"/>
            </way>, nodes)
        way.speed should  be (Way.DefaultSpeeds("motorway"))
    }
}
