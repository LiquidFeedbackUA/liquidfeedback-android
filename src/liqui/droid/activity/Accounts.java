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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.ocpsoft.pretty.time.PrettyTime;

import java.util.Date;

import liqui.droid.Constants;
import liqui.droid.db.DBProvider;
import liqui.droid.holder.BreadCrumbHolder;
import liqui.droid.R;

/**
 * The Class Accounts.
 */
public class Accounts extends Base implements OnItemClickListener {
    
    protected AccountManager mAccountManager;
    
    protected Intent mIntent;
    
    protected AccountsAdapter mAdapter;
    
    protected ListView mListView;
    
    protected Button mButtonAccountAdd;
    
    protected static final int ACTIVITY_SHOW_STATS = 1234;

    protected static final int SYNC_STAT_ID = Menu.FIRST + 1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.act_account_list);
        setUpActionBar();
        setBreadCrumbs();
        
        mAccountManager = AccountManager.get(getApplicationContext());
        Account[] accounts = mAccountManager.getAccountsByType(Constants.Account.TYPE);
        
        mAdapter = new AccountsAdapter(this, R.layout.row_account, accounts);
        
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);
        
        mButtonAccountAdd = (Button) findViewById(R.id.btn_account_add);
        mButtonAccountAdd.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                accountAdd();
            }
        });

    }
    
    protected void accountAdd() {
        Intent intent = new Intent().setClass(Accounts.this, LiquiDroid.class);
        
        intent.setAction(getString(R.string.action_login_sync));
        startActivity(intent);
        
        finish();
    }
    
    /**
     * Sets the bread crumbs.
     */
    protected void setBreadCrumbs() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[0];

        /*
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(getResources().getString(R.string.title_explore));
        b.setTag(Constants.EXPLORE);
        breadCrumbHolders[0] = b;
          */
        
        createBreadcrumb(getString(R.string.menu_accounts), breadCrumbHolders);
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Account account = (Account)mListView.getItemAtPosition(position);
        Intent intent = new Intent();
        intent.putExtra(Constants.Account.NAME, account);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, SYNC_STAT_ID, 0, getString(R.string.account_ctx_sync_stat));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SYNC_STAT_ID:
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                        .getMenuInfo();

                Intent intent = new Intent().setClass(Accounts.this, SyncStatListCached.class);

                intent.putExtra("ACCOUNT", mAdapter.getItem(info.position));
                
                startActivityForResult(intent, ACTIVITY_SHOW_STATS);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    public class AccountsAdapter extends ArrayAdapter<Account> {

        public AccountsAdapter(Context context, int textViewResourceId, Account[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder = null;
            
            if (row == null)
            {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.row_account, parent, false);
                
                holder = new ViewHolder();
                holder.tvTitle = (TextView)row.findViewById(R.id.tv_title);
                holder.tvDesc  = (TextView)row.findViewById(R.id.tv_desc);
                
                row.setTag(holder);
            } else {
                holder = (ViewHolder)row.getTag();
            }
            
            Account account = getItem(position);
            
            holder.tvTitle.setText(account.name);
            
            Uri uri = dbUri(DBProvider.SYNC_RUN_CONTENT_URI);
            String lastUpdate = queryString(uri, "last_update", new String[] { "MAX(sync_time) AS last_update" }, null, null, null);
            
            if (lastUpdate != null && !lastUpdate.equals("0")) {
                PrettyTime pt = new PrettyTime();
                Date dt = new DateTime(Long.parseLong(lastUpdate)).toDate();
                holder.tvDesc.setText(getResources().getString(R.string.last_update) + " " + pt.format(dt));
            } else {
                holder.tvDesc.setText(getResources().getString(R.string.last_update_never));
            }
            
            return row;
        }
        
        class ViewHolder {
            TextView tvTitle;
            TextView tvDesc;
        }
    }
}
