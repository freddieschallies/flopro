package za.co.flopro.jobcards;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.print.PrintAttributes;
import android.print.PrintManager;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Native helpers the web app calls from the APK: print / save PDF, share a backup file, and download + install app updates. */
@CapacitorPlugin(name = "FloPro")
public class FloProPlugin extends Plugin {

    @PluginMethod
    public void print(PluginCall call) {
        final String title = call.getString("title", "FloPro Plumbing");
        getActivity().runOnUiThread(() -> {
            try {
                PrintManager pm = (PrintManager) getContext().getSystemService(Context.PRINT_SERVICE);
                PrintAttributes attrs = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();
                pm.print(title, getBridge().getWebView().createPrintDocumentAdapter(title), attrs);
                call.resolve();
            } catch (Exception e) {
                call.reject("Could not open print: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void shareFile(PluginCall call) {
        String name = call.getString("name", "flopro-backup.json");
        String mime = call.getString("mime", "application/json");
        String text = call.getString("text", "");
        try {
            File dir = new File(getContext().getCacheDir(), "share");
            dir.mkdirs();
            File f = new File(dir, name);
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(text.getBytes(StandardCharsets.UTF_8));
            }
            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", f);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(mime);
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, name);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(send, "Save backup to…");
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getActivity().startActivity(chooser);
            call.resolve();
        } catch (Exception e) {
            call.reject("Could not share file: " + e.getMessage());
        }
    }

    @PluginMethod
    public void info(PluginCall call) {
        JSObject r = new JSObject();
        r.put("version", BuildConfig.VERSION_NAME);
        r.put("build", BuildConfig.VERSION_CODE);
        call.resolve(r);
    }

    /** Fetches a web address natively (no browser limits), following redirects. Returns {status, text}. */
    @PluginMethod
    public void getText(PluginCall call) {
        final String url = call.getString("url");
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                String next = url;
                int code = 0;
                for (int hop = 0; hop < 6; hop++) {
                    c = (HttpURLConnection) new URL(next).openConnection();
                    c.setInstanceFollowRedirects(false);
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(20000);
                    c.setRequestProperty("User-Agent", "FloProPlumbing-Android");
                    c.setRequestProperty("Accept", "application/json, */*");
                    c.setRequestProperty("Cache-Control", "no-cache");
                    code = c.getResponseCode();
                    if (code >= 300 && code < 400) { next = c.getHeaderField("Location"); c.disconnect(); continue; }
                    break;
                }
                InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                StringBuilder sb = new StringBuilder();
                if (in != null) {
                    byte[] buf = new byte[16384];
                    int n;
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                    in.close();
                    sb.append(bo.toString("UTF-8"));
                }
                JSObject r = new JSObject();
                r.put("status", code);
                r.put("text", sb.toString());
                call.resolve(r);
            } catch (Exception e) {
                call.reject("OFFLINE: " + e.getMessage());
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }

    /** Can this app install APKs yet? (Android asks once per app.) */
    @PluginMethod
    public void canInstall(PluginCall call) {
        JSObject r = new JSObject();
        r.put("allowed", Build.VERSION.SDK_INT < 26 || getContext().getPackageManager().canRequestPackageInstalls());
        call.resolve(r);
    }

    /** Downloads the new APK from GitHub and opens Android's installer. Data in the app is kept. */
    @PluginMethod
    public void installUpdate(PluginCall call) {
        final String url = call.getString("url");
        if (url == null || url.isEmpty()) { call.reject("No download link"); return; }
        if (Build.VERSION.SDK_INT >= 26 && !getContext().getPackageManager().canRequestPackageInstalls()) {
            Intent allow = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getContext().getPackageName()));
            allow.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(allow);
            call.reject("NEED_PERMISSION");
            return;
        }
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                File dir = new File(getContext().getCacheDir(), "updates");
                dir.mkdirs();
                File apk = new File(dir, "FloPro-Jobs.apk");
                String next = url;
                for (int hop = 0; hop < 6; hop++) {
                    c = (HttpURLConnection) new URL(next).openConnection();
                    c.setInstanceFollowRedirects(false);
                    c.setConnectTimeout(20000);
                    c.setReadTimeout(60000);
                    int code = c.getResponseCode();
                    if (code >= 300 && code < 400) { next = c.getHeaderField("Location"); c.disconnect(); continue; }
                    if (code != 200) throw new Exception("server said " + code);
                    break;
                }
                long total = c.getContentLengthLong(), done = 0;
                int last = -1;
                try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(apk)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        done += n;
                        if (total > 0) {
                            int pct = (int) (done * 100 / total);
                            if (pct != last) { last = pct; JSObject p = new JSObject(); p.put("pct", pct); notifyListeners("updateProgress", p); }
                        }
                    }
                }
                Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", apk);
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setDataAndType(uri, "application/vnd.android.package-archive");
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(install);
                call.resolve();
            } catch (Exception e) {
                call.reject("Download failed: " + e.getMessage());
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }
}
