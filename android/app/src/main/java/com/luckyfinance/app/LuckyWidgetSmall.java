package com.luckyfinance.app;

/** Маленький виджет 2x2: остаток, траты за сегодня, на сколько хватит денег. */
public class LuckyWidgetSmall extends LuckyWidget {
    @Override
    protected int layoutId() {
        return R.layout.widget_small;
    }
}
