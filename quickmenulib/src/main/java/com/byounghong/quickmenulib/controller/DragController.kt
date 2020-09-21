package com.byounghong.quickmenulib.controller

import android.content.Context
import android.view.*
import android.widget.Scroller
import com.byounghong.quickmenulib.callback.ControlDragCallback
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * #
 * @auther : byounghonglim
 * @since : 2019-07-08
 */

class DragController(private val parentView:ViewGroup, sensitivity:Float,
                     private val callback: ControlDragCallback) {

    private val mContext: Context = parentView.context
    private var mLastPointIndex:Int = -1

    companion object{
//        const val BASE_SETTLE_DURATION = 256
        const val BASE_SETTLE_DURATION = 500
        const val MAX_SETTLE_DURATION = 600
    }

    enum class DragState(var value: Int){
        STATE_IDLE(0),
        STATE_DRAGGING(1),
        STATE_SETTLING(2);
    }

    private var mPrevDragState = DragState.STATE_IDLE

    private val mViewConfiguration = ViewConfiguration.get(mContext)
    private val mTouchSlop = (mViewConfiguration.scaledTouchSlop*(1/sensitivity)).toInt()     // 의도하지 않는 Scrolling 처리
    private var mMaxVelocity:Float = mViewConfiguration.scaledMaximumFlingVelocity.toFloat()
    private var mMinVelocity:Float = mViewConfiguration.scaledMinimumFlingVelocity.toFloat()

    private var isChangedMotion = false

    private lateinit var mInitialMotionX: FloatArray
    private lateinit var mInitialMotionY: FloatArray
    private lateinit var mLastMotionX: FloatArray
    private lateinit var mLastMotionY: FloatArray

    private val mVelocityTracker by lazy {
        VelocityTracker.obtain()
    }

    //드래그의 속도를 측정
    private lateinit var mChildView: View
    private var isAvailChildView = false

    private var mScroller = Scroller(mContext)

    private fun captureChildView(childView: View, activePointerId: Int) {
        mChildView = childView
        isAvailChildView = true
        callback.onViewCaptured(childView, activePointerId)
        setDragState(DragState.STATE_DRAGGING)
    }

    fun abort() {
        clearMotionHistory()
        if (mPrevDragState == DragState.STATE_SETTLING) {
            val oldX = mScroller.currX
            val oldY = mScroller.currY
            mScroller.abortAnimation()
            val newX = mScroller.currX
            val newY = mScroller.currY
            callback.onViewPositionChanged(mChildView, newX, newY, newX - oldX, newY - oldY)
        }
        setDragState(DragState.STATE_IDLE)
    }

    fun destroy(){
        mVelocityTracker.recycle()
    }

    //settleSavedViewAt 동일하지만 VelocityTracker 값만 0
    fun smoothSlideViewTo(child: View, finalLeft: Int, finalTop: Int): Boolean {
        mChildView = child
        isAvailChildView = finalTop > 0

        return forceSettleSavedViewAt(finalLeft, finalTop, 0, 0)
    }


    // 드래그 하던 View(Captured View)를 놓았을 때, 해야할 일 정의
    // left, top 위치만 알려주면 예전의 위치로 돌아감
    fun settleSavedViewAt(finalLeft: Int, finalTop: Int){
        forceSettleSavedViewAt(finalLeft, finalTop,
                mVelocityTracker.getXVelocity(0).toInt(),
                mVelocityTracker.getYVelocity(0).toInt())
    }

    // startScroll 호출, computeScroll을 override해서 사용
    private fun forceSettleSavedViewAt(finalLeft: Int, finalTop: Int, xVal: Int, yVal: Int)
            : Boolean {
        val dx = finalLeft - mChildView.left
        val dy = finalTop - mChildView.top

        if (dx == 0 && dy == 0) {
            mScroller.abortAnimation()
            setDragState(DragState.STATE_IDLE)
            return false
        }

        mScroller.startScroll(mChildView.left, mChildView.top, dx, dy,
                computeSettleDuration(mChildView, dx, dy, xVal, yVal))

        setDragState(DragState.STATE_SETTLING)
        return true
    }

    private fun computeSettleDuration(child: View, dx: Int, dy: Int, x: Int, y: Int): Int {
        val xVal = clampMag(x, mMinVelocity.toInt(), mMaxVelocity.toInt())
        val yVal = clampMag(y, mMinVelocity.toInt(), mMaxVelocity.toInt())

        val absDx = abs(dx)
        val absDy = abs(dy)
        val absXVel = abs(xVal)
        val absYVel = abs(yVal)
        val addedVel = absXVel + absYVel
        val addedDistance = absDx + absDy

        val xWeight =
                if (xVal != 0) absXVel.toFloat() / addedVel
                else absDx.toFloat() / addedDistance
        val yWeight =
                if (yVal != 0) absYVel.toFloat() / addedVel
                else absDy.toFloat() / addedDistance

        val xDuration = computeAxisDuration(dx, xVal, 0)
        val yDuration = computeAxisDuration(dy, yVal, callback.getViewVerticalDragRange(child))

        return (xDuration * xWeight + yDuration * yWeight).toInt()
    }

    //축 길이 계산
    private fun computeAxisDuration(delta: Int, velocity: Int, motionRange: Int): Int {
        val speed = abs(velocity)

        if (delta == 0) return 0

        val width = parentView.width
        val halfWidth = width / 2
        val distanceRatio = min(1f, abs(delta).toFloat() / width)
        val distance = halfWidth + halfWidth * distanceInfluenceForSnapDuration(distanceRatio)

        val duration = if (speed > 0) {
            (4 * (1000 * abs(distance / speed)).roundToInt())
        } else {
            val range = abs(delta).toFloat() / motionRange
            ((range + 1) * BASE_SETTLE_DURATION).toInt()
        }
        return min(duration, MAX_SETTLE_DURATION)
    }

    //값이 최소값보다 낮으면 0으로 고정, 값이 최대값보다 크면 최대값으로 고정
    private fun clampMag(value: Int, absMin: Int, absMax: Int): Int {
        val absValue = abs(value)
        if (absValue < absMin) return 0
        return if (absValue > absMax) if (value > 0) absMax else -absMax else value
    }

    private fun clampMag(value: Float, absMin: Float, absMax: Float): Float {
        val absValue = abs(value)
        if (absValue < absMin) return 0f
        return if (absValue > absMax) if (value > 0) absMax else -absMax else value
    }

    //스탭 지속 시간에 대한 거리 영향
    private fun distanceInfluenceForSnapDuration(distanceRatio: Float): Float {
        var ratio = distanceRatio
        ratio -= 0.5f // center the values about 0.
        ratio *= (0.3f * Math.PI / 2.0f).toFloat()
        return sin(ratio.toDouble()).toFloat()
    }

    //현재 시간에 적절한 양만큼 캡쳐된 화면의 이동
    fun continueSettling(): Boolean {
        if (mPrevDragState == DragState.STATE_SETTLING) {
            var keepGoing = mScroller.computeScrollOffset()
            val x = mScroller.currX
            val y = mScroller.currY
            val dx = x - mChildView.left
            val dy = y - mChildView.top

            //드래그 상태가 잘못 됨
            if (!keepGoing && dy != 0) { //fix #525
                mChildView.top = 0
                return true
            }
            // offsetLeftAndRight 뷰가 움직이는 모습을 보여줌
            // setTranslateX/scrollTo는 움직이면서 사라지는 모습
            if (dx != 0) mChildView.offsetLeftAndRight(dx)
            if (dy != 0) mChildView.offsetTopAndBottom(dy)
            if (dx != 0 || dy != 0) callback.onViewPositionChanged(mChildView, x, y, dx, dy)

            //최대치 멈춤
            if (keepGoing && x == mScroller.finalX && y == mScroller.finalY) {
                mScroller.abortAnimation()
                keepGoing = mScroller.isFinished
            }

            if (!keepGoing) setDragState(DragState.STATE_IDLE)
        }
        return mPrevDragState == DragState.STATE_SETTLING
    }

    //모든 Callback 이벤트는 UI 스레드에서 발생. 그리고 release
    private fun dispatchViewReleased(xvel: Float, yvel: Float) {
        callback.onViewReleased(mChildView, xvel, yvel)
        isAvailChildView = false

        if (mPrevDragState == DragState.STATE_DRAGGING) setDragState(DragState.STATE_IDLE)
    }

    fun clearMotionHistory() {
        if(isChangedMotion) {
            Arrays.fill(mInitialMotionX, 0f)
            Arrays.fill(mInitialMotionY, 0f)
            Arrays.fill(mLastMotionX, 0f)
            Arrays.fill(mLastMotionY, 0f)
        }
    }

    //모션 히스토리 사이즈를 보장
    private fun ensureMotionHistorySizeForId(pointerId: Int) {
        if (!isChangedMotion || mInitialMotionX.size <= pointerId) {
            val imx = FloatArray(pointerId + 1)
            val imy = FloatArray(pointerId + 1)
            val lmx = FloatArray(pointerId + 1)
            val lmy = FloatArray(pointerId + 1)

            if(isChangedMotion) {
                System.arraycopy(mInitialMotionX, 0, imx, 0, mInitialMotionX.size)
                System.arraycopy(mInitialMotionY, 0, imy, 0, mInitialMotionY.size)
                System.arraycopy(mLastMotionX, 0, lmx, 0, mLastMotionX.size)
                System.arraycopy(mLastMotionY, 0, lmy, 0, mLastMotionY.size)
            }

            mInitialMotionX = imx
            mInitialMotionY = imy
            mLastMotionX = lmx
            mLastMotionY = lmy
            isChangedMotion = true
        }
    }

    //초기 모션을 저장
    private fun saveInitialMotion(x: Float, y: Float, pointerId: Int) {
        ensureMotionHistorySizeForId(pointerId)
        mLastMotionX[pointerId] = x
        mInitialMotionX[pointerId] = mLastMotionX[pointerId]
        mLastMotionY[pointerId] = y
        mInitialMotionY[pointerId] = mLastMotionY[pointerId]
    }

    //마지막 모션을 저장
    private fun saveLastMotion(motionEvent: MotionEvent) {
        for (i in 0 until motionEvent.pointerCount) {
            mLastPointIndex = motionEvent.getPointerId(i)

            if (mLastMotionX.size > mLastPointIndex && mLastMotionY.size > mLastPointIndex) {
                mLastMotionX[mLastPointIndex] = motionEvent.getX(i)
                mLastMotionY[mLastPointIndex] = motionEvent.getY(i)
            }
        }
    }

    private fun setDragState(state: DragState) {
        if (mPrevDragState != state) {
            mPrevDragState = state
            callback.onViewDragStateChanged(state.value)
        }
    }

    // pointerID로 뷰를 캡쳐 시도
    // Drag 상태로 만듬
    // 이전에 캡쳐된 뷰라면 바로 true return
    private fun tryCaptureViewForDrag(toCapture: View?, pointerId: Int): Boolean {
        if (isAvailChildView && toCapture == mChildView && 0 == pointerId) return true

        if (toCapture != null && callback.tryCaptureView(toCapture)) {
            captureChildView(toCapture, pointerId)
            return true
        }
        return false
    }

    fun getViewDragState() = mPrevDragState

    fun setMinVelocity(minVel: Float) { mMinVelocity = minVel }

    fun getTouchSlop() = mTouchSlop

    // 부모 뷰의 onInterceptTouchEvent로 outch event를 가로챔
    // 부모 뷰의 motionevent로 처리
    fun shouldInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        //액션 리셋
        if (motionEvent.action == MotionEvent.ACTION_DOWN) clearMotionHistory()

        mVelocityTracker.addMovement(motionEvent)

        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = motionEvent.x
                val y = motionEvent.y
                val pointerId = motionEvent.getPointerId(0)
                saveInitialMotion(x, y, pointerId)

                val toCapture = findTopChildUnder(x.toInt(), y.toInt())

                // 가능하면 기존 뷰를 잡음
                if (isAvailChildView && toCapture == mChildView && mPrevDragState == DragState.STATE_SETTLING) {
                    tryCaptureViewForDrag(toCapture, pointerId)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                //드래그 가능한 View에서 교차되면 첫번째가 승리함
                //가장자리 끄는 것을 처리
                var i = 0
                while (i < motionEvent.pointerCount && isChangedMotion) {
                    val pointerId = motionEvent.getPointerId(i)
                    if (pointerId >= mInitialMotionX.size || pointerId >= mInitialMotionY.size) {
                        i++
                        continue
                    }
                    //콜백 가장자리 끌기 시작
                    if (mPrevDragState == DragState.STATE_DRAGGING) break

                    val toCapture = findTopChildUnder(mInitialMotionX[pointerId].toInt(),
                            mInitialMotionY[pointerId].toInt())
                    if (toCapture != null && checkTouchSlop(toCapture,
                                    motionEvent.getY(i) - mInitialMotionY[pointerId])
                            && tryCaptureViewForDrag(toCapture, pointerId)) {
                        break
                    }
                    i++
                }
                saveLastMotion(motionEvent)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> clearMotionHistory()
        }
        return mPrevDragState == DragState.STATE_DRAGGING
    }

    //부모 뷰에서 받은 터치 이벤트를 처리
    //콜백 메소드로 다시 보냄
    //부모뷰는 onTouchEvent에서 구현해야함
    fun processTouchEvent(motionEvent: MotionEvent): Boolean {
        if(motionEvent.action == MotionEvent.ACTION_DOWN)
            clearMotionHistory()
        if((mPrevDragState == DragState.STATE_DRAGGING
                        || mPrevDragState == DragState.STATE_SETTLING)
                && (motionEvent.action == MotionEvent.ACTION_DOWN)){
            dispatchViewReleased(0f, 0f)
            clearMotionHistory()
            return false
        }

        mVelocityTracker.addMovement(motionEvent)

        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = motionEvent.x
                val y = motionEvent.y
                val pointerId = motionEvent.getPointerId(0)

                val toCapture = findTopChildUnder(x.toInt(), y.toInt())

                saveInitialMotion(x, y, pointerId)
                tryCaptureViewForDrag(toCapture, pointerId)
            }

            MotionEvent.ACTION_MOVE -> {
                if (mPrevDragState == DragState.STATE_DRAGGING) {
                    val x = motionEvent.getX(0)
                    val y = motionEvent.getY(0)
                    val idx = (x - mLastMotionX[0]).toInt()
                    val idy = (y - mLastMotionY[0]).toInt()

                    dragTo(mChildView.left + idx, mChildView.top + idy, idx, idy)

                    saveLastMotion(motionEvent)
                } else {
                    // 포인터가 드래그 가능한 뷰에 있는지 확인
//                    for (i in 0 until motionEvent.pointerCount) {
//                        val pointerId = motionEvent.getPointerId(i)

//                        if (mPrevDragState == DragState.STATE_DRAGGING) break


                    if (mPrevDragState != DragState.STATE_DRAGGING) {
                        val x = motionEvent.x
                        val y = motionEvent.y
                        val pointerId = motionEvent.getPointerId(0)
                        saveInitialMotion(x, y, pointerId)

                        val toCapture = findTopChildUnder(mInitialMotionX[0].toInt(), mInitialMotionY[0].toInt())
                        checkTouchSlop(toCapture, motionEvent.getY(0) - mInitialMotionY[0])
                                && tryCaptureViewForDrag(toCapture, 0)

                        tryCaptureViewForDrag(toCapture, pointerId)
                    }
//                    }
                    saveLastMotion(motionEvent)
                }
            }

            MotionEvent.ACTION_UP -> {
                if(mPrevDragState == DragState.STATE_IDLE){
                    dispatchViewReleased(0f, 0f)
                    clearMotionHistory()
                    return false
                }else if (mPrevDragState == DragState.STATE_DRAGGING) {
                    releaseViewForPointerUp()
                } else{
                    clearMotionHistory()
                    return false
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                if (mPrevDragState == DragState.STATE_DRAGGING)
                    dispatchViewReleased(0f, 0f)
                clearMotionHistory()
                return false
            }
        }
        return true
    }

    //자식 뷰에를 위해 정상적인 터치 교차 체크
    //자식이 수평 또는 수직 축, 모션으로 드래그 되지 않다면, 그 축들은 slop 체크에 포함되지 않음
    private fun checkTouchSlop(child: View?, dy: Float): Boolean {
        if (child == null) return false

         if (callback.getViewVerticalDragRange(child) > 0) {
            return abs(dy) > mTouchSlop
        }
        return false
    }

    fun isDragging() = mPrevDragState == DragState.STATE_DRAGGING

    private fun releaseViewForPointerUp() {
        mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity)

        dispatchViewReleased(
                clampMag(mVelocityTracker.getXVelocity(0), mMinVelocity, mMaxVelocity)
                , clampMag(mVelocityTracker.getYVelocity(0), mMinVelocity, mMaxVelocity))
    }

    private fun dragTo(left: Int, top: Int, dx: Int, dy: Int) {
        var clampedY = top
        val oldLeft = mChildView.left
        val oldTop = mChildView.top

        if (dy != 0) {
            clampedY = callback.clampViewPositionVertical(mChildView, top, dy)
            mChildView.offsetTopAndBottom(clampedY - oldTop)
        }

        if (dx != 0 || dy != 0) {
            val clampedDx = left - oldLeft
            val clampedDy = clampedY - oldTop
            callback.onViewPositionChanged(mChildView, left, clampedY, clampedDx, clampedDy)
        }
    }

    private fun findTopChildUnder(x: Int, y: Int): View? {
        val child = parentView.getChildAt(1)
        if (x >= child.left && x < child.right && y >= 5) {
            return child
        }
        return null
    }
}
