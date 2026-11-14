package com.luckyfinance.app;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Ловит уведомления банка BIDV (по тексту: «Transaction amount» и «Ending balance») и кладёт их в очередь.
 * Остальные уведомления не читаются и не сохраняются. Разбор текста делает приложение.
 */
public class BankNotifListener extends NotificationListenerService {
    static final String PREFS = "lucky_bank_notif";
    static final Object LOCK = new Object();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification();
            if (n == null || n.extras == null) return;
            CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence big = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            String body = (big != null && big.length() > 0) ? big.toString() : (text != null ? text.toString() : "");
            String all = (title != null ? title : "") + "\n" + body;
            if (!all.contains("Transaction amount") || !all.contains("Ending balance")) return;
            enqueue(this, all, sbn.getPostTime());
        } catch (Exception e) {
            /* молча: чужое уведомление не должно ронять службу */
        }
    }

    /** Кладёт текст в очередь (без дублей). Используется и для SMS. */
    static void enqueue(Context c, String all, long t) {
        try {
            synchronized (LOCK) {
                SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                JSONArray q = new JSONArray(p.getString("q", "[]"));
                for (int i = 0; i < q.length(); i++) {
                    if (all.equals(q.getJSONObject(i).optString("x"))) return;
                }
                JSONObject o = new JSONObject();
                o.put("t", t);
                o.put("x", all);
                q.put(o);
                JSONArray cut = new JSONArray();
                for (int i = Math.max(0, q.length() - 100); i < q.length(); i++) cut.put(q.get(i));
                p.edit().putString("q", cut.toString()).apply();
            }
        } catch (Exception e) {
            /* молча */
        }
    }
}
