package com.byounghong.quickmenulib.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.ViewCompat
import com.byounghong.quickmenulib.callback.ControlDragCallback
import com.byounghong.quickmenulib.controller.DragController
import com.byounghong.quickmenulib.R
import com.byounghong.quickmenulib.model.QuickMenuViewModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class QuickMenuLayout @JvmOverloads constructor(context: Context,
                                                attrs: AttributeSet? = null, defStyle: Int = 0)
    : ViewGroup(context, attrs, defStyle) {

    companion object{
        const val MAX_OFFSET = 1.0f
        const val COLLAPSED_HEIGHT = 80
        const val MIN_FLING_VELOCITY = 1000
    }

    enum class ChildViewState {
        EXPANDED, COLLAPSED, DRAGGING
    }

    private var mDragViewResId = -1

    private var mQuickMenuView: View? = null
    private lateinit var mChildView: View
    private lateinit var mParentView: View

    private var mDragController: DragController

    private var mChildViewState = ChildViewState.COLLAPSED
    private var mLastNotDraggingSlideState  = ChildViewState.COLLAPSED

    private var mDragOffset: Float = 0.toFloat()
    private var mDragRange: Int = 0
    private var mPrevMotionX: Float = 0.toFloat()
    private var mPrevMotionY: Float = 0.toFloat()
    private var mInitialMotionX: Float = 0.toFloat()
    private var mInitialMotionY: Float = 0.toFloat()

    private var mIsUnableToDrag: Boolean = false
    private var mIsChangedLayout = true

    private val mPanelSlideListeners = CopyOnWriteArrayList<DragListener>()

    lateinit var quickMenuViewModel:QuickMenuViewModel

    var childViewState: ChildViewState
        get() = mChildViewState
        set(state) {
            if (mDragController.getViewDragState() == DragController.DragState.STATE_SETTLING) {
                mDragController.abort()
            }

            if (!isEnabled || state == mChildViewState
                    || mChildViewState == ChildViewState.DRAGGING) return

            if (mIsChangedLayout) {
                setPanelStateInternal(state)
            } else {
                when (state) {
                    ChildViewState.COLLAPSED -> smoothSlideTo(0f)
                    ChildViewState.EXPANDED  -> smoothSlideTo(1.0f)
                    else                     ->{}
                }
            }
        }

    init {
        if (attrs != null) {
            val typedArray =
                    context.obtainStyledAttributes(attrs, R.styleable.QuickMenuLayout)

            if (typedArray != null) {
                mDragViewResId =
                        typedArray.getResourceId(R.styleable.QuickMenuLayout_DragView, -1)
                typedArray.recycle()
            }
        }
        setWillNotDraw(false)

        mDragController = DragController(this, 0.5f, DragCallback())
        mDragController.setMinVelocity(MIN_FLING_VELOCITY * context.resources.displayMetrics.density)


    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (mDragViewResId != -1) setDragView(findViewById<View>(mDragViewResId))
    }

    fun addDragListener(listener: DragListener) {
        synchronized(mPanelSlideListeners) {
            mPanelSlideListeners.add(listener)
        }
        quickMenuViewModel.isCollapse.observeForever {
            childViewState = if(it) ChildViewState.COLLAPSED
            else ChildViewState.EXPANDED
        }
    }

    private fun setDragView(dragView: View?) {
        mQuickMenuView = dragView
        mQuickMenuView?.let {
            it.isClickable = true
            it.isFocusable = false
            it.isFocusableInTouchMode = false
            it.setOnClickListener {
                if(isEnabled)
                    childViewState = if (mChildViewState != ChildViewState.EXPANDED) {
                        ChildViewState.EXPANDED
                    } else {
                        ChildViewState.COLLAPSED
                    }
            }
        }
    }

    private fun dispatchOnPanelStateChanged(panel: View, previousState: ChildViewState, newState: ChildViewState) {
        synchronized(mPanelSlideListeners) {
            for (listener in mPanelSlideListeners) {
                listener.onStateChanged(panel, previousState, newState)
            }
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }

    internal fun changedView() {
        val parent = getChildAt(0)

        parent.visibility = if (parent.left >= mChildView.left
                && parent.top >= mChildView.top && parent.right <= mChildView.right
                && parent.bottom <= mChildView.bottom) {
            View.INVISIBLE } else { View.VISIBLE }
    }

    internal fun setChildVisible() {
        getChildAt(0).visibility = View.GONE
        for(i in 1 until childCount){
            val child = getChildAt(i)
            if (child.visibility == View.INVISIBLE || child.visibility == View.GONE)
                child.visibility = View.VISIBLE
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mIsChangedLayout = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDragController.destroy()
        mIsChangedLayout = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        mParentView = getChildAt(0)
        mChildView = getChildAt(1)
        if (mQuickMenuView == null) setDragView(mChildView)
        if (mChildView.visibility != View.VISIBLE) mChildViewState = ChildViewState.COLLAPSED

        val layoutHeight = heightSize - paddingTop - paddingBottom
        val layoutWidth = widthSize - paddingLeft - paddingRight

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val layoutParams = child.layoutParams as LayoutParams

            val childWidthSpec = MeasureSpec.makeMeasureSpec(layoutWidth, MeasureSpec.EXACTLY)
            val childHeightSpec = if (layoutParams.height == WRAP_CONTENT) {
                MeasureSpec.makeMeasureSpec(layoutHeight, MeasureSpec.AT_MOST)
            } else {
                MeasureSpec.makeMeasureSpec(layoutHeight, MeasureSpec.EXACTLY)
            }

            child.measure(childWidthSpec, childHeightSpec)

            if (child == mChildView)
                mDragRange = mChildView.measuredHeight - COLLAPSED_HEIGHT
        }

        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (mIsChangedLayout) {
            mDragOffset = when (mChildViewState) {
                ChildViewState.EXPANDED -> 1.0f
                else                    -> 0f
            }
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)

            if (child.visibility == View.GONE && (i == 0 || mIsChangedLayout)) continue

            val childHeight = child.measuredHeight
            var childTop = paddingTop

            if (child == mChildView)
                childTop = computePanelTopPosition(mDragOffset)

            val childBottom = childTop + childHeight
            val childLeft = 0
            val childRight = childLeft + child.measuredWidth

            child.layout(childLeft, childTop, childRight, childBottom)
        }

        if (mIsChangedLayout) changedView()

        mIsChangedLayout = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (h != oldh) {
            mIsChangedLayout = true
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val adx = abs(x - mInitialMotionX)
        val ady = abs(y - mInitialMotionY)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mIsUnableToDrag = false
                mInitialMotionX = x
                mInitialMotionY = y
            }
            MotionEvent.ACTION_MOVE -> {
                if (ady > mDragController.getTouchSlop() && adx > ady) {
                    mDragController.clearMotionHistory()
                    mIsUnableToDrag = true
                    return false
                }
            }

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                mIsUnableToDrag = false
                if (mDragController.isDragging()) {
                    mDragController.processTouchEvent(event)
                    return true
                }
            }
        }
        return mDragController.shouldInterceptTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return super.onTouchEvent(event)
        }
        return mDragController.processTouchEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || mIsUnableToDrag
                && event.actionMasked != MotionEvent.ACTION_DOWN) {
            mDragController.abort()
            return super.dispatchTouchEvent(event)
        }

        val x = event.x
        val y = event.y

        when(event.actionMasked){
            MotionEvent.ACTION_DOWN -> {
                mPrevMotionX = x
                mPrevMotionY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val directionX = x - mPrevMotionX
                val directionY = y - mPrevMotionY
                mPrevMotionX = x
                mPrevMotionY = y

                if (abs(directionX) > abs(directionY))
                    return super.dispatchTouchEvent(event)

                if (directionY * -1 > 0) { // 접힐 때
                    return this.onTouchEvent(event)
                } else if (directionY * -1 < 0) { // Expanding
                    if (mDragOffset < 1.0f) { //풀 사이즈로 펼쳐지는 것이 아닐 때
                        //                    mIsScrollableViewHandlingTouch = false
                        return this.onTouchEvent(event)
                    }
                    if (mDragController.isDragging()) {
                        mDragController.clearMotionHistory()
                        event.action = MotionEvent.ACTION_DOWN
                    }
                    return super.dispatchTouchEvent(event)
                }
            }
            MotionEvent.ACTION_UP -> mIsUnableToDrag = false

            else -> return super.dispatchTouchEvent(event)
        }
        return super.dispatchTouchEvent(event)
    }

    //접힌 상태에 상단 패널 계산
    private fun computePanelTopPosition(slideOffset: Float)
            = paddingTop - mChildView.measuredHeight + COLLAPSED_HEIGHT +
            ((slideOffset * mDragRange).toInt())

    //슬라이드 되는 상태에서 오프셋 계산
    private fun computeSlideOffset(topPosition: Int)
            = (topPosition - computePanelTopPosition(0f)).toFloat() / mDragRange


    private fun setPanelStateInternal(state: ChildViewState) {
        if (mChildViewState == state) return
        //        val oldState = mChildViewState
        //        mChildViewState = state
        dispatchOnPanelStateChanged(this, mChildViewState, state)
        mChildViewState = state
    }

    private fun onPanelDragged(topPosition: Int) {
        if (mChildViewState != ChildViewState.DRAGGING)
            mLastNotDraggingSlideState = mChildViewState

        setPanelStateInternal(ChildViewState.DRAGGING)
        mDragOffset = computeSlideOffset(topPosition)
        mParentView.requestLayout()
    }

    private fun smoothSlideTo(slideOffset: Float): Boolean {
        if(isEnabled){
            val panelTop = computePanelTopPosition(slideOffset)

            if (mDragController.smoothSlideViewTo(mChildView, mChildView.left, panelTop)) {
                setChildVisible()
                ViewCompat.postInvalidateOnAnimation(this)
                return true
            }
        }
        return false
    }

    override fun computeScroll() {
        if (mDragController.continueSettling()) {
            if(isEnabled) ViewCompat.postInvalidateOnAnimation(this)
            else mDragController.abort()
        }
    }

    private inner class DragCallback : ControlDragCallback() {

        override fun tryCaptureView(child: View)
                = !mIsUnableToDrag && child === mChildView

        override fun onViewDragStateChanged(state: Int) {
            if (mDragController.getViewDragState() == DragController.DragState.STATE_IDLE) {
                mDragOffset = computeSlideOffset(mChildView.top)

                when (mDragOffset) {
                    1f -> {
                        changedView()
                        setPanelStateInternal(ChildViewState.EXPANDED)
                    }
                    0f -> setPanelStateInternal(ChildViewState.COLLAPSED)
                    else -> {
                        changedView()
                        setPanelStateInternal(ChildViewState.EXPANDED)
                    }
                }
            }
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            setChildVisible()
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            onPanelDragged(top)
            invalidate()
        }

        override fun onViewReleased(releasedChild: View, xVal: Float, yVal: Float) {
            val target = if (yVal > 0 && mDragOffset <= MAX_OFFSET) {
                computePanelTopPosition(MAX_OFFSET)
            } else if (yVal < 0 && mDragOffset >= MAX_OFFSET) {
                computePanelTopPosition(MAX_OFFSET)
            } else if (yVal < 0 && mDragOffset < MAX_OFFSET) {
                computePanelTopPosition(0.0f)
            } else if (mDragOffset >= (1f + MAX_OFFSET) / 2) {
                computePanelTopPosition(1.0f)
            } else if (mDragOffset >= MAX_OFFSET / 2) {
                computePanelTopPosition(MAX_OFFSET)
            } else {
                computePanelTopPosition(0.0f)
            }

            mDragController.settleSavedViewAt(releasedChild.left, target)
            invalidate()
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return mDragRange
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            val collapsedTop = computePanelTopPosition(0f)
            val expandedTop = computePanelTopPosition(1.0f)
            return min(max(top, collapsedTop), expandedTop)
        }
    }

    interface DragListener {
        fun onStateChanged(panel: View, previousState: ChildViewState, newState: ChildViewState)
    }
}
