package liqui.droid.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.List;

import liqui.droid.R;

/**
 * The class Preferences.
 *
 * @author Jakob Flierl
 */
public class Preferences extends PreferenceActivity {
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle("Settings");
    }
    
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.prefs_headers, target);
    }

    public static class GeneralSettingsPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_general);
        }
    }
}
