package org.landroo.kotlintemplate

import android.view.View
import java.util.*

class ScaleView(displayWidth: Int, displayHeight: Int, pictureWidth: Int, pictureHeight: Int, view: View): TouchInterface {

    private val TAG = ScaleView::class.java.simpleName
    private val SWIPE_INTERVAL: Long = 10

    private var displayWidth = displayWidth
    private var displayHeight = displayHeight
    private var pictureWidth: Double = pictureWidth.toDouble()
    private var pictureHeight: Double = pictureHeight.toDouble()
    private var origWidth: Double = pictureWidth.toDouble()
    private var origHeight: Double = pictureHeight.toDouble()

    var xPos = 0.0
    var yPos = 0.0

    private var sX = 0.0
    private var sY = 0.0
    private var mX = 0.0
    private var mY = 0.0

    private var swipeTimer: Timer = Timer()
    private var swipeDistX = 0.0
    private var swipeDistY = 0.0
    private var swipeVelocity = 0.0
    private var swipeSpeed = 0.0

    private var backSpeedX = 0.0
    private var backSpeedY = 0.0
    private val offMarginX = displayWidth / 8
    private val offMarginY = displayHeight / 8

    private var zoomSize = 0.0
    var zoomX = 1.0
    var zoomY = 1.0

    private val view: View = view

    internal inner class SwipeTask : TimerTask() {
        override fun run() {
            var redraw = false
            if (swipeVelocity > 0) {
                val dist = Math.sqrt(swipeDistY * swipeDistY + swipeDistX * swipeDistX)
                val x = xPos - (swipeDistX / dist * (swipeVelocity / 10))
                val y = yPos - (swipeDistY / dist * (swipeVelocity / 10))

                if (pictureWidth > displayWidth && (x < displayWidth - pictureWidth || x > 0) || pictureWidth <= displayWidth && (x > displayWidth - pictureWidth || x < 0)) {
                    swipeDistX *= -1
                    swipeSpeed += .1
                }

                if (pictureHeight > displayHeight && (y < displayHeight - pictureHeight || y > 0) || pictureHeight <= displayHeight && (y > displayHeight - pictureHeight || y < 0)) {
                    swipeDistY *= -1
                    swipeSpeed += .1
                }

                xPos -= (swipeDistX / dist * (swipeVelocity / 10))
                yPos -= (swipeDistY / dist * (swipeVelocity / 10))

                swipeVelocity -= swipeSpeed
                swipeSpeed += .0001

                redraw = true

                if (swipeVelocity <= 0) checkOff()
            }

            if (backSpeedX !== 0.0) {
                if (backSpeedX < 0 && xPos <= 0.1f || backSpeedX > 0 && xPos + 0.1f >= displayWidth - pictureWidth)
                    backSpeedX = 0.0
                else if (backSpeedX < 0)
                    xPos -= xPos / 20
                else
                    xPos += (displayWidth - (pictureWidth + xPos)) / 20

                redraw = true
            }

            if (backSpeedY !== 0.0) {
                if (backSpeedY < 0 && yPos <= 0.1f || backSpeedY > 0 && yPos + 0.1f >= displayHeight - pictureHeight)
                    backSpeedY = 0.0
                else if (backSpeedY < 0)
                    yPos -= yPos / 20
                else
                    yPos += (displayHeight - (pictureHeight + yPos)) / 20

                redraw = true
            }

            if (redraw) {
                view.postInvalidate()
            }

            return
        }
    }

    init {

        // set cnter
        xPos = (displayWidth - pictureWidth).toDouble() / 2
        yPos = (displayHeight - pictureHeight).toDouble() / 2

        startTimer()
    }

    fun startTimer() {
        swipeTimer = Timer()
        swipeTimer.scheduleAtFixedRate(SwipeTask(), 0, SWIPE_INTERVAL)
    }

    private fun checkOff() {
        if (pictureWidth >= displayWidth) {
            if (xPos > 0 && xPos <= offMarginX)
                backSpeedX = -1.0
            else if (xPos < pictureWidth - offMarginX && xPos <= pictureWidth) backSpeedX = 1.0
        }
        if (pictureHeight >= displayHeight) {
            if (yPos > 0 && yPos <= offMarginY)
                backSpeedY = -1.0
            else if (yPos < pictureHeight - offMarginY && yPos <= pictureHeight) backSpeedY = 1.0
        }
    }

    fun setPos(x: Double, y: Double) {
        xPos = x
        yPos = y
    }

    fun setZoom(zx: Double, zy: Double) {
        zoomX = zx
        zoomY = zy

        pictureWidth *= zoomX
        pictureHeight *= zoomY
    }

    fun stopTimer() {
        if (swipeTimer != null) {
            swipeTimer.cancel()
            swipeTimer.purge()
        }
    }

    fun setSize(displayWidth: Int, displayHeight: Int, pictureWidth: Int, pictureHeight: Int) {
        this.displayWidth = displayWidth
        this.displayHeight = displayHeight
        this.pictureWidth = pictureWidth.toDouble()
        this.pictureHeight = pictureHeight.toDouble()

        origWidth = pictureWidth.toDouble()
        origHeight = pictureHeight.toDouble()
    }

    override fun onDown(x: Double, y: Double) {
        sX = x
        sY = y

        swipeVelocity = 0.0

        view.postInvalidate()
    }

    override fun onUp(x: Double, y: Double) {
        checkOff()
        view.postInvalidate()
    }

    override fun onTap(x: Double, y: Double) {
    }

    override fun onHold(x: Double, y: Double) {
    }

    override fun onMove(x: Double, y: Double) {
        mX = x
        mY = y

        var dx = mX - sX
        var dy = mY - sY

        if (pictureWidth > displayWidth) {
            if (xPos + dx < displayWidth - (pictureWidth + offMarginX) || xPos + dx > offMarginX) {
                dx = 0.0
            }
        } else {
            if (xPos + dx > displayWidth - pictureWidth || xPos + dx < 0) {
                dx = 0.0
            }
        }

        if (pictureHeight > displayHeight) {
            if (yPos + dy < displayHeight - (pictureHeight + offMarginY) || yPos + dy > offMarginY) {
                dy = 0.0
            }
        } else {
            if (yPos + dy > displayHeight - pictureHeight || yPos + dy < 0) {
                dy = 0.0
            }
        }

        xPos += dx
        yPos += dy

        sX = mX
        sY = mY

        view.postInvalidate()
    }

    override fun onDoubleTap(x: Double, y: Double) {
        swipeVelocity = 0.0

        backSpeedX = 0.0
        backSpeedY = 0.0

        pictureWidth = origWidth
        pictureHeight = origHeight

        zoomX = 1.0
        zoomY = 1.0

        view.postInvalidate()
    }

    override fun onSwipe(direction: Int, velocity: Double, x1: Double, y1: Double, x2: Double, y2: Double) {

        swipeDistX = x2 - x1
        swipeDistY = y2 - y1
        swipeSpeed = 1.0
        swipeVelocity = velocity

        view.postInvalidate()
    }

    override fun onZoom(mode: Int, x: Double, y: Double, distance: Double, xDiff: Double, yDiff: Double) {
        // zoom speed
        var dist = distance * (pictureHeight / displayHeight)
        if (pictureHeight < pictureWidth)
            dist = distance * (pictureWidth / displayWidth)
        //float dist = distance * 200;
        //int dist = (int) distance * 8;
        when (mode) {
            1 -> zoomSize = dist
            2 -> {
                val diff = dist - zoomSize
                val sizeNew = Math.sqrt(pictureWidth * pictureWidth + pictureHeight * pictureHeight)
                val sizeDiff = 100f / (sizeNew / (sizeNew + diff))
                val newSizeX = pictureWidth * sizeDiff / 100f
                val newSizeY = pictureHeight * sizeDiff / 100f

                // zoom between min and max value
                if (newSizeX >= origWidth / 4.0 && newSizeX <= origWidth * 6.0) {
                    zoomX = pictureWidth / origWidth
                    zoomY = pictureHeight / origHeight

                    val diffX = newSizeX - pictureWidth
                    val diffY = newSizeY - pictureHeight
                    val xPer = 100.0 / (pictureWidth / (Math.abs(xPos) + mX)) / 100.0
                    val yPer = 100.0 / (pictureHeight / (Math.abs(yPos) + mY)) / 100.0

                    xPos -= diffX * xPer
                    yPos -= diffY * yPer

                    pictureWidth = newSizeX
                    pictureHeight = newSizeY

                    zoomSize = dist

                    //Log.i(TAG, "Math.abs(diffX * xPer - lastMidX) " + Math.abs(diffX * xPer - lastMidX));

                    if (pictureWidth > displayWidth || pictureHeight > displayHeight) {
                        if (xPos > 0) xPos = 0.0
                        if (yPos > 0) yPos = 0.0

                        if (xPos + pictureWidth < displayWidth) xPos = displayWidth - pictureWidth
                        if (yPos + pictureHeight < displayHeight) yPos = displayHeight - pictureHeight
                    } else {
                        if (xPos <= 0) xPos = 0.0
                        if (yPos <= 0) yPos = 0.0

                        if (xPos + pictureWidth > displayWidth) xPos = displayWidth - pictureWidth
                        if (yPos + pictureHeight > displayHeight) yPos = displayHeight - pictureHeight
                    }
                    // Log.i(TAG, "" + xPos + " " + yPos);
                }
            }
            3 -> zoomSize = 0.0
        }

        view.postInvalidate()
    }

    override fun onRotate(mode: Int, x: Double, y: Double, angle: Double) {
    }

    override fun onFingerChange() {
    }
}