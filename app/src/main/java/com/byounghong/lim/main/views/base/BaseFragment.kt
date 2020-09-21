package com.byounghong.lim.main.views.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.byounghong.lim.main.eventbus.EventBus
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject

/**
 *
 * @author byounghong
 * @since 08/10/2018 2:45 PM
 **/
abstract class BaseFragment : Fragment() {
    protected abstract val layoutId : Int
    protected abstract val fragment : Fragment

    val eventBus:EventBus<Any> by inject()

    private val disposables by lazy { CompositeDisposable() }
    internal  lateinit var subscription : Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?)
            = inflater.inflate(layoutId, container, false)


    override fun onPause() {
        super.onPause()
//        subscription?.let { subscription.dispose() }
    }
    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }
}