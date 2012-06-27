package liqui.droid.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import java.util.List;

import liqui.droid.Constants;
import liqui.droid.R;
import liqui.droid.db.DBSystem;
import liqui.droid.db.DBSystemProvider;

/**
 * The class Preferences.
 *
 * @author Jakob Flierl
 */
public class Preferences extends SherlockPreferenceActivity {
    
    @Override
    public void onCreate(Bundle icicle) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(icicle);
        setTitle(getString(R.string.settings_title));
    }
    
    @Override
    public void onBuildHeaders(List<Header> target) {
        
        Header general = new Header();
        general.title = getString(R.string.settings_general);
        general.fragment = "liqui.droid.activity.Preferences$GeneralSettingsPreferenceFragment";
        
        target.add(general);
        
        AccountManager am = AccountManager.get(getApplicationContext());
        for (Account account : am.getAccountsByType(getString(R.string.account_type))) {
            
            Header h = new Header();
            
            h.title = account.name;
            h.fragment = "liqui.droid.activity.Preferences$AccountSettingsPreferenceFragment";
            
            Bundle extras = new Bundle();
            extras.putParcelable("account", account);
            h.fragmentArguments = extras;
            
            target.add(h);
        }
        
        Header about = new Header();
        about.title = getString(R.string.settings_about);
        about.fragment = "liqui.droid.activity.Preferences$AboutSettingsPreferenceFragment";
        
        target.add(about);
    }

    public static class GeneralSettingsPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_general);
        }
    }
    
    public static class AccountSettingsPreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener {
        
        Account mAccount;
        
        LQFBContentObserver mObserver;
        
        EditTextPreference mApiKey;
        
        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            addPreferencesFromResource(R.xml.prefs_account);
            
            Bundle extras = null;
            if (getArguments() != null) {
                extras = getArguments();
                
            } else if (getActivity() != null && getActivity().getIntent() != null) {
                extras = getActivity().getIntent().getExtras();
            }

            mAccount = (Account)extras.getParcelable("account");

            mObserver = new LQFBContentObserver(new Handler());
            
            getActivity().getContentResolver().registerContentObserver(DBSystemProvider.ACCOUNT_CONTENT_URI, true, mObserver);
            
            mApiKey  = (EditTextPreference) findPreference("api_key");
            
            mApiKey.setOnPreferenceChangeListener(this);

            fillData();
        }
        
        public void fillData() {
            
            getActivity().setTitle(mAccount.name);
            
            AccountManager am = AccountManager.get(getActivity().getApplicationContext());
            
            Uri contentUri = DBSystemProvider.ACCOUNT_CONTENT_URI;
            
            Cursor c = getActivity().getContentResolver().query(contentUri, null,
                    DBSystem.Account.COLUMN_MEMBER_ID + " = ? AND " + DBSystem.Account.COLUMN_NAME + " = ?",
                    new String[] { am.getUserData(mAccount, Constants.Account.MEMBER_ID), am.getUserData(mAccount, Constants.Account.API_NAME) }, null);
            
            c.moveToFirst();
            
            if (!c.isAfterLast()) {
                String apiKey  = c.getString(c.getColumnIndex(DBSystem.Account.COLUMN_API_KEY));
                
                mApiKey.setText(apiKey);
                
                mApiKey.setSummary(apiKey);
            }
            
            c.close();
        }
        
        @Override
        public void onDestroy() {
            getActivity().getContentResolver().unregisterContentObserver(mObserver);
            super.onDestroy();
        };
        
        @Override
        public void onResume() {
            getActivity().getContentResolver().registerContentObserver(DBSystemProvider.ACCOUNT_CONTENT_URI, true, mObserver);
            super.onResume();
        };
        
        @Override
        public void onPause() {
            getActivity().getContentResolver().unregisterContentObserver(mObserver);
            super.onPause();
        };
        
        class LQFBContentObserver extends ContentObserver {

            public LQFBContentObserver(Handler handler) {
                super(handler);
            }

            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                fillData();
            }
            
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference == mApiKey) {
                String apiKey = (String)newValue;
                
                Log.d("XXX", "Updating apiKey: " + apiKey);
                
                AccountManager am = AccountManager.get(getActivity().getApplicationContext());

                ContentValues values = new ContentValues();
                values.put(DBSystem.Account.COLUMN_API_KEY, apiKey);

                getActivity().getContentResolver().update(DBSystemProvider.ACCOUNT_CONTENT_URI, values,
                        DBSystem.Account.COLUMN_MEMBER_ID + " = ? AND " + DBSystem.Account.COLUMN_NAME + " = ?",
                        new String[] { am.getUserData(mAccount, Constants.Account.MEMBER_ID),
                            am.getUserData(mAccount, Constants.Account.API_NAME)});
                
                am.setPassword(mAccount, apiKey);
                
                fillData();
            }
            
            return false;
        }

    }
    
    public static class AboutSettingsPreferenceFragment extends PreferenceFragment {
        
        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            
            addPreferencesFromResource(R.xml.prefs_about);
        }
    }
}
