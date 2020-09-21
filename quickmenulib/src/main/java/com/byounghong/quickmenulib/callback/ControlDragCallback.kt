package com.byounghong.quickmenulib.callback

import android.view.View

/**
 * #
 * @auther : byounghonglim
 * @since : 2019-07-08
 */

abstract class ControlDragCallback{
    open fun onViewDragStateChanged(state: Int) {}
    open fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {}
    open fun onViewCaptured(capturedChild: View, activePointerId: Int) {}
    open fun onViewReleased(releasedChild: View, xVal: Float, yVal: Float) {}
    open fun getViewVerticalDragRange(child: View) = 0
    abstract fun tryCaptureView(child: View): Boolean
    open fun clampViewPositionVertical(child: View, top: Int, dy: Int) = 0

}