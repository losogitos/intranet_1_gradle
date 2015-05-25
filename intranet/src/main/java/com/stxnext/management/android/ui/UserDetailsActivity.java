
package com.stxnext.management.android.ui;

import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.stxnext.management.android.R;
import com.stxnext.management.android.dto.local.AbsenceDisplayData;
import com.stxnext.management.android.dto.local.IntranetUser;
import com.stxnext.management.android.dto.local.UserProperty;
import com.stxnext.management.android.receivers.CommandReceiver;
import com.stxnext.management.android.receivers.CommandReceiver.CommandReceiverListener;
import com.stxnext.management.android.sync.ContactSyncManager;
import com.stxnext.management.android.sync.ContactSyncManager.SyncManagerListener;
import com.stxnext.management.android.sync.ProviderPhone;
import com.stxnext.management.android.ui.dependencies.AsyncTaskEx;
import com.stxnext.management.android.ui.dependencies.BitmapUtils;
import com.stxnext.management.android.ui.dependencies.PropertyListAdapter;
import com.stxnext.management.android.ui.dependencies.RoundedDrawable;

public class UserDetailsActivity extends AbstractSimpleActivity implements SyncManagerListener,
        CommandReceiverListener {

    public static final String EXTRA_USER = "user";

    ImageView userImageView;
    TextView nameView;
    ListView listView;
    TextView lateTimeView;
    TextView lateDescriptionView;
    PropertyListAdapter adapter;

    ViewGroup loadingView;
    ViewGroup loadedView;

    IntranetUser user;
    ContactSyncManager syncManager;
    private CommandReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.receiver = new CommandReceiver(this);
        registerReceiver(receiver, new IntentFilter(
                CommandReceiver.ACTION_ACTIVITY_COMMAND));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    protected void fillViews() {
        userImageView = (ImageView) findViewById(R.id.userImageView);
        nameView = (TextView) findViewById(R.id.nameView);
        listView = (ListView) findViewById(R.id.listView);
        loadingView = (ViewGroup) findViewById(R.id.loadingView);
        loadedView = (ViewGroup) findViewById(R.id.loadedView);
        lateTimeView = (TextView) findViewById(R.id.lateTimeView);
        lateDescriptionView = (TextView) findViewById(R.id.lateDescriptionView);

        // userImageView.setCornersRadius(12F);

        Bundle bundle = getIntent().getExtras();
        user = (IntranetUser) bundle.getSerializable(EXTRA_USER);

        syncManager = new ContactSyncManager(this);

        nameView.setText(user.getName());
        if (user.getAbsenceDisplayData() != null)
            insertAbsenceData(user.getAbsenceDisplayData(), lateTimeView,
                    lateDescriptionView);

        if (user.getLatenessDisplayData() != null)
            insertAbsenceData(user.getLatenessDisplayData(), lateTimeView,
                    lateDescriptionView);
        new LoadDataTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_contact) {
            addContactAction();
        }
        return super.onOptionsItemSelected(item);
    }

    private void addContactAction() {
        if (prefs.isSyncing()) {
            Toast.makeText(this, R.string.notification_sync_in_progress, Toast.LENGTH_SHORT).show();
            return;
        }
        syncManager.launchQueryAsync(getSupportLoaderManager(), user.getPhone(), user.getName());
    }

    private void insertAbsenceData(AbsenceDisplayData data,
            TextView lateTimeView, TextView lateDescriptionView) {
        if (data != null) {
            lateTimeView.setText(formatLatenessTime(data));
            lateDescriptionView
                    .setText(data.explanation);
            lateTimeView.setVisibility(View.VISIBLE);
            lateDescriptionView.setVisibility(View.VISIBLE);
        } else {
            lateTimeView.setVisibility(View.GONE);
            lateDescriptionView.setVisibility(View.GONE);
        }
    }

    private String formatLatenessTime(AbsenceDisplayData data) {
        if (data.start != null && data.end != null) {
            return data.start + " - " + data.end;
        }
        return null;
    }

    @Override
    protected void setActions() {

    }

    @Override
    protected int getContentResourceId() {
        return R.layout.activity_user_details;
    }

    private void setViewLoading(boolean loading) {
        loadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
        loadedView.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private class MergeTask extends
            AsyncTaskEx<Void, Void, Void> {

        List<ProviderPhone> phones;

        public MergeTask(List<ProviderPhone> phones) {
            this.phones = phones;
        }

        @Override
        protected Void doInBackground(Void... params) {
            syncManager.mergeContacts(phones, user);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (isFinishing())
                return;
            Toast.makeText(UserDetailsActivity.this, R.string.notification_contact_updated,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private class LoadDataTask extends
            AsyncTaskEx<Void, Void, List<UserProperty>> {

        RoundedDrawable drawable;
        Resources res;

        LoadDataTask(){
            res = getResources();
        }
        
        @Override
        protected void onPreExecute() {
            setViewLoading(true);
            super.onPreExecute();
        }

        @Override
        protected List<UserProperty> doInBackground(Void... params) {
            Bitmap bitmap = BitmapUtils.getTempBitmap(UserDetailsActivity.this, user.getId()
                    .toString());
            if (bitmap != null) {
                drawable = new RoundedDrawable(bitmap);
                drawable.setCornerRadius(15F);
            }
            return user.getProperties(res);
        }

        @Override
        protected void onPostExecute(List<UserProperty> result) {
            super.onPostExecute(result);
            if (isFinishing())
                return;

            if (drawable != null) {
                userImageView.setImageDrawable(drawable);
            }
            adapter = new PropertyListAdapter(UserDetailsActivity.this,
                    listView, result);
            listView.setAdapter(adapter);
            setViewLoading(false);
            applyListAnimation(listView);
        }
    }

    private void prepareAndMergePhones(List<ProviderPhone> phones) {
        for (ProviderPhone phone : phones) {
            phone.setNumberToUpdate(user.getPhone());
        }
        new MergeTask(phones).execute();
    }

    // Content provider related
    private void showImportDialog(String content, final List<ProviderPhone> phones) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle("Import");
        alertDialog.setMessage(content);

        alertDialog.setPositiveButton(R.string.common_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        prepareAndMergePhones(phones);
                    }
                });
        alertDialog.setNegativeButton(R.string.common_no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

    @Override
    public void onPhoneQueryComplete(List<ProviderPhone> phones) {
        if (isFinishing())
            return;

        StringBuilder content = new StringBuilder();
        if (phones.size() > 0) {
            content.append(getString(R.string.notification_sync_contacts_found));
            for (ProviderPhone phone : phones) {
                content.append(phone.getDisplayName()).append("\n").append(phone.getPhoneNumber());
                content.append("\n\n");
            }
        }
        else {
            ProviderPhone phone = new ProviderPhone();
            phone.setDisplayName(user.getName());
            phone.setNumberToUpdate(user.getPhone());
            phones.add(phone);
        }
        content.append(getString(R.string.notification_sync_contacts_found_confirmation));
        showImportDialog(content.toString(), phones);
    }

    @Override
    public void onOffline() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnline() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onLostSession() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSyncStateChanged(boolean started) {
        // TODO Auto-generated method stub

    }
}
