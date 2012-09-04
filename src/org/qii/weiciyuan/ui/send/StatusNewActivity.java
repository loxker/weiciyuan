package org.qii.weiciyuan.ui.send;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.bean.AccountBean;
import org.qii.weiciyuan.bean.GeoBean;
import org.qii.weiciyuan.dao.send.StatusNewMsgDao;
import org.qii.weiciyuan.othercomponent.PhotoUploadService;
import org.qii.weiciyuan.support.database.DatabaseManager;
import org.qii.weiciyuan.support.error.WeiboException;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.ui.Abstract.AbstractAppActivity;
import org.qii.weiciyuan.ui.widgets.SendProgressFragment;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * User: qii
 * Date: 12-7-29
 */
public class StatusNewActivity extends AbstractAppActivity implements DialogInterface.OnClickListener {


    private static final int CAMERA_RESULT = 0;
    private static final int PIC_RESULT = 1;
    protected String token = "";

    private String picPath = "";

    private String imageFilePath = "";

    private GeoBean geoBean;


    private TextView contentNumber = null;
    private ImageView haveGPS = null;
    private ImageView havePic = null;
    private EditText content = null;

    private String location;


    @Override
    public void onClick(DialogInterface dialog, int which) {

        switch (which) {
            case 0:

                imageFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/myfavoritepicture.jpg";
                File imageFile = new File(imageFilePath);
                Uri imageFileUri = Uri.fromFile(imageFile);
                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri);
                startActivityForResult(i, CAMERA_RESULT);
                break;
            case 1:
                Intent choosePictureIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(choosePictureIntent, PIC_RESULT);
                break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {

            if (TextUtils.isEmpty(content.getText().toString())) {
                content.setText(getString(R.string.share_pic));
                content.setSelection(content.getText().toString().length());
            }

            switch (requestCode) {
                case CAMERA_RESULT:
                    picPath = imageFilePath;
                    break;
                case PIC_RESULT:
                    Uri imageFileUri = intent.getData();
                    picPath = getPicPathFromUri(imageFileUri);
                    break;
            }
            havePic.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statusnewactivity_layout);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.write_weibo);

        View title = getLayoutInflater().inflate(R.layout.statusnewactivity_title_layout, null);
        contentNumber = (TextView) title.findViewById(R.id.content_number);
        haveGPS = (ImageView) title.findViewById(R.id.have_gps);
        haveGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String geoUriString = "geo:" + geoBean.getLat() + "," + geoBean.getLon() + "?q=" + location;
                Uri geoUri = Uri.parse(geoUriString);
                Intent mapCall = new Intent(Intent.ACTION_VIEW, geoUri);
                PackageManager packageManager = getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(mapCall, 0);
                boolean isIntentSafe = activities.size() > 0;
                if (isIntentSafe) {
                    startActivity(mapCall);
                }
            }
        });
        haveGPS.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                haveGPS.setVisibility(View.GONE);
                geoBean = null;
                return true;
            }
        });
        havePic = (ImageView) title.findViewById(R.id.have_pic);

        havePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(picPath)), "image/png");
                PackageManager packageManager = getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
                boolean isIntentSafe = activities.size() > 0;
                if (isIntentSafe) {
                    startActivity(intent);
                }
            }
        });

        havePic.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                havePic.setVisibility(View.GONE);
                picPath = "";
                return true;
            }
        });
        actionBar.setCustomView(title, new ActionBar.LayoutParams(Gravity.RIGHT));
        actionBar.setDisplayShowCustomEnabled(true);
        content = ((EditText) findViewById(R.id.status_new_content));
        content.addTextChangedListener(new TextNumLimitWatcher(contentNumber, content, this));

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent);
            } else if (type.startsWith("image/")) {
                handleSendImage(intent);
            }
        } else {
            token = intent.getStringExtra("token");
            String accountName = intent.getStringExtra("accountName");
            getActionBar().setSubtitle(accountName);
            String contentTxt = intent.getStringExtra("content");
            if (!TextUtils.isEmpty(contentTxt)) {
                content.setText(contentTxt + " ");
                content.setSelection(content.getText().toString().length());
            }
        }
    }


    private void getAccountInfo() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String id = sharedPref.getString("id", "");
        if (!TextUtils.isEmpty(id)) {
            AccountBean bean = DatabaseManager.getInstance().getAccount(id);
            if (bean != null) {
                token = bean.getAccess_token();
                getActionBar().setSubtitle(bean.getUsernick());
            } else {
                List<AccountBean> accountList = DatabaseManager.getInstance().getAccountList();
                if (accountList != null && accountList.size() > 0) {
                    AccountBean account = accountList.get(0);
                    token = account.getAccess_token();
                    getActionBar().setSubtitle(account.getUsernick());
                }
            }
        }
    }

    private void handleSendText(Intent intent) {
        getAccountInfo();
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
            content.setText(sharedText);
            content.setSelection(content.getText().toString().length());
        }

    }


    private void handleSendImage(Intent intent) {
        getAccountInfo();

        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        picPath = getPicPathFromUri(imageUri);
        content.setText(getString(R.string.share_pic));
        content.setSelection(content.getText().toString().length());
    }

    private String getPicPathFromUri(Uri uri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    private boolean canSend() {

        boolean haveContent = !TextUtils.isEmpty(content.getText().toString());
        boolean haveToken = !TextUtils.isEmpty(token);
        boolean contentNumBelow140 = (content.getText().toString().length() < 140);

        if (haveContent && haveToken && contentNumBelow140) {
            return true;
        } else {
            if (!haveContent && !haveToken) {
                Toast.makeText(this, getString(R.string.content_cant_be_empty_and_dont_have_account), Toast.LENGTH_SHORT).show();
            } else if (!haveContent) {
                content.setError(getString(R.string.content_cant_be_empty));
            } else if (!haveToken) {
                Toast.makeText(this, getString(R.string.dont_have_account), Toast.LENGTH_SHORT).show();
            }

            if (!contentNumBelow140) {
                content.setError(getString(R.string.content_words_number_too_many));
            }

        }

        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.statusnewactivity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm.isActive())
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
                onBackPressed();
                break;
            case R.id.menu_add_gps:
                getLocation();
                break;
            case R.id.menu_add_pic:
                new MyAlertDialogFragment().show(getFragmentManager(), "");
                break;

            case R.id.menu_send:
                String value = content.getText().toString();
                if (canSend()) {
                    executeTask(value);
                }
                break;
        }
        return true;
    }

    protected void executeTask(String content) {

        if (TextUtils.isEmpty(picPath)) {
            new StatusNewTask(content).execute();
        } else {
            Intent intent = new Intent(StatusNewActivity.this, PhotoUploadService.class);
            intent.putExtra("token", token);
            intent.putExtra("picPath", picPath);
            intent.putExtra("content", content);
            intent.putExtra("geo", geoBean);
            startService(intent);
            finish();
        }
    }

    class StatusNewTask extends AsyncTask<Void, String, String> {
        String content;

        StatusNewTask(String content) {
            this.content = content;
        }

        SendProgressFragment progressFragment = new SendProgressFragment();

        @Override
        protected void onPreExecute() {
            progressFragment.onCancel(new DialogInterface() {

                @Override
                public void cancel() {
                    StatusNewTask.this.cancel(true);
                }

                @Override
                public void dismiss() {
                    StatusNewTask.this.cancel(true);
                }
            });

            progressFragment.show(getSupportFragmentManager(), "");

        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                boolean result = new StatusNewMsgDao(token).setGeoBean(geoBean).sendNewMsg(content);
            } catch (WeiboException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            return null;
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
            progressFragment.dismissAllowingStateLoss();
            Toast.makeText(StatusNewActivity.this, getString(R.string.send_failed), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(String s) {
            progressFragment.dismissAllowingStateLoss();
            finish();
            Toast.makeText(StatusNewActivity.this, getString(R.string.send_successfully), Toast.LENGTH_SHORT).show();
            super.onPostExecute(s);

        }
    }


    private class MyAlertDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            String[] items = {getString(R.string.take_camera), getString(R.string.select_pic)};

            AlertDialog.Builder builder = new AlertDialog.Builder(StatusNewActivity.this)
                    .setTitle(getString(R.string.select))
                    .setItems(items, StatusNewActivity.this);
            return builder.create();
        }
    }

    private void getLocation() {
        LocationManager locationManager = (LocationManager) StatusNewActivity.this
                .getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(StatusNewActivity.this, getString(R.string.gps_is_searching), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(StatusNewActivity.this, getString(R.string.please_open_gps), Toast.LENGTH_SHORT).show();
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0,
                locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0,
                locationListener);
    }


    private void updateWithNewLocation(Location result) {
        haveGPS.setVisibility(View.VISIBLE);
        geoBean = new GeoBean();
        geoBean.setLatitude(result.getLatitude());
        geoBean.setLongitude(result.getLongitude());
        new GetGoogleLocationInfo(geoBean).execute();
        ((LocationManager) StatusNewActivity.this
                .getSystemService(Context.LOCATION_SERVICE)).removeUpdates(locationListener);

    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);

        }

        public void onProviderDisabled(String provider) {

        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status,
                                    Bundle extras) {
        }
    };


    private class GetGoogleLocationInfo extends MyAsyncTask<Void, String, String> {

        GeoBean geoBean;

        public GetGoogleLocationInfo(GeoBean geoBean) {
            this.geoBean = geoBean;
        }

        @Override
        protected String doInBackground(Void... params) {

            Geocoder geocoder = new Geocoder(StatusNewActivity.this, Locale.getDefault());

            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(geoBean.getLat(), geoBean.getLon(), 1);
            } catch (IOException e) {
                cancel(true);
            }
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);

                StringBuilder builder = new StringBuilder();
                int size = address.getMaxAddressLineIndex();
                for (int i = 0; i < size; i++) {
                    builder.append(address.getAddressLine(i));
                }
                return builder.toString();
            }

            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(StatusNewActivity.this, s, Toast.LENGTH_SHORT).show();
            location = s;
            super.onPostExecute(s);
        }
    }
}
