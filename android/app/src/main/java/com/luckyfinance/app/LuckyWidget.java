package com.luckyfinance.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

/** Виджеты рабочего стола. Этот, подробный (4x2), и два других: маленький 2x2 и полоска 4x1. Данные кладёт приложение. */
public class LuckyWidget extends AppWidgetProvider {
    public static final String PREFS = "lucky_widget";

    protected int layoutId() {
        return R.layout.widget_lucky;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) manager.updateAppWidget(id, build(context, layoutId()));
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        Class<?>[] kinds = {LuckyWidget.class, LuckyWidgetSmall.class, LuckyWidgetStrip.class};
        int[] layouts = {R.layout.widget_lucky, R.layout.widget_small, R.layout.widget_strip};
        for (int k = 0; k < kinds.length; k++) {
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, kinds[k]));
            for (int id : ids) manager.updateAppWidget(id, build(context, layouts[k]));
        }
    }

    private static PendingIntent open(Context c, String uri, int code) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        i.setClass(c, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static RemoteViews build(Context c, int layout) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        String title = p.getString("title", "");
        v.setTextViewText(R.id.w_title, title.isEmpty() ? "Lucky Finance" : title);
        v.setTextViewText(R.id.w_line2, p.getString("line2", ""));
        v.setOnClickPendingIntent(R.id.w_root, open(c, "luckyfinance://open", 10));
        if (layout != R.layout.widget_strip) v.setTextViewText(R.id.w_line1, p.getString("line1", ""));
        if (layout != R.layout.widget_small) {
            v.setOnClickPendingIntent(R.id.w_exp, open(c, "luckyfinance://op/expense", 11));
            v.setOnClickPendingIntent(R.id.w_inc, open(c, "luckyfinance://op/income", 12));
        }
        return v;
    }
}
