package com.byounghong.quickmenulib.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * #
 * @auther : byounghonglim
 * @since : 2019-10-16
 */
class QuickMenuViewModel() : ViewModel(){
    var isCollapse
            = MutableLiveData<Boolean>().apply { this.value = true }
}