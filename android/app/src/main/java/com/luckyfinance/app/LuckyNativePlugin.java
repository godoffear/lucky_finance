package com.luckyfinance.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import java.io.File;
import android.graphics.Bitmap;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Мост между приложением и Android: данные для виджета и печать отчёта в PDF. */
@CapacitorPlugin(name = "LuckyNative")
public class LuckyNativePlugin extends Plugin {
    private WebView printView;

    @PluginMethod
    public void updateWidget(PluginCall call) {
        SharedPreferences.Editor e = getContext().getSharedPreferences(LuckyWidget.PREFS, Context.MODE_PRIVATE).edit();
        e.putString("title", call.getString("title", ""));
        e.putString("line1", call.getString("line1", ""));
        e.putString("line2", call.getString("line2", ""));
        e.apply();
        LuckyWidget.refreshAll(getContext());
        call.resolve();
    }

    /** Открывает системную установку скачанного APK. Если разрешения «Установка из этого источника» ещё нет, открывает его настройку. */
    @PluginMethod
    public void installApk(PluginCall call) {
        try {
            Context c = getContext();
            File f = new File(call.getString("path", ""));
            String cache = c.getCacheDir().getCanonicalPath();
            if (!f.exists() || !f.getCanonicalPath().startsWith(cache)) {
                call.reject("file not found");
                return;
            }
            JSObject r = new JSObject();
            if (Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls()) {
                Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.getPackageName()));
                s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(s);
                r.put("needPermission", true);
                call.resolve(r);
                return;
            }
            Uri u = FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(u, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.put("needPermission", false);
            call.resolve(r);
        } catch (Exception e) {
            call.reject("install failed");
        }
    }

    @PluginMethod
    public void setBackupJob(PluginCall call) {
        boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", false));
        SharedPreferences.Editor e = getContext().getSharedPreferences(GhBackupWorker.PREFS, Context.MODE_PRIVATE).edit();
        e.putBoolean("enabled", enabled);
        e.putString("repo", call.getString("repo", ""));
        e.putString("token", call.getString("token", ""));
        e.apply();
        GhBackupWorker.schedule(getContext(), enabled, call.getInt("hour", 22));
        call.resolve();
    }

    @PluginMethod
    public void getBackupState(PluginCall call) {
        SharedPreferences p = getContext().getSharedPreferences(GhBackupWorker.PREFS, Context.MODE_PRIVATE);
        JSObject r = new JSObject();
        r.put("lastAt", p.getLong("lastAt", 0));
        r.put("lastHash", p.getString("lastHash", ""));
        r.put("err", p.getString("err", ""));
        r.put("errAt", p.getLong("errAt", 0));
        call.resolve(r);
    }

    @PluginMethod
    public void printHtml(final PluginCall call) {
        final String html = call.getString("html", "");
        final String name = call.getString("name", "LuckyFinance");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    printView = new WebView(getActivity());
                    printView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            PrintManager pm = (PrintManager) getActivity().getSystemService(Context.PRINT_SERVICE);
                            PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(name);
                            pm.print(name, adapter, new PrintAttributes.Builder().build());
                            call.resolve();
                        }
                    });
                    printView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
                } catch (Exception ex) {
                    call.reject("print failed");
                }
            }
        });
    }
}
