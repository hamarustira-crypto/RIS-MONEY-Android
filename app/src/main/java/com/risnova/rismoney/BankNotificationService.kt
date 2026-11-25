
package com.risnova.rismoney

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class BankNotificationService : NotificationListenerService() {

    companion object {
        const val TAG = "RIS_BANK"
        const val KPLUS_PACKAGE =
            "com.kasikorn.retail.mbanking.wap"
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {
        if (sbn.packageName != KPLUS_PACKAGE) return

        Log.d(TAG, "K PLUS notification detected")
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification
    ) {
        // ไม่ประมวลผลเมื่อแจ้งเตือนถูกลบ
    }
}
