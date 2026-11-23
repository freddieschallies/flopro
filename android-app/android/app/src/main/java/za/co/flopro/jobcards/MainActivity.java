package za.co.flopro.jobcards;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FloProPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
