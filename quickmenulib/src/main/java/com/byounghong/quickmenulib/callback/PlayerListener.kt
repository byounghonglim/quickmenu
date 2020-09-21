package com.byounghong.quickmenulib.callback

import android.os.Message

/**
 * #
 * @auther : byounghonglim
 * @since : 2019-10-11
 */

interface PlayerListener{
    fun updateStatus(msg:Message)
}