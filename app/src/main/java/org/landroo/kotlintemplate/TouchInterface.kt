package org.landroo.kotlintemplate

interface TouchInterface {
    fun onDown(x: Double, y: Double)
    fun onUp(x: Double, y: Double)
    fun onTap(x: Double, y: Double)
    fun onHold(x: Double, y: Double)
    fun onMove(x: Double, y: Double)
    fun onDoubleTap(x: Double, y: Double)
    fun onSwipe(direction: Int, velocity: Double, x1: Double, y1: Double, x2:Double, y2:Double)
    fun onZoom(mode: Int, x: Double, y: Double, distance: Double, xDiff: Double, yDiff: Double)
    fun onRotate(mode: Int, x: Double, y: Double, angle: Double)
    fun onFingerChange()
}
