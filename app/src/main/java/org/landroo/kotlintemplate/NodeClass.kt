package org.landroo.kotlintemplate

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.content.Context
import android.graphics.PathEffect
import android.graphics.PathDashPathEffect
import android.graphics.Path
import android.util.Log
import org.json.JSONObject

/**
 * Node list handler class
 */
class NodeClass(context: Context){

    private val TAG = "NodeClass"
    private val TOLERANCE = 10
    private val GESTURE_THRESHOLD_DIP = 16.0f
    private val pathSize = 5.0
    private var ARROW = 30

    private val nodes: ArrayList<Node> = ArrayList()
    private val links: ArrayList<NodeLink> = ArrayList()

    private val selectPaint: Paint = Paint()
    private val editPaint: Paint = Paint()
    private val strokePaint: Paint = Paint()
    private val linePaint: Paint = Paint()
    private val foreColor: Paint = Paint()
    private val backColor: Paint = Paint()

    private var effect: PathEffect? = null

    private var nodeId: Int = 0
    private var linkId: Int = 0
    private var scale: Float = 1f

    /**
     * node link class
     */
    class NodeLink(n1: Node, n2: Node, var id: Int) {
        var node1: Node = n1
        var node2: Node = n2

        var selected: Boolean = false
    }

    /**
     * constructor
     */
    init {

        ARROW = (ARROW * scale).toInt()

        effect = PathDashPathEffect(makePathPattern(pathSize * 4, pathSize * 2), (pathSize * 4).toFloat(), 0.0f,
                PathDashPathEffect.Style.ROTATE)

        scale = context.resources.displayMetrics.density
        foreColor.textSize = GESTURE_THRESHOLD_DIP * scale + 0.5f

        strokePaint.color = 0xFFFF0000.toInt()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 5f

        selectPaint.color = 0xFF09C671.toInt()
        selectPaint.style = Paint.Style.FILL

        editPaint.color = 0xFF09C671.toInt()
        editPaint.style = Paint.Style.FILL

        backColor.color = 0xFFAAAAAA.toInt()

        linePaint.color = 0xFF000000.toInt()
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 10f
    }

    /**
     * draw all node
     */
    fun drawNodes(canvas: Canvas, xPos: Double, yPos: Double, zx: Double, zy: Double, displayWidth: Int, displayHeight: Int) {
        var paint: Paint
        for (item in nodes) {

            val dx = xPos + item.px * zx
            val dy = yPos + item.py * zy

            if (dx > -item.width * zx && dx < displayWidth + item.width * zx && dy > -item.height * zy && dy < displayHeight + item.height * zy) {

                canvas.save()
                canvas.translate((xPos + item.px * zx).toFloat(), (yPos + item.py * zy).toFloat())
                canvas.scale(zx.toFloat(), zy.toFloat())

                canvas.clipRect(0f, 0f, item.width.toFloat(), item.height.toFloat())

                when(item.mode) {
                    Node.MOVE -> {
                        paint = selectPaint
                    }
                    Node.EDIT -> {
                        paint = editPaint
                    }
                    else -> {
                        paint = backColor
                        paint.color = item.color
                        strokePaint.pathEffect = null
                    }
                }

                canvas.drawPath(item.path, paint)

                canvas.drawPath(item.path, strokePaint)

                if(item.mode == Node.MOVE || item.mode == Node.EDIT) {
                    strokePaint.pathEffect = effect
                    canvas.drawPath(item.bound, strokePaint)
                    if(item.mode == Node.EDIT) {
                        canvas.drawRect(item.rect, paint)
                        strokePaint.pathEffect = null
                        canvas.drawRect(item.rect, strokePaint)
                    }
                }

                canvas.rotate(item.angle.toFloat())

                canvas.drawText("" + item.text, ((item.width - item.textWidth) / 2).toFloat(), item.v.toFloat() + foreColor.textSize / 2, foreColor)

                canvas.restore()
            }
        }
    }

    /**
     * add a new node
     */
    fun addNewNode(px: Double, py: Double, shape: Int) {
        val node = Node(px, py, ++nodeId, shape, scale)
        nodes.add(node)
    }

    /**
     * select a node
     */
    fun selectNode(px: Double, py: Double, zx: Double, zy: Double): Node? {
        var selItem: Node? = null
        for (i in nodes.size - 1 downTo 0) {
            val item = nodes.get(i)
            if (item.isInside(px, py, zx, zy)) {
                //nodeClass.reorder(item, i)
                selItem = item
                break
            }
        }
        return selItem
    }

    /**
     * select a link line
     */
    fun selectLink(px: Double, py: Double, zx: Double, zy: Double): NodeLink? {

        val xarr = DoubleArray(6)
        val yarr = DoubleArray(6)
        for (link in links) {

            val cx1 = (link.node1.px + link.node1.u) * zx
            val cy1 = (link.node1.py + link.node1.v) * zy

            val cx2 = (link.node2.px + link.node2.u) * zx
            val cy2 = (link.node2.py + link.node2.v) * zy

            // p1 = cx1 cy1
            // p2 = cx2 cy1
            // p3 = cx2 cy1
            // p4 = cx2 cx2

            xarr[0] = cx1 - TOLERANCE
            yarr[0] = cy1 - TOLERANCE
            xarr[1] = cx2 - TOLERANCE
            yarr[1] = cy1 - TOLERANCE
            xarr[2] = cx2 - TOLERANCE
            yarr[2] = cy2 - TOLERANCE
            xarr[3] = cx2 + TOLERANCE
            yarr[3] = cy1 - TOLERANCE
            xarr[4] = cx2 + TOLERANCE
            yarr[4] = cy1 + TOLERANCE
            xarr[5] = cx1 - TOLERANCE
            yarr[5] = cy1 + TOLERANCE

            val bIn = Utils().ponitInPoly(xarr, yarr, px, py)
            if(bIn) {
                return link
            }
        }
        return null
    }

    /**
     * add nodes link
     */
    fun linkNodes(node1: Node, node2: Node) {
        var bOK = true
        for (link in links) {
            if((link.node1.id == node1.id && link.node2.id == node2.id)
            || (link.node2.id == node1.id && link.node1.id == node2.id)) {
                bOK = false
            }
        }

        if(bOK) {
            //Log.i(TAG, "link added")
            val link = NodeLink(node1, node2, ++linkId)
            links.add(link)
        }
    }

    /**
     * draw the nodes links
     */
    fun drawLinks(canvas: Canvas, xPos: Double, yPos: Double, zx: Double, zy: Double, displayWidth: Int, displayHeight: Int) {
        for (link in links) {

            val cx1 = link.node1.px + link.node1.u
            val cy1 = link.node1.py + link.node1.v

            val cx2 = link.node2.px + link.node2.u
            val cy2 = link.node2.py + link.node2.v

            val w = Math.abs(cx1 - cx2)
            val h = Math.abs(cy1 - cy2)

            val dx1 = xPos + cx1 * zx
            val dy1 = yPos + cy1 * zy

            val dx2 = xPos + cx2 * zx
            val dy2 = yPos + cy2 * zy

            if (dx1 > -w * zx && dx1 < displayWidth + w * zx && dy1 > -h * zy && dy1 < displayHeight + h * zy
            && dx2 > -w && dx2 < displayWidth + w * zx && dy2 > -h && dy2 < displayHeight + h * zy) {

                canvas.save()

                canvas.translate((xPos).toFloat(), (yPos).toFloat())
                canvas.scale(zx.toFloat(), zy.toFloat())

                //canvas.clipRect(0f, 0f, item.width.toFloat(), item.height.toFloat())

                if(link.selected) {
                    linePaint.color = Color.CYAN
                }
                else {
                    linePaint.color = Color.BLACK
                }
                canvas.drawLine(cx1.toFloat(), cy1.toFloat(), cx2.toFloat(), cy1.toFloat(), linePaint)
                canvas.drawLine(cx2.toFloat(), cy1.toFloat(), cx2.toFloat(), cy2.toFloat(), linePaint)

                if(cx2 > cx1) {
                    canvas.drawLine(cx1.toFloat() + (cx2 - cx1).toFloat() / 2 - ARROW, cy1.toFloat() - ARROW, cx2.toFloat() - (cx2 - cx1).toFloat() / 2, cy1.toFloat(), linePaint)
                    canvas.drawLine(cx1.toFloat() + (cx2 - cx1).toFloat() / 2 - ARROW, cy1.toFloat() + ARROW, cx2.toFloat() - (cx2 - cx1).toFloat() / 2, cy1.toFloat(), linePaint)
                }
                else {
                    canvas.drawLine(cx1.toFloat() + (cx2 - cx1).toFloat() / 2 + ARROW, cy1.toFloat() - ARROW, cx2.toFloat() - (cx2 - cx1).toFloat() / 2, cy1.toFloat(), linePaint)
                    canvas.drawLine(cx1.toFloat() + (cx2 - cx1).toFloat() / 2 + ARROW, cy1.toFloat() + ARROW, cx2.toFloat() - (cx2 - cx1).toFloat() / 2, cy1.toFloat(), linePaint)
                }

                if(cy2 > cy1) {
                    canvas.drawLine(cx2.toFloat() - ARROW, cy1.toFloat() + (cy2 - cy1).toFloat() / 2 - ARROW, cx2.toFloat(), cy2.toFloat() - (cy2 - cy1).toFloat() / 2, linePaint)
                    canvas.drawLine(cx2.toFloat() + ARROW, cy1.toFloat() + (cy2 - cy1).toFloat() / 2 - ARROW, cx2.toFloat(), cy2.toFloat() - (cy2 - cy1).toFloat() / 2, linePaint)
                }
                else{
                    canvas.drawLine(cx2.toFloat() - ARROW, cy1.toFloat() + (cy2 - cy1).toFloat() / 2 + ARROW, cx2.toFloat(), cy2.toFloat() - (cy2 - cy1).toFloat() / 2, linePaint)
                    canvas.drawLine(cx2.toFloat() + ARROW, cy1.toFloat() + (cy2 - cy1).toFloat() / 2 + ARROW, cx2.toFloat(), cy2.toFloat() - (cy2 - cy1).toFloat() / 2, linePaint)
                }

                canvas.restore()
            }
        }
    }

    /**
     * delete the selected node and the connected links
     */
    fun deleteNode(delNode:Node) {
        for (link in links) {
            if(link.node1.id == delNode.id || link.node2.id == delNode.id) {
                links.remove(link)
                deleteNode(delNode)
                break
            }
        }
        nodes.remove(delNode)
    }

    /**
     * delete the selected link
     */
    fun deleteLink(delLink: NodeLink) {
        links.remove(delLink)
    }

    /**
     * create selection border pattern
     */
    private fun makePathPattern(w: Double, h: Double): Path {
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo((w / 2.0).toFloat(), 0f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo((w / 2.0).toFloat(), h.toFloat())
        path.close()

        return path
    }

    /**
     * serialize nodes
     */
    fun nodeList(): String {
        val sb = StringBuilder()
        for (node in nodes) {
            sb.append(node.toJson())
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * serialize links
     */
    fun linkList(): String {
        val sb = StringBuilder()
        for (link in links) {
            sb.append(link.id)
            sb.append(";")
            sb.append(link.node1.id)
            sb.append(";")
            sb.append(link.node2.id)
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * load nodes
     */
    fun addNodes(nodeList: String, linkList: String) {
        val nodeArr = nodeList.split("\n")
        nodeId = 0
        nodeArr.forEach {
            if (it.trim() != "") {
                //Log.i(TAG, it)
                val node = JSONObject(it)
                val id = node.get("id") as Int
                val shape = node.get("shape") as Int
                val px = node.get("px") as Double
                val py = node.get("py") as Double
                val width = node.get("width") as Double
                val height = node.get("height") as Double
                val text = node.get("text") as String
                val color = node.get("color") as Int
                val points = node.get("points") as String
                val newNode = Node(px, py, 0, shape, scale)
                //Log.i(TAG, "id: " + id)
                newNode.setNode(px, py, width, height, id, shape, text, color, points)
                nodes.add(newNode)
                if(nodeId < id) nodeId = id + 1
            }
        }

        val linkArr = linkList.split("\n")
        linkArr.forEach {
            if (it.trim() != "") {
                val arr = it.split(";")
                val id = arr[0].toInt()
                val id1 = arr[1].toInt()
                val id2 = arr[2].toInt()
                var node1 = nodes[0]
                var node2 = nodes[0]
                nodes.forEach {
                    if (it.id == id1) {
                        node1 = it
                    }
                    if (it.id == id2) {
                        node2 = it
                    }
                }
                val link = NodeLink(node1, node2, id)
                links.add(link)
                linkId++
            }
        }
    }
}