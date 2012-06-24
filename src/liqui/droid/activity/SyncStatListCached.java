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

import liqui.droid.db.DB;
import liqui.droid.db.DBProvider;
import liqui.droid.holder.BreadCrumbHolder;
import liqui.droid.Constants;
import liqui.droid.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * The Class SyncStatListCached.
 */
public class SyncStatListCached extends Base implements LoaderCallbacks<Cursor> {

    public Account mAccount;
    
    protected SyncStatCursorAdapter mAdapter;
    
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.act_generic_list);
        setUpActionBar();

        createBreadcrumb(getString(R.string.sync_stat_statistics), (BreadCrumbHolder[]) null);
        
        Bundle extras = getIntent().getExtras();

        // Check from the saved Instance
        mAccount = (bundle == null) ? null : (Account)bundle.getParcelable("ACCOUNT");

        // Or passed from the other activity
        if (extras != null) {
            mAccount = extras.getParcelable("ACCOUNT");
        }

        getSupportLoaderManager().initLoader(2, null, this);
        mAdapter = new SyncStatCursorAdapter(this, null, true);
        
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(mAdapter);
        registerForContextMenu(listView);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        
        AccountManager am = AccountManager.get(getApplicationContext());
        String name       = am.getUserData(mAccount, Constants.Account.API_NAME);
        String memberId   = am.getUserData(mAccount, Constants.Account.MEMBER_ID);
        
        Uri uri = DBProvider.SYNC_RUN_CONTENT_URI.buildUpon().appendQueryParameter("db", memberId + "@" + name).build();
        
        // Log.d("XXX", "Uri " + uri);
        
        return new CursorLoader(getApplication(), uri, null, null, null, "_id DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cl, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursor) {
        mAdapter.swapCursor(null);
    }
    
    /**
     * The SyncStat cursor adapter.
     */
    public class SyncStatCursorAdapter extends CursorAdapter {

        public SyncStatCursorAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        public SyncStatCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            Long   syncTime      = cursor.getLong(cursor.getColumnIndex(DB.SyncRun.COLUMN_SYNC_TIME));
            Long   syncDuration  = cursor.getLong(cursor.getColumnIndex(DB.SyncRun.COLUMN_SYNC_DURATION));
            Integer syncFail     = cursor.getInt(cursor.getColumnIndex(DB.SyncRun.COLUMN_SYNC_FAIL));
            
            TextView tv_summary = (TextView)view.findViewById(R.id.tv_title);
            
            String time = DateTimeFormat.shortDateTime().print(new DateTime(syncTime));
            
            tv_summary.setText(time);
            
            TextView tv_desc = (TextView)view.findViewById(R.id.tv_desc);
            tv_desc.setVisibility(View.GONE);

            TextView tv_desc2 = (TextView)view.findViewById(R.id.tv_desc2);
            tv_desc2.setVisibility(View.GONE);

            TextView tv_extra = (TextView)view.findViewById(R.id.tv_extra);
            tv_extra.setText("fail: " + syncFail + " in " + syncDuration / 1000 + " sec");
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.row_simple_4, viewGroup, false);
            bindView(v, context, cursor);
            return v;
        }

    }
}
