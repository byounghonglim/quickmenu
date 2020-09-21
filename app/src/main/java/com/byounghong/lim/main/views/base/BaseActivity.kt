package com.byounghong.lim.main.views.base

import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable

/**
 *
 * @author byounghong
 * @since 08/10/2018 2:44 PM
 **/
abstract class BaseActivity : AppCompatActivity()  {

    private val disposables by lazy { CompositeDisposable() }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

}