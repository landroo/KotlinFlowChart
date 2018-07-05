package org.landroo.kotlintemplate

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

class Utils {
    val RADTODEG: Double = 57.295779513082320876
    val DEGTORAD: Double = 0.0174532925199432957

    // get ang from first point to next
    fun getAng(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val nDelX = x2 - x1
        val nDelY = y2 - y1
        var nDe = 0.0

        if (nDelX != 0.0) {
            nDe = 2 * Math.PI
            nDe = nDe + Math.atan(nDelY / nDelX)
            if (nDelX <= 0) {
                nDe = Math.PI
                nDe = nDe + Math.atan(nDelY / nDelX)
            } else {
                if (nDelY >= 0) {
                    nDe = 0.0
                    nDe = nDe + Math.atan(nDelY / nDelX)
                }
            }
        } else {
            if (nDelY == 0.0) {
                nDe = 0.0
            } else {
                if (nDelY < 0) {
                    nDe = Math.PI
                }
                nDe = nDe + Math.PI / 2
            }
        }

        return nDe / Math.PI * 180
    }

    // return the distance
    fun getDist(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val nDelX = x2 - x1
        val nDelY = y2 - y1

        return Math.sqrt(nDelY * nDelY + nDelX * nDelX)
    }

    class Point2D(x: Double, y: Double) {
        internal var x: Double = x
        internal var y: Double = y

        fun isempty(): Boolean {
            if(x == 0.0 && y == 0.0) return true
            return false
        }
        fun empty() {
            x = 0.0
            y = 0.0
        }
    }

    // point line distance
    fun pointLineDist(A: Point2D, B: Point2D, pnt: Point2D): Double {
        val p2 = Point2D(B.x - A.x, B.y - A.y)
        val f = p2.x * p2.x + p2.y * p2.y
        var u = ((pnt.x - A.x) * p2.x + (pnt.y - A.y) * p2.y) / f

        if (u > 1)
            u = 1.0
        else if (u < 0) u = 0.0

        val x = A.x + u * p2.x
        val y = A.y + u * p2.y

        val dx = x - pnt.x
        val dy = y - pnt.y

        return Math.sqrt(dx * dx + dy * dy)
    }

    // rotate a point (x, y) around the center (u, v) with a radian
    fun rotatePnt(u: Double, v: Double, x: Double, y: Double, ang: Double): Point2D {

        var x: Double = (x - u) * Math.cos(ang) - (y - v) * Math.sin(ang) + u
        var y:Double = (x - u) * Math.sin(ang) + (y - v) * Math.cos(ang) + v

        return Point2D(x, y)
    }

    // is a point inside a polygon
    fun ponitInPoly(vertX: DoubleArray, vertY: DoubleArray, testX: Double, testY: Double): Boolean {
        var i: Int
        var j: Int
        var b = false
        i = 0
        j = vertX.size - 1
        while (i < vertX.size) {
            if (vertY[i] > testY != vertY[j] > testY && testX < (vertX[j] - vertX[i]) * (testY - vertY[i]) / (vertY[j] - vertY[i]) + vertX[i]) {
                b = !b
            }
            j = i++
        }

        return b
    }

    // recursive implementation of binary search
    fun rank(key: Int, a: IntArray): Int {
        return rank(key, a, 0, a.size - 1)
    }
    fun rank(key: Int, a: IntArray, lo: Int, hi: Int): Int {
        // Index of key in a[], if present, is not smaller than lo and not larger than hi.
        if (lo > hi) return -1
        val mid = lo + (hi - lo) / 2
        return if (key < a[mid])
            rank(key, a, lo, mid - 1)
        else if (key > a[mid])
            rank(key, a, mid + 1, hi)
        else
            mid
    }

    // create base64 string from the bitmap
    fun bitmapToBase64(bmp: Bitmap?): String {
        var encodedImage = ""

        if (bmp != null) {
            val byteArrayBitmapStream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 10, byteArrayBitmapStream)
            val b = byteArrayBitmapStream.toByteArray()
            encodedImage = Base64.encodeToString(b, Base64.NO_WRAP)
        }

        return encodedImage
    }

    // create a random number between minimum and maximum: Utils.random(0, 9, 1)
    fun random(nMinimum: Int, nMaximum: Int, nRoundToInterval: Int): Int {
        var nMinimum = nMinimum
        var nMaximum = nMaximum
        if (nMinimum > nMaximum) {
            val nTemp = nMinimum
            nMinimum = nMaximum
            nMaximum = nTemp
        }

        val nDeltaRange = nMaximum - nMinimum + 1 * nRoundToInterval
        var nRandomNumber = Math.random() * nDeltaRange

        nRandomNumber += nMinimum.toDouble()

        return (Math.floor(nRandomNumber / nRoundToInterval) * nRoundToInterval).toInt()
    }

    // get projected point on line
    fun getProjectedPointOnLine(x1: Double, y1: Double, x2: Double, y2: Double, x: Double, y: Double): Point2D
    {
        var ex1 = x2 - x1
        var ey1 = y2 - y1
        var ex2 = x - x1
        var ey2 = y - y1

        var valDp = ex1 * ex2 + ey1 * ey2

        var lenLineE1 = Math.sqrt(ex1 * ex1 + ey1 * ey1)
        var lenLineE2 = Math.sqrt(ex2 * ex2 + ey2 * ey2)

        var cos = valDp / (lenLineE1 * lenLineE2)
        var projLenOfLine = cos * lenLineE2

        var rx = x1 + (projLenOfLine * ex1) / lenLineE1
        var ry = y1 + (projLenOfLine * ey1) / lenLineE1

        return Point2D(rx, ry)
    }

}