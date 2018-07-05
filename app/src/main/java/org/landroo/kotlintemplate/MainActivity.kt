package org.landroo.kotlintemplate

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.graphics.drawable.Drawable
import android.graphics.*
import java.util.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.BitmapFactory
import android.text.Editable
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.view.inputmethod.InputMethodManager
import android.content.res.Configuration

/**
 * Main class
 */
class MainActivity : AppCompatActivity(), TouchInterface {

    private val SCROLL_INTERVAL = 10L
    private val SCROLL_ALPHA = 500
    private val SCROLL_SIZE = 16f
    private val GAP = 10
    private val TAG: String = "MainActivity"
    private val DESKTOP: Int = 10000

    // virtual desktop
    private lateinit var nodeView: NodeView
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private var deskWidth: Int = 0
    private var deskHeight: Int = 0

    // scroll plane
    private lateinit var scaleView: ScaleView
    private var sX = 0.0
    private var sY = 0.0
    private var mX = 0.0
    private var mY = 0.0
    private var zoomX = 1.0
    private var zoomY = 1.0
    private var xPos: Double = 0.0
    private var yPos: Double = 0.0
    private var afterMove = false
    private var paused = false

    // user event handler
    private lateinit var ui: TouchUI
    private var scrollTimer: Timer = Timer()
    private val scrollPaint1 = Paint()
    private val scrollPaint2 = Paint()
    private var scrollAlpha = SCROLL_ALPHA
    private var scrollBar = 0
    private var barPosX = 0.0
    private var barPosY = 0.0
    private val barPaint = Paint()

    // background
    private val tileSize = 80
    private lateinit var backBitmap: Bitmap
    private var backDrawable: Drawable? = null
    private var staticBack = false // fix or scrollable background
    private val backColor = Color.LTGRAY// background color
    private val rotation = 0.0
    private val rx = 0.0
    private val ry = 0.0
    private var longPress = 0
    private var back = "grid1"

    // node vars
    private var nodeClass: NodeClass? = null
    private var selItem: Node? = null
    private var selLink: NodeClass.NodeLink? = null
    private var stPnt: Utils.Point2D = Utils.Point2D(0.0, 0.0)
    private var edPnt: Utils.Point2D = Utils.Point2D(0.0, 0.0)
    private val linePaint = Paint()
    private lateinit var addIimageView: ImageView
    private lateinit var colorIimageView: ImageView
    private var addOut: Boolean = false
    private lateinit var editText: EditText
    private var colorOut: Boolean = false
    private var resize: Boolean = false
    private var lastOrientation: Int = 0
    private var landscape: Boolean = false

    /**
     * main view
     */
    private inner class NodeView(context: Context) : ViewGroup(context) {

        // draw items
        override fun dispatchDraw(canvas: Canvas) {
            drawBack(canvas)
            drawItems(canvas)
            drawScrollBars(canvas)
            drawLine(canvas)

            super.dispatchDraw(canvas)
        }

        override fun onLayout(b: Boolean, i: Int, i1: Int, i2: Int, i3: Int) {
            // main
            val child = this.getChildAt(0)
            child.layout(0, 0, displayWidth, displayHeight);
            //child?.layout(bottomViewRect.left, bottomViewRect.top, bottomViewRect.right, bottomViewRect.bottom)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(displayWidth, displayHeight)
            // main
            val child = this.getChildAt(0)
            if (child != null)
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
    }

    /**
     * initialize app
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        displayWidth = size.x
        displayHeight = size.y

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        if (!back.equals("")) {
            val resId = resources.getIdentifier(back, "drawable", packageName)
            backBitmap = BitmapFactory.decodeResource(resources, resId)
            backDrawable = BitmapDrawable(backBitmap)
            backDrawable?.setBounds(0, 0, backBitmap.width, backBitmap.height)
        }

        nodeView = NodeView(this)
        setContentView(nodeView)

        ui = TouchUI(this)

        val mainView = layoutInflater.inflate(R.layout.activity_main, null) as RelativeLayout
        nodeView.addView(mainView)

        nodeClass = NodeClass(this)

        deskWidth = DESKTOP
        deskHeight = DESKTOP
        scaleView = ScaleView(displayWidth, displayHeight, deskWidth, deskHeight, nodeView)

        addIimageView = findViewById<ImageView>(R.id.addImageView)
        /*imageView.setOnClickListener {
            Log.i(TAG, "add start")
            nodeClass?.addNewNode()
        }*/
        addIimageView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                addOut = !addOut
                moveAddView(addOut, false)
            }
        })

        val delBtn = findViewById<ImageView>(R.id.removeImageView)
        delBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if(selItem != null) {
                    nodeClass?.deleteNode(selItem!!)
                    selItem = null
                }
                if(selLink != null) {
                    nodeClass?.deleteLink(selLink!!)
                    selLink = null
                }
            }
        })

        editText = findViewById(R.id.editText)
        editText.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                // If the event is a key-down event on the "enter" button
                if (event.getAction() === KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    // Perform action on key press
                    if (v != null) {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm!!.hideSoftInputFromWindow(v.windowToken, 0)
                    }

                    selItem?.setNodeText((v as EditText).text.toString())
                    selItem?.mode = Node.NORMAL
                    selItem = null

                    editText?.visibility = View.GONE
                }

                return false
            }
        })
        val editBtn = findViewById<ImageView>(R.id.editImageView)
        editBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if(selItem != null) {
                    if (editText?.visibility == View.GONE) {
                        editText.text = Editable.Factory.getInstance().newEditable(selItem!!.text)
                        editText.visibility = View.VISIBLE
                    }
                    else {
                        editText?.visibility = View.GONE
                    }
                }
            }
        })

        colorIimageView = findViewById<ImageView>(R.id.colorImageView)
        colorIimageView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                colorOut = !colorOut
                moveAddView(colorOut, true)
            }
        })

        scrollPaint1.color = Color.GRAY
        scrollPaint1.isAntiAlias = true
        scrollPaint1.isDither = true
        scrollPaint1.style = Paint.Style.STROKE
        scrollPaint1.strokeJoin = Paint.Join.ROUND
        scrollPaint1.strokeCap = Paint.Cap.ROUND
        scrollPaint1.strokeWidth = SCROLL_SIZE

        scrollPaint2.color = 0xFF4AE2E7.toInt()
        scrollPaint2.isAntiAlias = true
        scrollPaint2.isDither = true
        scrollPaint2.style = Paint.Style.STROKE
        scrollPaint2.strokeJoin = Paint.Join.ROUND
        scrollPaint2.strokeCap = Paint.Cap.ROUND
        scrollPaint2.strokeWidth = SCROLL_SIZE

        barPaint.color = Color.GRAY
        barPaint.isAntiAlias = true
        barPaint.isDither = true
        barPaint.style = Paint.Style.STROKE
        barPaint.strokeJoin = Paint.Join.ROUND
        barPaint.strokeCap = Paint.Cap.ROUND
        barPaint.strokeWidth = SCROLL_SIZE

        linePaint.color = Color.LTGRAY
        linePaint.isAntiAlias = true
        linePaint.isDither = true
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeJoin = Paint.Join.ROUND
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeWidth = SCROLL_SIZE

        val settings = getSharedPreferences("org.landroo.node_preferences", Context.MODE_PRIVATE)
        val nodes = settings.getString("nodelist", "")
        val links = settings.getString("linklist", "")
        nodeClass?.addNodes(nodes, links)
    }

    /**
     * go to sleep
     */
    public override fun onPause() {
        paused = true

        scrollTimer?.cancel()
        scrollTimer?.purge()

        scaleView?.stopTimer()

        val settings = getSharedPreferences("org.landroo.node_preferences", Context.MODE_PRIVATE)
        val editor = settings.edit()
        val nodeList = nodeClass?.nodeList()
        editor.putString("nodelist", nodeList)
        val linkList = nodeClass?.linkList()
        editor.putString("linklist", linkList)
        editor.apply()

        //Log.i(TAG, "paused");
        super.onPause()
    }

    /**
     * app entry point
     */
    override fun onResume() {
        paused = false

        scrollTimer = Timer()
        scrollTimer.scheduleAtFixedRate(ScrollTask(), 0, SCROLL_INTERVAL)

        scaleView?.startTimer()

        //Log.i(TAG, "resumed");
        super.onResume()
    }

    /**
     * handle the screen dimension changed
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        if (newConfig.orientation === Configuration.ORIENTATION_LANDSCAPE) {
            //Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show()
            landscape = true
        }
        else if (newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {
            //Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show()
            landscape = false
        }

        // Reinitialize UI when orientation is changed
        if (newConfig.orientation !== lastOrientation){
            lastOrientation = newConfig.orientation

            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            displayWidth = size.x
            displayHeight = size.y

            deskWidth = DESKTOP
            deskHeight = DESKTOP
            scaleView = ScaleView(displayWidth, displayHeight, deskWidth, deskHeight, nodeView)
        }
    }

    /**
     * touch event redirect to touch ui class
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return ui.tapEvent(event)
    }

    /**
     * on finger down
     */
    override fun onDown(x: Double, y: Double) {
        afterMove = false
        scrollAlpha = SCROLL_ALPHA

        scaleView.onDown(x, y)

        sX = x / zoomX
        sY = y / zoomY

        mX = x / zoomX
        mY = y / zoomY

        xPos = scaleView.xPos
        yPos = scaleView.yPos

        scrollBar = checkBars(x, y)
        if (scrollBar === 1) {
            barPosX = x - barPosX
        } else if (scrollBar === 2) {
            barPosY = y - barPosY
        }
        else {
            if(selItem?.mode == Node.EDIT) {

            }
            else {
                selItem = nodeClass?.selectNode(x - xPos, y - yPos, zoomX, zoomY)
                selItem?.mode = Node.MOVE
            }
        }
    }

    /**
     * on up
     */
    override fun onUp(x: Double, y: Double) {
        scaleView.onUp(x, y)

        scrollBar = 0

        longPress = 0

        if(selItem?.mode == Node.MOVE) {
            afterMove = true
        }

        if(selItem?.mode == Node.EDIT) {

            var item = nodeClass?.selectNode(x - xPos, y - yPos, zoomX, zoomY)
            if(item != null) {
                nodeClass?.linkNodes(selItem!!, item)
            }

            stPnt.empty()
            edPnt.empty()

            afterMove = true
        }

        if(addOut) {
            val s = addIimageView.drawable.bounds.height()
            val t = addIimageView.top
            val l = addIimageView.left
            for(i in 1..7) {
                //Log.i(TAG, "" + i + " " + y + " " + (t - i * s) + " " + (t - i * s + s) + " " + x + " " + l + " " + (l + s))
                if(x > l && x < l + s && y > t - i * s && y < t - i * s + s) {
                    addOut = false
                    moveAddView(addOut, false)

                    nodeClass?.addNewNode((xPos * -1 + displayWidth / 2 - 100) / zoomX, (yPos * -1 + displayHeight / 2 - 50) / zoomY, i)

                    break
                }
            }
        }

        if(colorOut) {
            val s = colorIimageView.drawable.bounds.height()
            val t = colorIimageView.top
            val l = colorIimageView.left
            for(i in 1..7) {
                if(x > l && x < l + s && y > t - i * s && y < t - i * s + s) {
                    colorOut = false
                    moveAddView(colorOut, true)

                    var color = Color.WHITE
                    when(i) {
                        1 -> color = Color.RED
                        2 -> color = Color.BLUE
                        3 -> color = Color.GREEN
                        4 -> color = Color.CYAN
                        5 -> color = Color.MAGENTA
                        6 -> color = Color.YELLOW
                    }

                    selItem?.color = color
                    selItem?.mode = Node.NORMAL
                    selItem = null

                    break
                }
            }
        }

        selItem?.mode = Node.NORMAL
        selItem = null

        editText?.visibility = View.GONE

        resize = false
    }

    /**
     * on tap
     */
    override fun onTap(x: Double, y: Double) {
        Log.i(TAG, "onTap")
        scrollAlpha = SCROLL_ALPHA
        longPress = 60
        scrollBar = 0

        xPos = scaleView.xPos
        yPos = scaleView.yPos

        if(selItem == null) {
            selItem = nodeClass?.selectNode(x - xPos, y - yPos, zoomX, zoomY)
            if(selItem != null) {
                selItem?.mode = Node.EDIT
                stPnt.x = (selItem?.px!! + selItem?.width!! / 2) * zoomX + xPos
                stPnt.y = (selItem?.py!! + selItem?.height!! / 2) * zoomY + yPos
            }
        }
        else if(selItem?.mode == Node.EDIT) {
            selItem?.mode = Node.NORMAL
        }
        else {
            selItem?.mode = Node.EDIT
        }

        if(selLink == null) {
            selLink = nodeClass?.selectLink(x - xPos, y - yPos, zoomX, zoomY)
            if(selLink != null) {
                selLink?.selected = true
            }
        }
        else {
            selLink?.selected = false
            selLink = null
        }

    }

    /**
     * on hold
     */
    override fun onHold(x: Double, y: Double) {
    }

    /**
     * on move
     */
    override fun onMove(x: Double, y: Double) {

        scrollAlpha = SCROLL_ALPHA

        mX = x / zoomX
        mY = y / zoomY

        val dx = mX - sX
        val dy = mY - sY

        if (scrollBar !== 0) {
            // vertical scroll
            if (scrollBar === 1) {
                val xp = -(x - barPosX) / (displayWidth / (deskWidth * zoomX))
                //Log.i(TAG, "" + xp)
                if (xp < 0 && xp > displayWidth - deskWidth * zoomX) {
                    xPos = xp
                }
            } else {
                val yp = -(y - barPosY) / (displayHeight / (deskHeight * zoomY))
                //Log.i(TAG, "" + yp)
                if (yp < 0 && yp > displayHeight - deskHeight * zoomY) {
                    yPos = yp
                }
            }
            scaleView.setPos(xPos, yPos)
            nodeView.postInvalidate()
        }
        else if (selItem != null) {
            if (selItem?.mode == Node.MOVE) {
                selItem?.movePos(dx, dy)
            }
            else if (selItem?.mode == Node.EDIT) {
                if(resize || selItem?.resized(x - xPos, y - yPos, zoomX, zoomY)!!) {
                    selItem?.resize(dx, dy)
                    resize = true
                }
                else if(!resize){
                    edPnt.x = x
                    edPnt.y = y
                }
            }

            nodeView.postInvalidate()
        }
        else {
            scaleView.onMove(x, y)
        }

        sX = mX
        sY = mY
    }

    /**
     * double tap
     */
    override fun onDoubleTap(x: Double, y: Double) {

        when(back) {
            "" -> back = "grid"
            "grid" -> back = "grid1"
            "grid1" -> back = "grid2"
            "grid2" -> back = ""
        }

        if (!back.equals("")) {
            val resId = resources.getIdentifier(back, "drawable", packageName)
            backBitmap = BitmapFactory.decodeResource(resources, resId)
            backDrawable = BitmapDrawable(backBitmap)
            backDrawable?.setBounds(0, 0, backBitmap.width, backBitmap.height)
            staticBack = false
        }
        else {
            staticBack = true
            backDrawable = null
        }

        nodeView.postInvalidate()
    }

    /**
     * on swipe
     */
    override fun onSwipe(direction: Int, velocity: Double, x1: Double, y1: Double, x2: Double, y2: Double) {
        longPress = 0

        if (!afterMove)
            scaleView.onSwipe(direction, velocity, x1, y1, x2, y2)
    }

    /**
     * on zoom
     */
    override fun onZoom(mode: Int, x: Double, y: Double, distance: Double, xDiff: Double, yDiff: Double) {
        longPress = 0

        scaleView.onZoom(mode, x, y, distance, xDiff, yDiff)

        zoomX = scaleView.zoomX
        zoomY = scaleView.zoomY
    }

    /**
     * on rotate
     */
    override fun onRotate(mode: Int, x: Double, y: Double, angle: Double) {
    }

    /**
     * more finger touch
     */
    override fun onFingerChange() {
    }

    /**
     * draw background
     */
    private fun drawBack(canvas: Canvas) {
        if (backDrawable != null) {
            // static back or tiles
            if (staticBack) {
                if (backDrawable != null) {
                    backDrawable?.setBounds(0, 0, displayWidth, displayHeight)
                    backDrawable?.draw(canvas)
                } else {
                    canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
                }
            }
            else {
                if (scaleView != null) {
                    xPos = scaleView.xPos
                    yPos = scaleView.yPos
                }
                var x = 0.0
                while (x < deskWidth) {
                    var y = 0.0
                    while (y < deskHeight) {
                        // distance of the tile center from the rotation center
                        val dis = Utils().getDist(rx * zoomX, ry * zoomY, (x + tileSize / 2) * zoomX, (y + tileSize / 2) * zoomY)
                        // angle of the tile center from the rotation center
                        val ang = Utils().getAng(rx * zoomX, ry * zoomY, (x + tileSize / 2) * zoomX, (y + tileSize / 2) * zoomY)

                        // coordinates of the block after rotation
                        val cx = dis * Math.cos((rotation + ang) * Utils().DEGTORAD) + rx * zoomX + xPos
                        val cy = dis * Math.sin((rotation + ang) * Utils().DEGTORAD) + ry * zoomY + yPos

                        if (cx >= -tileSize && cx <= displayWidth + tileSize && cy >= -tileSize && cy <= displayHeight + tileSize) {
                            backDrawable?.setBounds(0, 0, (tileSize * zoomX).toInt() + 1, (tileSize * zoomY).toInt() + 1)

                            canvas.save()
                            canvas.rotate(rotation.toFloat(), (rx * zoomX + xPos).toFloat(), (ry * zoomY + yPos).toFloat())
                            canvas.translate((x * zoomX + xPos).toFloat(), (y * zoomY + yPos).toFloat())
                            backDrawable?.draw(canvas)
                            canvas.restore()
                        }
                        y += tileSize
                    }
                    x += tileSize
                }
            }
        }
        else {
            canvas.drawColor(backColor)
        }
    }

    /**
     * draw cells and resize bars
     */
    private fun drawItems(canvas: Canvas) {
        if (scaleView != null) {
            xPos = scaleView.xPos
            yPos = scaleView.yPos
        }

        nodeClass?.drawLinks(canvas, xPos, yPos, zoomX, zoomY, displayWidth, displayHeight)
        nodeClass?.drawNodes(canvas, xPos, yPos, zoomX, zoomY, displayWidth, displayHeight)
    }

    /**
     * draw scroll bars
     * @param canvas    Canvas canvas
     */
    private fun drawScrollBars(canvas: Canvas) {
        val xSize = displayWidth / (deskWidth * zoomX / displayWidth)
        val ySize = displayHeight / (deskHeight * zoomY / displayHeight)
        var x = displayWidth / (deskWidth * zoomX) * -xPos
        var y = displayHeight - SCROLL_SIZE - 2.0
        if (xSize < displayWidth) {
            if (scrollBar === 1) {
                canvas.drawLine(x.toFloat(), y.toFloat(), (x + xSize).toFloat(), y.toFloat(), scrollPaint2)
            } else {
                canvas.drawLine(x.toFloat(), y.toFloat(), (x + xSize).toFloat(), y.toFloat(), scrollPaint1)
            }
        }

        x = displayWidth - SCROLL_SIZE - 2.0
        y = displayHeight / (deskHeight * zoomY) * -yPos
        if (ySize < displayHeight) {
            if (scrollBar === 2) {
                canvas.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + ySize).toFloat(), scrollPaint2)
            } else {
                canvas.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + ySize).toFloat(), scrollPaint1)
            }
        }
    }

    /**
     * draw linker line
     */
    fun drawLine(canvas: Canvas) {
        if(!stPnt.isempty() && !edPnt.isempty()) {
            canvas.drawLine(stPnt.x.toFloat(), stPnt.y.toFloat(), edPnt.x.toFloat(), edPnt.y.toFloat(), linePaint)
        }
    }

    /**
     * scroll timer
     */
    internal inner class ScrollTask : TimerTask() {
        override fun run() {
            if (paused)
                return

            if (longPress < 50) {
                longPress++
            }

            if (scrollAlpha > 32) {
                scrollAlpha--
                if (scrollAlpha > 255)
                    scrollPaint1.alpha = 255
                else
                    scrollPaint1.alpha = scrollAlpha
                nodeView.postInvalidate()
            }
        }
    }

    /**
     * check scroll bar tap
     */
    private fun checkBars(x: Double, y: Double): Int {
        val xSize = displayWidth / (deskWidth * zoomX / displayWidth)
        val ySize = displayHeight / (deskHeight * zoomY / displayHeight)
        var px = displayWidth / (deskWidth * zoomX) * -xPos
        var py = displayHeight - SCROLL_SIZE - 2.0
        //Log.i(TAG, "" + x + " " + xp + " " + (x+ xSize) + " " + y + " " + yp + " " + (y + SCROLL_SIZE));
        if (x > px && y > py - GAP && x < px + xSize && y < py + SCROLL_SIZE + GAP && xSize < displayWidth) {
            barPosX = px
            return 1
        }

        px = displayWidth - SCROLL_SIZE - 2.0
        py = displayHeight / (deskHeight * zoomY) * -yPos
        if (x > px - GAP && y > py && x < px + SCROLL_SIZE + GAP && y < py + ySize && ySize < displayHeight) {
            barPosY = py
            return 2
        }

        return 0
    }

    /**
     * resize virtual desktop
     */
    private fun resizeDesk(newW: Int, newH: Int) {
        deskWidth = newW
        deskHeight = newH
        scaleView.setSize(displayWidth, displayHeight, deskWidth, deskHeight)
        scaleView.setPos(xPos, yPos)
        scaleView.setZoom(zoomX, zoomY)
        nodeView.postInvalidate()
    }

    /**
     * animate buttons
     */
    fun moveAddView(out: Boolean, color: Boolean) {

        var image: ImageView = if(color)
            findViewById(R.id.redImageView)
        else
            findViewById(R.id.rectImageView)
        if(out) {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.moverectout)
            image.startAnimation(animation1)
        }
        else {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.moverectin)
            image.startAnimation(animation1)
        }
        image = if(color)
            findViewById(R.id.blueImageView)
        else
            findViewById(R.id.circImageView)
        if(out) {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movecircout)
            image.startAnimation(animation1)
        }
        else {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movecircin)
            image.startAnimation(animation1)
        }
        image = if(color)
            findViewById(R.id.greenImageView)
        else
            findViewById(R.id.rombImageView)
        if(out) {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.moverombout)
            image.startAnimation(animation1)
        }
        else {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.moverombin)
            image.startAnimation(animation1)
        }
        image = if(color)
            findViewById(R.id.cyanImageView)
        else
            findViewById(R.id.tri1ImageView)
        if(out) {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movetri1out)
            image.startAnimation(animation1)
        }
        else {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movetri1in)
            image.startAnimation(animation1)
        }
        image = if(color)
            findViewById(R.id.magentaImageView)
        else
            findViewById(R.id.tri2ImageView)
        if(out) {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movetri2out)
            image.startAnimation(animation1)
        }
        else {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movetri2in)
            image.startAnimation(animation1)
        }
        image = if(color)
            findViewById(R.id.yellowImageView)
        else
            findViewById(R.id.tri3ImageView)

        if(out) {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movetri3out)
            image.startAnimation(animation1)
        }
        else {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movetri3in)
            image.startAnimation(animation1)
        }
        image = if(color)
            findViewById(R.id.whiteImageView)
        else
            findViewById(R.id.tri4ImageView)

        if(out) {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movetri4out)
            image.startAnimation(animation1)
        }
        else {
            val animation1 = AnimationUtils.loadAnimation(applicationContext, R.anim.movetri4in)
            image.startAnimation(animation1)
        }
    }
}


