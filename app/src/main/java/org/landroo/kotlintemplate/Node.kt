package org.landroo.kotlintemplate

import android.graphics.*
import android.util.Log

/**
 * Node class
 */
class Node(px: Double, py: Double, id: Int, shape: Int, scale: Float){

    private val TAG = "Node"

    companion object {
        const val RECTANGLE = 1
        const val CIRCLE = 2
        const val RHOMBUS = 3
        const val TRIANGLE1 = 4
        const val TRIANGLE2 = 5
        const val TRIANGLE3 = 6
        const val TRIANGLE4 = 7

        const val NORMAL = 0
        const val MOVE = 1
        const val EDIT = 2
    }

    var corner = 12

    var px: Double = px// X position
    var py: Double = py// Y position

    var id: Int = id

    var mode: Int = 0

    var angle = 0.0

    var width: Double = 320.0
    var height: Double = 240.0

    var path: Path = Path()
    var bound: Path = Path()
    var points: ArrayList<Utils.Point2D> = ArrayList()

    var rect: RectF = RectF()

    var text: String = ""

    var shape:Int = shape

    var color: Int = 0xFFAAAAAA.toInt()

    var u: Double = 0.0
    var v: Double = 0.0

    var textWidth = 0

    private var xarr: DoubleArray
    private var yarr: DoubleArray

    private var scale = scale

    /**
     * constructor
     */
    init {

        setShapes(false)

        xarr = DoubleArray(points.size)
        yarr = DoubleArray(points.size)

        var paint: Paint = Paint()
        paint.textSize = 16f * scale

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        textWidth = bounds.width()

    }

    /**
     * Load node
     */
    fun setNode(px: Double, py: Double, width: Double, height: Double, id: Int, shape: Int, text: String, color: Int, points: String) {
        this.px = px
        this.py = py
        this.width = width
        this.height = height
        this.id = id
        this.shape = shape
        this.text = text
        this.color = color

        val plarr = points.split(";")
        plarr.forEach {
            if(it.trim() != "") {
                val pntstr = it.split(",")
                val pnt = Utils.Point2D(pntstr[0].toDouble(), pntstr[1].toDouble())
                this.points.add(pnt)
            }
        }

        setShapes(true)
    }

    /**
     * is the point inside a the polygon
     */
    fun isInside(posx: Double, posy: Double, zx: Double, zy: Double): Boolean {
        var i = 0
        var pf: Utils.Point2D
        for (pnt in points) {
            pf = Utils().rotatePnt((px + u) * zx, (py + v) * zy, (pnt.x + px) * zx, (pnt.y + py) * zy, angle * Utils().DEGTORAD)
            xarr[i] = pf.x
            yarr[i] = pf.y
            i++
        }

        return Utils().ponitInPoly(xarr, yarr, posx, posy)
    }

    /**
     * resize button selected
     */
    fun resized(x: Double, y: Double, zx: Double, zy: Double): Boolean {
        if(x > (px + width - corner * scale) * zx && x < (px + width) * zx && y > (py + height - corner * scale) * zy && y < (py + height) * zy)
            return true
        return false
    }

    /**
     * resize node
     */
    fun resize(x: Double, y: Double){
        if(width + x > 120 && width + x < 720 && height + y > 80 && height + y < 720) {
            width = width + x
            height = height + y

            setShapes(true)
        }
    }

    /**
     * move position
     */
    fun movePos(dx: Double, dy: Double) {
        px += dx
        py += dy
    }

    /**
     * set text and position
     */
    fun setNodeText(txt: String) {
        text = txt

        var paint: Paint = Paint()
        paint.textSize = 16f * scale

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        width = bounds.width().toDouble() + paint.textSize
        if(width < 320) width = 320.0
        height = width * (3.0 / 4.0)
        setShapes(true)

        textWidth = bounds.width()
    }

    /**
     * set node shape
     */
    private fun setShapes(oval: Boolean) {

        points.removeAll(points)
        path.reset()
        bound.reset()

        u = width / 2
        v = height / 2

        when (shape) {
            RECTANGLE -> {
                color = Color.CYAN
                if(text == "") text = "Rectangle"
                setRect()
            }
            CIRCLE -> {
                color = Color.RED
                if(text == "") text = "Circle"
                setCircle(oval)
            }
            RHOMBUS -> {
                color = Color.GREEN
                if(text == "") text = "Rhombus"
                setRhombus()
            }
            TRIANGLE1 -> {
                color = Color.YELLOW
                if(text == "") text = "Triangle"
                setTriangle1()
            }
            TRIANGLE2 -> {
                color = Color.YELLOW
                if(text == "") text = "Triangle"
                setTriangle2()
            }
            TRIANGLE3 -> {
                color = Color.YELLOW
                if(text == "") text = "Triangle"
                setTriangle3()
            }
            TRIANGLE4 -> {
                color = Color.YELLOW
                if(text == "") text = "Triangle"
                setTriangle4()
            }
        }

        setPath()
        setBound()
    }

    /**
     * set drawing path
     */
    private fun setPath() {
        var first = true
        for (pnt in points) {
            if (first) {
                path.moveTo(pnt.x.toFloat(), pnt.y.toFloat())
                first = false
            }
            path.lineTo(pnt.x.toFloat(), pnt.y.toFloat())
        }
    }

    /**
     * set bound
     */
    private fun setBound() {
        bound.moveTo(0f, 0f)
        bound.lineTo(width.toFloat(), 0f)
        bound.lineTo(width.toFloat(), height.toFloat())
        bound.lineTo(0f, height.toFloat())
        bound.lineTo(0f, 0f)

        rect.left = width.toFloat() - corner * scale
        rect.top = height.toFloat() - corner * scale
        rect.right = width.toFloat()
        rect.bottom = height.toFloat()
    }

    /**
     * set rectangle
     */
    private fun setRect() {
        points.add(Utils.Point2D(0.0, 0.0))
        points.add(Utils.Point2D(width, 0.0))
        points.add(Utils.Point2D(width, height))
        points.add(Utils.Point2D(0.0, height))
        points.add(Utils.Point2D(0.0, 0.0))
    }

    /**
     * set circle
     */
    private fun setCircle(oval: Boolean) {
        color = Color.RED
        if(!oval)
            height = width
        u = width / 2
        v = height / 2
        for (i in 0..24) {
            val t = 2.0 * Math.PI * i.toDouble() / 24
            val x = u + u * Math.cos(t)
            val y = v + v * Math.sin(t)
            points.add(Utils.Point2D(x, y))
        }
    }

    /**
     * set rhombus
     */
    private fun setRhombus() {
        color = Color.GREEN
        points.add(Utils.Point2D(width / 2.0, 0.0))
        points.add(Utils.Point2D(width, height / 2.0))
        points.add(Utils.Point2D(width / 2.0, height))
        points.add(Utils.Point2D(0.0, height / 2.0))
        points.add(Utils.Point2D(width / 2.0, 0.0))
    }

    /**
     * set triangle
     */
    private fun setTriangle1() {
        color = Color.YELLOW
        points.add(Utils.Point2D(width / 2.0, 0.0))
        points.add(Utils.Point2D(width, height))
        points.add(Utils.Point2D(0.0, height))
        points.add(Utils.Point2D(width / 2.0, 0.0))
    }

    /**
     * set triangle
     */
    private fun setTriangle2() {
        color = Color.YELLOW
        points.add(Utils.Point2D(0.0, 0.0))
        points.add(Utils.Point2D(width, 0.0))
        points.add(Utils.Point2D(width / 2.0, height))
        points.add(Utils.Point2D(0.0, 0.0))
    }

    /**
     * set triangle
     */
    private fun setTriangle3() {
        color = Color.YELLOW
        points.add(Utils.Point2D(0.0, 0.0))
        points.add(Utils.Point2D(width, height / 2.0))
        points.add(Utils.Point2D(0.0, height))
        points.add(Utils.Point2D(0.0, 0.0))
    }

    /**
     * set triangle
     */
    private fun setTriangle4() {
        color = Color.YELLOW
        points.add(Utils.Point2D(0.0, height / 2.0))
        points.add(Utils.Point2D(width, 0.0))
        points.add(Utils.Point2D(width, height))
        points.add(Utils.Point2D(0.0, height / 2.0))
    }

    /**
     * create node json
     */
    fun toJson(): String {
        var sb = StringBuilder()

        sb.append("{")

        sb.append("\"id\":")
        sb.append(id)
        sb.append(",")

        sb.append("\"shape\":")
        sb.append(shape)
        sb.append(",")

        sb.append("\"px\":")
        sb.append(px)
        sb.append(",")
        sb.append("\"py\":")
        sb.append(py)
        sb.append(",")

        sb.append("\"width\":")
        sb.append(width)
        sb.append(",")
        sb.append("\"height\":")
        sb.append(height)
        sb.append(",")

        sb.append("\"text\":\"")
        sb.append(text)
        sb.append("\",")

        sb.append("\"color\":")
        sb.append(color)
        sb.append(",")

        sb.append("\"points\":\"")
        for (pnt in points) {
            sb.append(pnt.x)
            sb.append(",")
            sb.append(pnt.y)
            sb.append(";")
        }
        sb.append("\"")

        sb.append("}")

        return sb.toString()
    }

}

