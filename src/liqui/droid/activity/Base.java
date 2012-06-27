/*
 * Copyright 2012 Jakob Flierl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package liqui.droid.activity;

import liqui.droid.Constants;
import liqui.droid.LQFBApplication;
import liqui.droid.db.DBSystem;
import liqui.droid.db.DBSystemProvider;
import liqui.droid.holder.BreadCrumbHolder;
import liqui.droid.service.SyncService;
import liqui.droid.util.DetachableResultReceiver;
import liqui.droid.util.ScrollingTextView;
import liqui.droid.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;


import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * The Class Base.
 */
public abstract class Base extends SherlockFragmentActivity implements DetachableResultReceiver.Receiver {
    
    protected boolean mSyncing = false;

    protected String mApiName;
    
    protected String mApiUrl;
    
    protected String mMemberId;
    
    protected String mSessionKey;

    protected DetachableResultReceiver mReceiver;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putBoolean("SYNCING", mSyncing); //$NON-NLS-1$
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      
      mSyncing = savedInstanceState.getBoolean("SYNCING"); //$NON-NLS-1$
      // setProgressVisible(mSyncing);
      
    }

    /* (non-Javadoc)
     * @see android.content.ContextWrapper#getApplicationContext()
     */
    @Override
    public LQFBApplication getApplicationContext() {
        return (LQFBApplication) super.getApplicationContext();
    }

    /**
     * Common function when device search button pressed, then open
     * SearchActivity.
     *
     * @return true, if successful
     */
    @Override
    public boolean onSearchRequested() {
    	Intent intent = new Intent().setClass(getApplication(), Search.class);
    	
        Bundle extras = new Bundle();
        extras.putString(Constants.Account.API_NAME,    getAPIName());
        extras.putString(Constants.Account.API_URL,     getAPIUrl());
        extras.putString(Constants.Account.MEMBER_ID,   getMemberId());
        extras.putString(Constants.Account.SESSION_KEY, getSessionKey());
        intent.putExtras(extras);
        
    	startActivity(intent);
        return true;
    }

    /**
     * Hide keyboard.
     *
     * @param binder the binder
     */
    public void hideKeyboard(IBinder binder) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binder, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        if (isAuthenticated()
//                && this instanceof UserActivity) {
//            MenuInflater inflater = getMenuInflater();
//            inflater.inflate(R.menu.authenticated_menu, menu);
//        }
        if (!isAuthenticated()) {
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.anon_menu, menu);
        }
        
        if (!(this instanceof Search)) {
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.search, menu);
        }
        
        return true;        
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection

        Bundle extras = new Bundle();
        extras.putString(Constants.Account.API_NAME,    getAPIName());
        extras.putString(Constants.Account.API_URL,     getAPIUrl());
        extras.putString(Constants.Account.MEMBER_ID,   getMemberId());
        extras.putString(Constants.Account.SESSION_KEY, getSessionKey());
        
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intentHome = new Intent().setClass(getApplicationContext(), MemberActivity.class);
            intentHome.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
               
            intentHome.putExtras(extras);
            startActivity(intentHome);
            return true;
        case R.id.menu_search:
            Intent intentSearch = new Intent().setClass(getApplicationContext(), Search.class);
            intentSearch.putExtras(extras);
            startActivity(intentSearch);
            return true;
        case R.id.menu_accounts:
            openAccounts();
            return true;
            /*
        case R.id.menu_logout:
            Uri LQFBUri = Uri.parse(DBSystemProvider.INSTANCE_CONTENT_URI);
            ContentValues values = new ContentValues();
            values.put(DBSystem.TableLQFBs.COLUMN_LAST_ACTIVE, 0);
            getContentResolver().update(LQFBUri, values,
                    DBSystem.TableLQFBs.COLUMN_NAME + " = ?",
                    new String[] { getAPIName() });

            Intent intent = new Intent().setClass(this, LQFB.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);
            Toast.makeText(this, getResources().getString(R.string.successful_signout), Toast.LENGTH_SHORT).show();
            this.finish();
            return true;
            */
        case R.id.menu_edit_lqfbs:
            Intent intentLQFBEdit = new Intent().setClass(this, InstanceListCached.class);
            intentLQFBEdit.putExtras(extras);
            startActivity(intentLQFBEdit);
            return true;
            /*
        case R.id.menu_contacts:
            openContacts();
            return true;
            */
        case R.id.menu_prefs:
            Intent intentPrefs = new Intent().setClass(this, Preferences.class);
            intentPrefs.putExtras(extras);
            startActivity(intentPrefs);
            return true;
        case R.id.menu_refresh:
            triggerRefresh();
            return true;
        case R.id.menu_test:
            triggerTest();
            return true;
            /*
        case R.id.membership:
            Intent intentMembership = new Intent().setClass(this, AreaListSelectActivity.class);
            startActivity(intentMembership);
            return true;
            */
        default:
            return setMenuOptionItemSelected(item);
        }
    }
    
    private void triggerTest() {
        Intent intent = new Intent().setClass(Base.this, Test.class);
        
        Bundle extras = new Bundle();
        extras.putString(Constants.Account.API_NAME,    getAPIName());
        extras.putString(Constants.Account.API_URL,     getAPIUrl());
        extras.putString(Constants.Account.MEMBER_ID,   getMemberId());
        extras.putString(Constants.Account.SESSION_KEY, getSessionKey());
        intent.putExtras(extras);

        startActivity(intent);
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
    }
    
    private void openAccounts() {
        final Intent intent = new Intent(Intent.ACTION_VIEW, null, Base.this, Accounts.class);
        
        Bundle extras = new Bundle();
        extras.putString(Constants.Account.API_NAME,    getAPIName());
        extras.putString(Constants.Account.API_URL,     getAPIUrl());
        extras.putString(Constants.Account.MEMBER_ID,   getMemberId());
        extras.putString(Constants.Account.SESSION_KEY, getSessionKey());
        intent.putExtras(extras);
        
        startActivityForResult(intent, 42);
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
    }

    @SuppressWarnings("unused")
    private void openContacts() {
        final Intent intent = new Intent(Intent.ACTION_VIEW, null, Base.this, ContactListCached.class);
        
        Bundle extras = new Bundle();
        extras.putString(Constants.Account.API_NAME,    getAPIName());
        extras.putString(Constants.Account.API_URL,     getAPIUrl());
        extras.putString(Constants.Account.MEMBER_ID,   getMemberId());
        extras.putString(Constants.Account.SESSION_KEY, getSessionKey());
        intent.putExtras(extras);
        
        startActivity(intent);
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
    }

    private void triggerRefresh() {
        final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
        intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mReceiver);
        
        Bundle extras = new Bundle();
        extras.putString(Constants.Account.API_NAME,    getAPIName());
        extras.putString(Constants.Account.API_URL,     getAPIUrl());
        extras.putString(Constants.Account.MEMBER_ID,   getMemberId());
        extras.putString(Constants.Account.SESSION_KEY, getSessionKey());
        intent.putExtras(extras);
        
        startService(intent);
    }

    /**
     * Sets the menu option item selected.
     *
     * @param item the item
     * @return true, if successful
     */
    public boolean setMenuOptionItemSelected(MenuItem item) {
        return true;
    }
    
    /**
     * Creates the breadcrumb.
     *
     * @param subTitle the sub title
     * @param breadCrumbHolders the bread crumb holders
     */
    public void createBreadcrumb(String subTitle, BreadCrumbHolder... breadCrumbHolders) {
        if (breadCrumbHolders != null) {
            LinearLayout llPart = (LinearLayout) this.findViewById(R.id.ll_part);
            for (int i = 0; i < breadCrumbHolders.length; i++) {
                TextView tvBreadCrumb = new TextView(getApplication());
                SpannableString part = new SpannableString(breadCrumbHolders[i].getLabel());
                part.setSpan(new UnderlineSpan(), 0, part.length(), 0);
                tvBreadCrumb.append(part);
                tvBreadCrumb.setTag(breadCrumbHolders[i]);
                // tvBreadCrumb.setBackgroundResource(R.drawable.default_link);
                tvBreadCrumb.setTextAppearance(getApplication(), android.R.style.TextAppearance_DeviceDefault_Medium);
                tvBreadCrumb.setSingleLine(true);
                tvBreadCrumb.setOnClickListener(new OnClickBreadCrumb(this));
    
                llPart.addView(tvBreadCrumb);
    
                if (i < breadCrumbHolders.length - 1) {
                    TextView slash = new TextView(getApplication());
                    slash.setText(" / "); //$NON-NLS-1$
                    slash.setTextAppearance(getApplication(), android.R.style.TextAppearance_DeviceDefault_Medium);
                    llPart.addView(slash);
                }
            }
        }

        ScrollingTextView tvSubtitle = (ScrollingTextView) this.findViewById(R.id.tv_subtitle);
        tvSubtitle.setText(subTitle);
    }

    /**
     * Sets the up action bar.
     */
    public void setUpActionBar() {
        ActionBar actionBar = getSupportActionBar();
        
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setIcon(R.drawable.ic_home);
        
        Bundle extras = new Bundle();
        extras.putString(Constants.Account.API_NAME,    getAPIName());
        extras.putString(Constants.Account.API_URL,     getAPIUrl());
        extras.putString(Constants.Account.MEMBER_ID,   getMemberId());
        extras.putString(Constants.Account.SESSION_KEY, getSessionKey());

        if (isAuthenticated()) {
            Intent intent = new Intent().setClass(getApplicationContext(), MemberActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            intent.putExtras(extras);
            
            // actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
         }
        
        setActionBarTitle();
        
        /*
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        
        Bundle extras = new Bundle();
        extras.putString(Constants.Account.API_NAME,    getAPIName());
        extras.putString(Constants.Account.API_URL,     getAPIUrl());
        extras.putString(Constants.Account.MEMBER_ID,   getMemberId());
        extras.putString(Constants.Account.SESSION_KEY, getSessionKey());

        if (isAuthenticated()) {
           Intent intent = new Intent().setClass(getApplicationContext(), MemberActivity.class);
           intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

           intent.putExtras(extras);
           
           actionBar.setHomeAction(new IntentAction(this, intent, R.drawable.ic_home));
        }
        // actionBar.addAction(new IntentAction(this, new Intent(getApplication(),
        //         ExploreActivity.class), R.drawable.ic_explore));
        
        Intent searchIntent = new Intent(getApplication(), Search.class);
        searchIntent.putExtras(extras);
        actionBar.addAction(new IntentAction(this, searchIntent, R.drawable.ic_search));
        
        setActionBarTitle();
        */
    }
    
    public void setActionBarTitle() {
        ActionBar actionBar= getSupportActionBar();

        if (this instanceof InstanceListCached
                || this instanceof InstanceEdit
                || this instanceof Accounts
                || this instanceof LiquiDroid
                || this instanceof SyncStatListCached) {
            actionBar.setTitle(R.string.app_name);
        } else {       
            actionBar.setTitle(getAPIName());
        }
    }
    
    /**
     * Checks if is authenticated.
     *
     * @return true, if is authenticated
     */
    public boolean isAuthenticated() {
        return getMemberId() != null;
    }
    
    /**
     * The Class OnClickBreadCrumb.
     */
    private class OnClickBreadCrumb implements OnClickListener {

        /**
         * The target.
         */
        private WeakReference<Base> mTarget;

        /**
         * Instantiates a new on click bread crumb.
         *
         * @param activity the activity
         */
        public OnClickBreadCrumb(Base activity) {
            mTarget = new WeakReference<Base>(activity);
        }

        /* (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View view) {
            TextView breadCrumb = (TextView) view;
            BreadCrumbHolder b = (BreadCrumbHolder) breadCrumb.getTag();
            String tag = b.getTag();
            HashMap<String, String> data = b.getData();

            Base baseActivity = mTarget.get();

            if (Constants.Member.LOGIN.equals(tag)) {
                mTarget.get().getApplicationContext().openUserInfoActivity(baseActivity,
                        data.get(Constants.Member.LOGIN), null);
            }
            else if (Constants.EXPLORE.equals(tag)) {
                Intent intent = new Intent().setClass(mTarget.get(), Explore.class);

                Bundle extras = new Bundle();
                extras.putString(Constants.Account.API_NAME,    getAPIName());
                extras.putString(Constants.Account.API_URL,     getAPIUrl());
                extras.putString(Constants.Account.MEMBER_ID,   getMemberId());
                extras.putString(Constants.Account.SESSION_KEY, getSessionKey());
                intent.putExtras(extras);
                
                mTarget.get().startActivity(intent);
                mTarget.get().overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
            }
        }
    };

    /**
     * Show error.
     */
    public void showError() {
        Toast
                .makeText(getApplication(), "An error occured while fetching data",
                        Toast.LENGTH_SHORT).show();
        super.finish();
    }

    /**
     * Show error.
     *
     * @param finishThisActivity the finish this activity
     */
    public void showError(boolean finishThisActivity) {
        Toast
                .makeText(getApplication(), "An error occured while talking to the API",
                        Toast.LENGTH_SHORT).show();
        if (finishThisActivity) {
            super.finish();
        }
    }
    
    /**
     * Show message.
     *
     * @param message the message
     * @param finishThisActivity the finish this activity
     */
    public void showMessage(String message, boolean finishThisActivity) {
        Toast
                .makeText(getApplication(), message,
                        Toast.LENGTH_SHORT).show();
        if (finishThisActivity) {
            super.finish();
        }
    }
    
    /**
     * Checks if is setting enabled.
     *
     * @param key the key
     * @return true, if is setting enabled
     */
    public boolean isSettingEnabled(String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(key, false);
    }
    
    /**
     * Gets the setting string value.
     *
     * @param key the key
     * @return the setting string value
     */
    public String getSettingStringValue(String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(key, null);
    }
    
    /**
     * Gets the setting string value.
     *
     * @param key the key
     * @param nullValue the null value
     * @return the setting string value
     */
    public String getSettingStringValue(String key, String nullValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(key, nullValue);
    }
    
    /**
     * Sets the setting string value.
     *
     * @param key the key
     * @param value the value
     */
    public void setSettingStringValue(String key, String value) {
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    	sp.edit().putString(key, value).commit();
    }

    @Override
    protected void onCreate(Bundle arg0) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(arg0);
        
        mReceiver = new DetachableResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        // Toast.makeText(this, "resultCode: " + resultCode, Toast.LENGTH_SHORT).show();

        switch (resultCode) {
            case SyncService.STATUS_RUNNING: {
                mSyncing = true;
                // setProgressVisible(true);
                // Toast.makeText(this, "syncing..", Toast.LENGTH_SHORT).show();
                // Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // v.vibrate(300);
                break;
            }
            case SyncService.STATUS_FINISHED: {
                mSyncing = false;
                // setProgressVisible(false);
                // Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // v.vibrate(300);
                // Toast.makeText(this, "finished syncing.", Toast.LENGTH_SHORT).show();
                break;
            }
            case SyncService.STATUS_ERROR: {
                // Error happened down in SyncService, show as toast.
                mSyncing = false;
                // setProgressVisible(false);
                final String errorText = "sync error: " + resultData.getString(Intent.EXTRA_TEXT);
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(300);
                Toast.makeText(this, errorText, Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
    
    /**
     * Returns the current API URL.
     * 
     * @return the current API URL.
     */
    public String getAPIUrl() {
        if (mApiUrl != null) {
            return mApiUrl;
        } else {
            Bundle extras = getIntent().getExtras();
            
            String apiUrl = null;
            
            if (extras != null) {
                apiUrl = extras.getString(Constants.Account.API_URL);
            }
            
            return apiUrl;
        }
    }
    
    /**
     * Returns the current API name.
     * 
     * @return the current API name.
     */
    public String getAPIName() {
        if (mApiName != null) {
            return mApiName;
        } else {
            Bundle extras = getIntent().getExtras();
            
            String apiName = null;
            
            if (extras != null) {
                apiName = extras.getString(Constants.Account.API_NAME);
            }
            
            return apiName;
        }
    }

    /**
     * Returns the current DB name.
     * 
     * @return the current DB name.
     */
    public String getAPIDB() {
        if (getAPIName() != null) {
            return getMemberId() + "@" + getAPIName();
        } else {
            return null;
        }
    }

    /**
     * Returns the current session key.
     * 
     * @return the current session key.
     */
    public String getSessionKey() {
        if (mSessionKey != null) {
            return mSessionKey;
        } else {
            Bundle extras = getIntent().getExtras();
            
            String sessionKey = null;
            
            if (extras != null) {
                sessionKey = extras.getString(Constants.Account.SESSION_KEY);
            }
            
            return sessionKey;
        }
    }
    
    /**
     * Returns the current member id.
     * 
     * @return the current member id.
     */
    public String getMemberId() {
        if (mMemberId != null) {
            return mMemberId;
        } else {
            Bundle extras = getIntent().getExtras();
            
            String memberId = null;
            
            if (extras != null) {
                memberId = extras.getString(Constants.Account.MEMBER_ID);
            }
            
            return memberId;
        }
    }
    
    public void share(String subject, String text) {
        final Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);

        startActivity(Intent.createChooser(intent, getString(R.string.title_share)));
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
    }
    
    public Uri dbUri(Uri uri) {
        return dbUri(uri.toString());
    }
    
    public Uri dbUri(String path) {
        String apiDB = getAPIDB();
        
        if (apiDB != null) {
            return Uri.parse(path).buildUpon().appendQueryParameter("db", getAPIDB()).build();
        } else {
            return Uri.parse(path);
        }
    }
    
    public boolean isResultEmpty(Uri uri, String selection, String[] selectionArgs, String orderBy) {
        boolean empty;
        
        Cursor c = getContentResolver().query(uri, null, selection, selectionArgs, orderBy);
        c.moveToFirst();
        
        if (c.isAfterLast()) {
            empty = true;
        } else {
            empty = false;
        }
        c.close();
        
        return empty;
    }
    
    public Integer queryInteger(Uri uri, String column, String selection, String[] selectionArgs, String orderBy) {
        return queryInteger(uri, column, null, selection, selectionArgs, orderBy);
    }

    public Integer queryInteger(Uri uri, String column, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        Cursor c = getContentResolver().query(uri, projection, selection, selectionArgs, orderBy);
    
        c.moveToFirst();
        
        
        Integer integer = null;
    
        if (!c.isAfterLast()) {
            integer = c.getInt(c.getColumnIndex(column));
        }
    
        c.close();
        
        return integer;
    }

    public String queryString(Uri uri, String column, String selection, String[] selectionArgs, String orderBy) {
        return queryString(uri, column, null, selection, selectionArgs, orderBy);
    }

    public String queryString(Uri uri, String column, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        Cursor c = getContentResolver().query(uri, projection, selection, selectionArgs, orderBy);
    
        c.moveToFirst();
        
        
        String str = null;
    
        if (!c.isAfterLast()) {
            str = c.getString(c.getColumnIndex(column));
        }
    
        c.close();
        
        return str;
    }

    public int queryCount(Uri uri, String selection, String[] selectionArgs, String orderBy) {
        return queryCount(uri, null, selection, selectionArgs, orderBy);
    }

    public int queryCount(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        Cursor c = getContentResolver().query(uri, projection, selection, selectionArgs, orderBy);
    
        c.moveToFirst();
        int count = c.getCount();
        c.close();
        
        return count;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        Log.d("XXX", "onActivityResult: " + requestCode + ", " + resultCode);
        
        switch(requestCode) { 
            case (42) : { // open accounts 
              if (resultCode == Activity.RESULT_OK) { 
              
                      Account account = intent.getParcelableExtra(Constants.Account.NAME);
                      
                      if (account == null) return;
                      
                      AccountManager am = AccountManager.get(getApplicationContext());
                      
                      Log.d("XXX", "Selected account: " + account);
                      
                      Uri accountUri = DBSystemProvider.ACCOUNT_CONTENT_URI;

                      // clear all last active entries
                      ContentValues valuesActive = new ContentValues();
                      valuesActive.put(DBSystem.Account.COLUMN_LAST_ACTIVE, 0);
                      getContentResolver().update(accountUri, valuesActive, null, null);

                      // save last active entry and member + session values
                      ContentValues values = new ContentValues();
                      values.put(DBSystem.Account.COLUMN_NAME, am.getUserData(account, Constants.Account.API_NAME));
                      values.put(DBSystem.Account.COLUMN_URL, am.getUserData(account, Constants.Account.API_URL));
//                      values.put(DBSystem.Account.COLUMN_API_KEY, am.getUserData(account, Constants.Account.));
                      
                      values.put(DBSystem.Account.COLUMN_MEMBER_ID, am.getUserData(account, Constants.Account.MEMBER_ID));
                      values.put(DBSystem.Account.COLUMN_SESSION_KEY, am.getUserData(account, Constants.Account.SESSION_KEY));
                      values.put(DBSystem.Account.COLUMN_LAST_ACTIVE, 1);
                      values.put(DBSystem.Account.COLUMN_META_CACHED, System.currentTimeMillis());
                      
                      int updated = getContentResolver().update(accountUri, values,
                              DBSystem.Account.COLUMN_ID + " = ? AND " + DBSystem.Account.COLUMN_NAME + " = ?",
                              new String[] { am.getUserData(account, Constants.Account.MEMBER_ID),
                                  am.getUserData(account, Constants.Account.API_NAME) });
                      
                      if (updated == 0) {
                          Log.d("XXX", "updated == 0");
                          getContentResolver().insert(accountUri, values);
                      }
                      
                      // start member activity
                      Bundle extras = new Bundle();
                      extras.putString(Constants.Account.API_NAME,    am.getUserData(account, Constants.Account.API_NAME));
                      extras.putString(Constants.Account.API_URL,     am.getUserData(account, Constants.Account.API_URL));
                      extras.putString(Constants.Account.MEMBER_ID,   am.getUserData(account, Constants.Account.MEMBER_ID));
                      extras.putString(Constants.Account.SESSION_KEY, am.getUserData(account, Constants.Account.SESSION_KEY));

                      Intent i = new Intent().setClass(getApplicationContext(), MemberActivity.class);
                      i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                      i.putExtras(extras);
                      startActivity(i);
              }
              break; 
            } 
          } 
    }
    
    
}