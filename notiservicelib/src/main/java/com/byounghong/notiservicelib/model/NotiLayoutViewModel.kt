package com.byounghong.notiservicelib.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.byounghong.notiservicelib.*

/**
 * #
 * @auther : byounghonglim
 * @since : 2019-08-27
 */

class NotiLayoutViewModel : ViewModel() {
    val isItemsLayout
            = MutableLiveData<Boolean>().apply { this.value = false }

    val isDeviceLayout
            = MutableLiveData<Boolean>().apply { this.value = false }

    val isScheduleLayout
            = MutableLiveData<Boolean>().apply { this.value = false }

    val isAlarmLayout
            = MutableLiveData<Boolean>().apply { this.value = false }

    val isNormalLayout
            = MutableLiveData<Boolean>().apply { this.value = false }

    val alertAppPackage
            = MutableLiveData<String>().apply { this.value = "" }

    val isShowApp
            = MutableLiveData<Boolean>().apply { this.value = false }

    val notiID
            = MutableLiveData<Int>().apply { this.value = -1 }

    val agreementUri
            = MutableLiveData<String>().apply { this.value = "" }

    val agreementCode
            = MutableLiveData<String>().apply { this.value = "" }

    fun updateNotificationLayout(str:String){
        alertAppPackage.value = ""
        when(str){
            "lim" ->{
                isItemsLayout.value = true
                isDeviceLayout.value = false
                isScheduleLayout.value = false
                isAlarmLayout.value = false
                isNormalLayout.value = false
            }
            "system" ->{
                isItemsLayout.value = false
                isDeviceLayout.value = true
                isScheduleLayout.value = false
                isAlarmLayout.value = false
                isNormalLayout.value = false
                alertAppPackage.value = SETTINGS
            }
            "agreement"->{
                isItemsLayout.value = false
                isDeviceLayout.value = true
                isScheduleLayout.value = false
                isAlarmLayout.value = false
                isNormalLayout.value = false
                alertAppPackage.value = AGREEMENT
            }
            "schedule" ->{
                isItemsLayout.value = false
                isDeviceLayout.value = false
                isScheduleLayout.value = true
                isAlarmLayout.value = false
                isNormalLayout.value = false
                alertAppPackage.value = SCHEDULE
            }
            "alarm" ->{
                isItemsLayout.value = false
                isDeviceLayout.value = false
                isScheduleLayout.value = false
                isAlarmLayout.value = true
                isNormalLayout.value = false
                alertAppPackage.value = ALARM
            }
            "normal"  ->{
                isItemsLayout.value = false
                isDeviceLayout.value = false
                isScheduleLayout.value = false
                isAlarmLayout.value = false
                isNormalLayout.value = true
            }
        }
    }
}