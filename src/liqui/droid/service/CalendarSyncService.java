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

package liqui.droid.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.util.Log;

import org.joda.time.DateTime;
import org.ocpsoft.pretty.time.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import lfapi.v2.schema.Interval;
import liqui.droid.Constants;
import liqui.droid.R;
import liqui.droid.db.DB;
import liqui.droid.db.DBProvider;

/**
 * The Class CalendarSyncService.
 */
public class CalendarSyncService extends BaseService {
    
    private static boolean mSyncRunning = true;
    
    private static SyncAdapterImpl sSyncAdapter = null;
    
    private static ContentResolver mContentResolver = null;
    
    public CalendarSyncService() {
        super("CalendarSyncService");
    }

    private class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
        private Context mContext;

        public SyncAdapterImpl(Context context) {
            super(context, true);
            mContext = context;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
            try {
                CalendarSyncService.this.performSync(mContext, account, extras, authority, provider, syncResult);
            } catch (OperationCanceledException e) {
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        ret = getSyncAdapter().getSyncAdapterBinder();
        return ret;
    }

    private SyncAdapterImpl getSyncAdapter() {
        if (sSyncAdapter == null)
            sSyncAdapter = new SyncAdapterImpl(this);
        return sSyncAdapter;
    }

    private static long getCalendar(Account account) {
        
        // Find the LiquiDroid calendar if we've got one
        Uri calenderUri = Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(Calendars.ACCOUNT_NAME, account.name)
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, account.type)
                .appendQueryParameter("account_name", account.name)
                .appendQueryParameter("account_type", account.type)
                .build();
        
        
        Cursor c1 = mContentResolver.query(calenderUri, new String[] { BaseColumns._ID }, null, null, null);

        if (c1.moveToNext() && !c1.isAfterLast()) {
            Log.d("XXXXXXX", "id = " + c1.getLong(0));
            return c1.getLong(0);
        } else {
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Calendars.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(Calendars.ACCOUNT_NAME, account.name)
                    .appendQueryParameter(Calendars.ACCOUNT_TYPE, account.type)
                    .build()
                    );
            builder.withValue(Calendars.ACCOUNT_NAME, account.name);
            builder.withValue(Calendars.ACCOUNT_TYPE, account.type);
            builder.withValue(Calendars.NAME, "LiquiDroid Events");
            builder.withValue(Calendars.CALENDAR_DISPLAY_NAME, "LiquiDroid Events");
            builder.withValue(Calendars.CALENDAR_COLOR, -5159922); // orange
            builder.withValue(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
            builder.withValue(Calendars.OWNER_ACCOUNT, account.name);
            builder.withValue(Calendars.SYNC_EVENTS, 1);
            operationList.add(builder.build());
            
            Log.d("XXX", builder.build().toString());
            
            try {
                mContentResolver.applyBatch(CalendarContract.AUTHORITY, operationList);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return -1;
            }

            return getCalendar(account);
        }
    }
    
    private static void deleteEvent(Context context, Account account, long rawId) {
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, rawId).buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, account.name)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, account.type)
            .build();
        
        ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(CalendarContract.AUTHORITY);
        
        try {
            client.delete(uri, null, null);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        client.release();
    }
    
    public static class Event {
        public Date startDate, endDate;
        public String title, description, url;
        public int status, id;

        Date getStartDate() {
            return startDate;
//            return new Date(2012, 06, 23, 20, 30);
        }
        
        Date getEndDate() {
            return endDate;
//            return new Date(2012, 06, 23, 21, 30);
        }
        
        String getTitle() {
            return title;
        }
        
        String getDescription() {
            return description;
        }
        
        int getStatus() {
            return status;
        }
        
        String getUrl() {
            return url;
        }
        
        long getId() {
            return id;
        }
        
    }
    
    private static ContentProviderOperation updateEvent(long calendar_id, Account account, Event event, long raw_id) {
        ContentProviderOperation.Builder builder;
        if(raw_id != -1) {
            builder = ContentProviderOperation.newUpdate(Events.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(Calendars.ACCOUNT_NAME, account.name)
                    .appendQueryParameter(Calendars.ACCOUNT_TYPE, account.type)
                    .build()
                    );
            builder.withSelection(Events._ID + " = '" + raw_id + "'", null);
        } else {
            builder = ContentProviderOperation.newInsert(Events.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(Calendars.ACCOUNT_NAME, account.name)
                    .appendQueryParameter(Calendars.ACCOUNT_TYPE, account.type)
                    .build()
                    );
        }
        long dtstart = event.getStartDate().getTime();
        long dtend = dtstart + (1000*60*5);
        if(event.getEndDate() != null)
            dtend = event.getEndDate().getTime();
        builder.withValue(Events.CALENDAR_ID, calendar_id);
        builder.withValue(Events.DTSTART, dtstart);
        builder.withValue(Events.DTEND, dtend);
        builder.withValue(Events.TITLE, event.getTitle());
        
        String description = event.getUrl() + "\n\n";
        
        if(event.getDescription() != null && event.getDescription().length() > 0) {
            description += event.getDescription();
        }
        
        builder.withValue(Events.DESCRIPTION, description);
        
        if(Integer.valueOf(event.getStatus()) == 1)
            builder.withValue(Events.STATUS, Events.STATUS_TENTATIVE);
        else
            builder.withValue(Events.STATUS, Events.STATUS_CONFIRMED);
        builder.withValue(Events._SYNC_ID, Long.valueOf(event.getId()));
        return builder.build();
    }
    
    private static class SyncEntry {
        public Long raw_id = 0L;
    }
    
    public void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
            throws OperationCanceledException {

        AccountManager am = AccountManager.get(context.getApplicationContext());
        
        mApiName     = am.getUserData(account, Constants.Account.API_NAME);
        mApiUrl      = am.getUserData(account, Constants.Account.API_URL);
        mMemberId    = am.getUserData(account, Constants.Account.MEMBER_ID);
        mSessionKey  = am.getUserData(account, Constants.Account.SESSION_KEY);
        
        mIntent = new Intent();
        Bundle b = new Bundle();
        b.putString(Constants.Account.API_NAME,    mApiName);
        b.putString(Constants.Account.API_URL,     mApiUrl);
        b.putString(Constants.Account.MEMBER_ID,   mMemberId);
        b.putString(Constants.Account.SESSION_KEY, mSessionKey);
        mIntent.putExtras(b);
        
        Log.i("XXX", "performSync: " + account.toString() + "bundle: " + b.toString() );
        
        HashMap<Long, SyncEntry> localEvents = new HashMap<Long, SyncEntry>();
        ArrayList<Long> lqfbEvents = new ArrayList<Long>();
        mContentResolver = context.getContentResolver();

        boolean is_full_sync = true;
        
        long calendar_id = getCalendar(account);
        if (calendar_id == -1) {
            Log.e("CalendarSyncAdapter", "Unable to create LiquiDroid calendar");
            return;
        }
        
        // Load the local events
        Cursor c1 = mContentResolver.query(Events.CONTENT_URI.buildUpon().appendQueryParameter(Events.ACCOUNT_NAME, account.name).appendQueryParameter(Events.ACCOUNT_TYPE, account.type).build(), new String[] { Events._ID, Events._SYNC_ID }, Events.CALENDAR_ID + "=?", new String[] { String.valueOf(calendar_id) }, null);
        while (c1 != null && c1.moveToNext()) {
            if(is_full_sync) {
                deleteEvent(context, account, c1.getLong(0));
            } else {
                SyncEntry entry = new SyncEntry();
                entry.raw_id = c1.getLong(0);
                localEvents.put(c1.getLong(1), entry);
            }
        }
        c1.close();

        try {
            Uri uri = dbUri("content://liqui.droid.db/issues");

            String[] projection = new String[] {
                    " issue._id                   AS _id                         ",
                    " issue.population            AS issue_population            ",
                    " area.name                   AS area_name                   ",
                    
                    " issue.policy_id             AS policy_id                   ",
                    
                    " issue.created               AS issue_created               ",
                    " issue.accepted              AS issue_accepted              ",
                    " issue.half_frozen           AS issue_half_frozen           ",
                    " issue.fully_frozen          AS issue_fully_frozen          ",
                    " issue.closed                AS issue_closed                ",
                    " issue.cleaned               AS issue_cleaned               ",
                    " issue.admission_time        AS issue_admission_time        ",
                    " issue.discussion_time       AS issue_discussion_time       ",
                    " issue.voting_time           AS issue_voting_time           ",
                    " issue.snapshot              AS issue_snapshot              ",
                    " issue.latest_snapshot_event AS issue_latest_snapshot_event ",
                        
                    " issue.state                 AS issue_state                 ",
                    
                    " policy.name                 AS policy_name                 "
            };
                
            String[] selectionArgs = null;
            String selection = "area._id = issue.area_id AND issue.policy_id = policy._id AND " +
                    "(issue_state = 'admission' OR          " +
                    " issue_state = 'discussion' OR         " +
                    " issue_state = 'verification' OR       " +
                    " issue_state = 'voting')               ";      

            String   sortOrder = "_id DESC";

            Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);

            List<Event> es = new LinkedList<Event>();
            
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String issue_id         = cursor.getString(cursor.getColumnIndex("_id"));
                String issue_state      = cursor.getString(cursor.getColumnIndex("issue_state"));

                Integer policy_id       = cursor.getInt(cursor.getColumnIndex("policy_id"));
                String policy_name     = cursor.getString(cursor.getColumnIndex("policy_name"));
               
                Long created         = cursor.getLong(cursor.getColumnIndex("issue_created"));
                Long accepted        = cursor.getLong(cursor.getColumnIndex("issue_accepted"));
                Long half_frozen     = cursor.getLong(cursor.getColumnIndex("issue_half_frozen"));
                Long fully_frozen    = cursor.getLong(cursor.getColumnIndex("issue_fully_frozen"));
                Long closed          = cursor.getLong(cursor.getColumnIndex("issue_closed"));

                Uri policyUri = DBProvider.POLICY_CONTENT_URI.buildUpon().appendQueryParameter("db", getAPIDB()).build();

                Cursor c = getContentResolver().query(policyUri, null, "_id = ?",
                        new String[] { String.valueOf(policy_id) }, null);
                c.moveToFirst();
                
                String policy_admission_time = c.getString(c.getColumnIndex("admission_time"));
                String policy_discussion_time = c.getString(c.getColumnIndex("discussion_time"));
                String policy_verification_time = c.getString(c.getColumnIndex("verification_time"));
                String policy_voting_time = c.getString(c.getColumnIndex("voting_time"));
                
                c.close();
                
                if (DB.Issue.STATE_VOTING.equals(issue_state)) {
                    Event e = new Event();
                    
                    Date votingEnd = new DateTime(fully_frozen).plus(new Interval(policy_voting_time).getPeriod()).toDate();
                    
                    e.startDate = new DateTime(fully_frozen).toDate();
                    
                    e.id = 100000 + Integer.parseInt(issue_id);
                    e.status = 2;
                    e.title = "Voting on i" + issue_id + " - " + policy_name;
                    e.url = "http://dev.liquidfeedback.org/lf2/" + "issue/show/" + issue_id + ".html";
                    e.description = getResources().getString(R.string.cal_voting_starts);
                    
                    es.add(e);
                }
                
                cursor.moveToNext();
            }

            // Event[] events = server.getUserEvents(account.name);
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            for (Event event : es) {
                lqfbEvents.add(Long.valueOf(event.getId()));

                if (localEvents.containsKey(Long.valueOf(event.getId()))) {
                    SyncEntry entry = localEvents.get(Long.valueOf(event.getId()));
                    operationList.add(updateEvent(calendar_id, account, event, entry.raw_id));
                } else {
                    operationList.add(updateEvent(calendar_id, account, event, -1));
                }

                if(operationList.size() >= 50) {
                    try {
                        mContentResolver.applyBatch(CalendarContract.AUTHORITY, operationList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    operationList.clear();
                }
            }

            if(operationList.size() > 0) {
                try {
                    mContentResolver.applyBatch(CalendarContract.AUTHORITY, operationList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        
        Iterator<Long> i = localEvents.keySet().iterator();
        while(i.hasNext()) {
            Long event = i.next();
            if(!lqfbEvents.contains(event))
                deleteEvent(context, account, localEvents.get(event).raw_id);
        }
        
        mSyncRunning = false;
    }

    @Override
    protected boolean isFinished() {
        return mSyncRunning;
    }
}
