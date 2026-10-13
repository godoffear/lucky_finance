package com.luckyfinance.app;

import android.content.Context;
import android.content.SharedPreferences;
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
