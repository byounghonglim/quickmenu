package com.byounghong.lim.main.views.base

import android.os.Bundle
import com.byounghong.lim.main.R
import com.byounghong.lim.main.eventbus.EventBus
import com.byounghong.lim.main.views.util.setFragment
import org.koin.android.ext.android.inject

/**
 * Created by byounghong on 2017. 10. 17..
 */


abstract class BaseFragmentActivity()
    : BaseActivity() {

    val eventBus:EventBus<Any> by inject()

    protected abstract val fragment : BaseFragment

//    internal  lateinit var subscription : Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        setFragment(fragment)
    }

    override fun onPause() {
        super.onPause()
//        subscription.let { subscription.dispose() }
    }

}