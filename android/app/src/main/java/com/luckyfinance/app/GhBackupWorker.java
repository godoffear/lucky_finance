package com.luckyfinance.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Вечерняя отправка резервной копии в приватный репозиторий GitHub, даже если приложение закрыто. */
public class GhBackupWorker extends Worker {
    public static final String PREFS = "lucky_backup";
    private static final String WORK_NAME = "gh-backup";

    public GhBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** Ставит ежедневное задание на выбранный час (по местному времени) или снимает его. */
    public static void schedule(Context c, boolean enabled, int hour) {
        WorkManager wm = WorkManager.getInstance(c);
        if (!enabled) {
            wm.cancelUniqueWork(WORK_NAME);
            return;
        }
        Calendar t = Calendar.getInstance();
        long now = t.getTimeInMillis();
        t.set(Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, hour)));
        t.set(Calendar.MINUTE, 10);
        t.set(Calendar.SECOND, 0);
        if (t.getTimeInMillis() <= now) t.add(Calendar.DAY_OF_MONTH, 1);
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(GhBackupWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(t.getTimeInMillis() - now, TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req);
    }

    private static String read(File f) throws Exception {
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean("enabled", false)) return Result.success();
        String repo = p.getString("repo", "");
        String token = p.getString("token", "");
        if (repo.isEmpty() || token.isEmpty()) return Result.success();
        try {
            File data = new File(c.getFilesDir(), "lucky-mirror.json");
            File hashFile = new File(c.getFilesDir(), "lucky-mirror.hash");
            if (!data.exists()) return Result.success();
            String json = read(data);
            String hash = hashFile.exists() ? read(hashFile).trim() : "";
            if (!hash.isEmpty() && hash.equals(p.getString("lastHash", ""))) return Result.success();

            String name = "LuckyFinance_backup_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date()) + ".json";
            JSONObject body = new JSONObject();
            body.put("message", "Резервная копия " + name);
            body.put("content", Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
            HttpURLConnection con = (HttpURLConnection) new URL("https://api.github.com/repos/" + repo + "/contents/backups/" + name).openConnection();
            con.setRequestMethod("PUT");
            con.setConnectTimeout(20000);
            con.setReadTimeout(30000);
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", "Bearer " + token);
            con.setRequestProperty("Accept", "application/vnd.github+json");
            con.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = con.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = con.getResponseCode();
            SharedPreferences.Editor e = p.edit();
            if (code == 200 || code == 201) {
                e.putString("lastHash", hash).putLong("lastAt", System.currentTimeMillis()).putString("err", "");
                e.apply();
                return Result.success();
            }
            String msg;
            if (code == 401) msg = "токен не принят — проверь или создай новый";
            else if (code == 403) msg = "у токена нет права записи в репозиторий (Contents: Read and write)";
            else if (code == 404) msg = "репозиторий не найден или у токена нет к нему доступа";
            else msg = "GitHub ответил кодом " + code;
            e.putString("err", msg).putLong("errAt", System.currentTimeMillis()).apply();
            return (code >= 500 || code == 429) ? Result.retry() : Result.success();
        } catch (Exception ex) {
            return Result.retry();
        }
    }
}
