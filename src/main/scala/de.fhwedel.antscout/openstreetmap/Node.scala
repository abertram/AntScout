package de.fhwedel.antscout
package openstreetmap

import scala.math._
import net.liftweb.common.Logger

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 11:36
 */

class Node(val id: Int, val geographicCoordinate: GeographicCoordinate) {
    val logger = Logger(classOf[Node])

    /**
      * Berechnet die geographischen Entfernung zu einem anderen Knoten.
      *
      * Die Berechnung basiert auf der Vincenty-Formel (http://en.wikipedia.org/wiki/Vincenty%27s_formulae) inklusive
      * Vincenty's Modifizierung (http://en.wikipedia.org/wiki/Vincenty%27s_formulae#Vincenty.27s_modification).
      *
      * @param that Knoten, zu dem der Abstand berechnet werden soll.
      * @return Geographischer Abstand in Metern oder 0, wenn der Abstand nicht berechnet werden konnte.
      */
    def distanceTo(that: Node) = {
        val a = 6378137.
        val f = 1 / 298.257223563
        val b = (1 - f) * a
        val U1 = atan((1 - f) * tan(geographicCoordinate.latitude.toRadians))
        val U2 = atan((1 - f) * tan(that.geographicCoordinate.latitude.toRadians))
        val L = (geographicCoordinate.longitude - that.geographicCoordinate.longitude).toRadians
        val sinU1 = sin(U1)
        val cosU1 = cos(U1)
        val sinU2 = sin(U2)
        val cosU2 = cos(U2)

        def iterate(lambda: Double, iterationLimit: Int): Option[(Double, Double, Double, Double, Double)] = {
            iterationLimit match {
                case 0 =>
                    None
                case _ =>
                    val sinLambda = sin(lambda)
                    val cosLambda = cos(lambda)
                    val sinSigma = sqrt(pow(cosU2 * sinLambda, 2) + pow(cosU1 * sinU2 - sinU1 * cosU2 * cosLambda, 2))
                    if (sinSigma.isNaN)
                        None
                    val cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
                    val sigma = atan2(sinSigma, cosSigma)
                    val alpha = asin(cosU1 * cosU2 * sinLambda / sinSigma)
                    val cosSquaredAlpha = 1 - pow(sin(alpha), 2)
                    val cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSquaredAlpha
                    val c = f / 16 * cosSquaredAlpha * (4 + f * (4 - 3 * cosSquaredAlpha))
                    val lambdaPrime = L + (1 - c) * f * sin(alpha) * (sigma + c * sinSigma * (cos2SigmaM + c * cosSigma * (-1 + 2 * pow(cos2SigmaM, 2))))
                    if (abs(lambda - lambdaPrime) <= 1e-12)
                        Some(Tuple5(cosSquaredAlpha, sinSigma, cos2SigmaM, cosSigma, sigma))
                    else
                        iterate(lambdaPrime, iterationLimit - 1)
            }
        }

        iterate(L, 99) match {
            case None =>
                0.
            case iterationResult =>
                val cosSquaredAlpha = iterationResult.get._1
                val sinSigma = iterationResult.get._2
                val cos2SigmaM = iterationResult.get._3
                val cosSigma = iterationResult.get._4
                val sigma = iterationResult.get._5
                val uSquared = cosSquaredAlpha * (pow(a, 2) - pow(b, 2)) / pow(b, 2)
                val sqrtOfOnePlusUSquared = sqrt(1 + uSquared)
                val k1 = (sqrtOfOnePlusUSquared - 1) / (sqrtOfOnePlusUSquared + 1)
                val A = (1 + 1 / 4 * pow(k1, 2)) / (1 - k1)
                val B = k1 * (1 - 3/8 * pow(k1, 2))
                val deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * pow(cos2SigmaM, 2)) - B / 6 * cos2SigmaM * (-3 + 4 * pow(sinSigma, 2)) * (-3 + 4 * pow(cos2SigmaM, 2))))
                b * A * (sigma - deltaSigma)
        }
    }

    override def toString = "[%d] %s".format(id, geographicCoordinate)
}

object Node {
    val logger = Logger(getClass)

    def parseNode(node: xml.Node): Node = {
        val id = (node \ "@id").text.toInt
        logger.assertLog(id < 1, id.toString)
        val latitude = (node \ "@lat").text.toFloat
        require(latitude > -90. && latitude < 90., "Node %d: invalid latitude" format id)
        val longitude = (node \ "@lon").text.toFloat
        require(longitude > -180.0 && longitude < 180.0, "Node %d: invalid longitude" format id)
        val geographicCoordinate = new GeographicCoordinate(latitude, longitude)
        new Node(id, geographicCoordinate)
    }
}