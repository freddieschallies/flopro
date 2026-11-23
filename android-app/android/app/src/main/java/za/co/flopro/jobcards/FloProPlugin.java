package za.co.flopro.jobcards;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import java.nio.charset.StandardCharsets;

/** Native helpers the web app calls from the APK: print / save PDF, and share a backup file. */
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
}
