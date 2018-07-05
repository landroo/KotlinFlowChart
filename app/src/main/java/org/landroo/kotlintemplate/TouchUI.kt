package org.landroo.kotlintemplate

import android.view.MotionEvent
import java.util.*
import android.R.attr.y
import android.R.attr.x
import android.R.attr.action
import android.text.method.TextKeyListener.clear



//
class TouchUI(context: TouchInterface) {

    private val TAG: String = "TouchUI"
    private val SWIPE_MIN_DISTANCE: Double = 20.0
    private val SWIPE_MAX_OFF_PATH: Double = 250.0
    private val SWIPE_THRESHOLD_VELOCITY: Double = 20.0
    private val TAP_TIME: Int = 400
    private val HOLD_TIME: Int = 800
    private val DOUBLE_TAP_TIME: Int = 250
    private val TAP_TRESHOLD: Int = 5

    private var lastX1: Double = 0.0
    private var lastY1: Double = 0.0
    private var lastX2: Double = 0.0
    private var lastY2: Double = 0.0
    private var moveX: Double = 0.0
    private var moveY: Double = 0.0
    private var lastTime: Long = 0
    private var dubleTime: Long = 0
    private var eventMode: Int = 0

    var isDouble: Boolean = false

    private var holdTimer: Timer = Timer()
    private var holdTime: Double = 0.0
    private var isHold: Boolean = false

    internal inner class HoldTask: TimerTask() {
        override fun run() {
            if(isHold) {
                holdTime++
                if(holdTime == 1500.0) {
                    callBack.onHold(lastX1, lastY1)
                }
            }
        }
    }

    inner class Finger {
        var id: Int = 0
        var action: Int = 0
        var x: Double = 0.0
        var y: Double = 0.0

        fun clear() {
            action = 0
            x = 0.0
            y = 0.0
        }
    }

    var fingers: Array<Finger> = Array(10) {Finger()}

    private var callBack: TouchInterface = context

    init {
        for(i in 0..9) {
            fingers[i] = Finger()
        }
        holdTimer.scheduleAtFixedRate(HoldTask(), 0, 1)
    }

    fun tapEvent(event: MotionEvent): Boolean {
        for (i in 0..9) fingers[i].clear()
        for (id in 0 until event.pointerCount) {
            val x = event.getX(id).toDouble()
            val y = event.getY(id).toDouble()

            isDouble = false
            // second finger
            if (id == 1) isDouble = processSecTap(x, y, event.action, id)

            // first finger
            if (id == 0 && !isDouble) processTap(x, y, event.action, id)

            fingers[id].id = event.getPointerId(event.actionIndex)
            fingers[id].action = event.action and MotionEvent.ACTION_MASK
            fingers[id].x = x
            fingers[id].y = y
        }

        this.callBack.onFingerChange()

        return false
    }

    private fun processTap(x1: Double, y1: Double, action: Int, id: Int):Boolean {
        var x2 = 0.0
        var y2 = 0.0
        var deltaTime: Long = 0
        var doubleDelta: Long = 0
        var now = Date()
        var eventType = 0
        var distance = 0.0
        var velocity = 0.0

        when(action) {
            MotionEvent.ACTION_DOWN -> {
                lastX1 = x1
                lastY1 = y1
                lastTime = now.time
                eventType = 1
                callBack.onDown(x1, y1)
            }
            MotionEvent.ACTION_MOVE -> {
                moveX = x1
                moveY = y1
                eventType = 2
                callBack.onMove(x1, y1)
            }
            MotionEvent.ACTION_UP -> {
                deltaTime = now.time - lastTime;
                doubleDelta = now.time - dubleTime;
                x2 = lastX1
                y2 = lastY1
                eventType = 6
                callBack.onUp(x1, y1)
                if(deltaTime < TAP_TIME) {
                    eventType = 3
                    dubleTime = now.time
                }
                if(deltaTime > HOLD_TIME
                && x1 + TAP_TRESHOLD > lastX1 && x1 - TAP_TRESHOLD < lastX1
                && y1 + TAP_TRESHOLD > lastY1 && y1 - TAP_TRESHOLD < lastY1) {
                    eventType = 4
                }
                if(doubleDelta < DOUBLE_TAP_TIME
                && x1 + TAP_TRESHOLD > lastX1 && x1 - TAP_TRESHOLD < lastX1
                && y1 + TAP_TRESHOLD > lastY1 && y1 - TAP_TRESHOLD < lastY1) {
                    eventType = 5
                }
            }
        }

        if(deltaTime > 0) {
            distance = Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1))
            velocity = (distance / deltaTime) * 100

            if (Math.abs(y1 - y2) < SWIPE_MAX_OFF_PATH) {
                if (x2 - x1 > SWIPE_MIN_DISTANCE && velocity > SWIPE_THRESHOLD_VELOCITY) {
                    eventType = 7
                } else if (x1 - x2 > SWIPE_MIN_DISTANCE && velocity > SWIPE_THRESHOLD_VELOCITY) {
                    eventType = 8
                }
            }

            if (Math.abs(x1 - x2) < SWIPE_MAX_OFF_PATH) {
                if (y1 - y2 > SWIPE_MIN_DISTANCE && velocity > SWIPE_THRESHOLD_VELOCITY) {
                    eventType = 9
                } else if (y2 - y1 > SWIPE_MIN_DISTANCE && velocity > SWIPE_THRESHOLD_VELOCITY) {
                    eventType = 10
                }
            }
        }

        when(eventType) {
            0 -> { // nothing
            }
            1 -> { // on down
                holdTime = 0.0
                isHold = true
            }
            2 -> { // on move
            }
            3 -> {
                callBack.onTap(x1, y1)
                isHold = false
            }
            4 -> { // on hold
                callBack.onHold(x1, y1)
            }
            5 -> { // on double tap
                callBack.onDoubleTap(x1, y1)
            }
            6 -> { // on up
                isHold = false
            }
            7 -> { // on swipe right to left
                callBack.onSwipe(1, velocity, x1, y1, x2, y2)
            }
            8 -> { // on swipe left to right
                callBack.onSwipe(2, velocity, x1, y1, x2, y2)
            }
            9 -> { // on swipe up to down
                callBack.onSwipe(3, velocity, x1, y1, x2, y2)
            }
            10 -> { // on swipe down to up
                callBack.onSwipe(4, velocity, x1, y1, x2, y2)
            }
        }

        return true
    }

    private fun processSecTap(x1: Double, y1: Double, action: Int, id: Int): Boolean {
        var x2 = lastX1
        var y2 = lastY1

        lastX2 = x1
        lastY2 = y1

        eventMode = 0

        if (action == MotionEvent.ACTION_POINTER_1_DOWN || action == MotionEvent.ACTION_POINTER_2_DOWN) {
            eventMode = 1
        }
        if (action == MotionEvent.ACTION_MOVE) {
            eventMode = 2
        }
        if (action == MotionEvent.ACTION_POINTER_1_UP || action == MotionEvent.ACTION_POINTER_2_UP) {
            eventMode = 3
        }

        //Log.i("UI", "Mode: " + eventMode + " x=" + x1 + " y=" + y1);

        if (eventMode !== 0) {
            val rotate = Utils().getAng(x2, y2, x1, y1)
            this.callBack.onRotate(eventMode, x1, y1, rotate)

            val distance = Utils().getDist(x1, y1, x2, y2)
            this.callBack.onZoom(eventMode, x1, y1, distance, x2 - x1, y2 - y1)

            return true
        }

        return false
    }
}