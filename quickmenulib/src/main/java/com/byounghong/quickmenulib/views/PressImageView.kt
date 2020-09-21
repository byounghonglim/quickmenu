package com.byounghong.quickmenulib.views

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView

/**
 * #
 * @auther : byounghonglim
 * @since : 2019-07-24
 */
class PressImageView @JvmOverloads constructor(context: Context,
                                               attrs: AttributeSet? = null, defStyle: Int = 0)
    : ImageView(context, attrs, defStyle){

    override fun onFinishInflate() {
        super.onFinishInflate()
        isClickable = true
        isFocusable = true
    }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if(visibility == View.INVISIBLE  || visibility == View.GONE) alpha = 1f
        super.onVisibilityChanged(changedView, visibility)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("ClickableViewAccessibility", "ResourceAsColor")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action){
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                setColorFilter(Color.argb(30, 0, 0, 0),
                        PorterDuff.Mode.SRC_ATOP)
            else ->
                clearColorFilter()

        }
        return super.onTouchEvent(event)
    }

}