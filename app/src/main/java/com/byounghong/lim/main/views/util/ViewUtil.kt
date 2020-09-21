package com.byounghong.lim.main.views.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.byounghong.lim.main.R
import kotlin.reflect.KClass
import java.io.DataOutputStream
import java.io.IOException


/**
 *
 * @author byounghong
 * @since 08/10/2018 2:38 PM
 **/
fun AppCompatActivity.setFragment(fragment: Fragment, container: Int = R.id.container) {
    supportFragmentManager
            .beginTransaction()
            .replace(container, fragment)
            .commit()
}

fun AppCompatActivity.getFragment()
        = supportFragmentManager.fragments.get(0)

fun setSystemUIEnabled(enabled: Boolean) {
    try {
        val p = Runtime.getRuntime().exec("su")
        val os = DataOutputStream(p.outputStream)
        os.writeBytes("pm " + (if (enabled) "enable" else "disable") + " com.android.systemui\n")
        os.writeBytes("exit\n")
        os.flush()
    } catch (e: IOException) {
        e.printStackTrace()
    }

}


fun Context.startActivity(url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    val uri = Uri.parse(url)
    intent.data = uri

    startActivity(intent)
}

fun Fragment.startActivity(kClass:KClass<out Activity>, bundle: Bundle?=null ){
    val intent = Intent(activity, kClass.java)
    bundle?.let { intent.putExtras(it) }
    startActivity(intent)
}

fun Context.getAppVersion() = packageManager.getPackageInfo(packageName, 0).versionName

fun Context.isAvailUsim()  =
        (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)?.let {
            val simStatus = it.simState
            if(it.simState == TelephonyManager.SIM_STATE_ABSENT
                    || it.simState == TelephonyManager.SIM_STATE_UNKNOWN) return false
            else true
        }

