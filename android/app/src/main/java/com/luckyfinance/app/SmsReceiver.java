package com.luckyfinance.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import java.util.regex.Pattern;

/**
 * Ловит SMS банка (Т-Банк: «Пополнение. Счет RUB. 1000 ₽…», «Перевод…», «Покупка…») и кладёт текст в очередь.
 * Остальные SMS не сохраняются.
 */
public class SmsReceiver extends BroadcastReceiver {
    private static final Pattern BANK = Pattern.compile("^(Пополнение|Перевод|Покупка|Оплата|Снятие)\\.[\\s\\S]*₽");

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
            SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (parts == null || parts.length == 0) return;
            StringBuilder sb = new StringBuilder();
            for (SmsMessage m : parts) sb.append(m.getMessageBody());
            String body = sb.toString().trim();
            if (BANK.matcher(body).find()) BankNotifListener.enqueue(context, body, System.currentTimeMillis());
        } catch (Exception e) {
            /* молча */
        }
    }
}
