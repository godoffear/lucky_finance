package com.luckyfinance.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(LuckyNativePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
