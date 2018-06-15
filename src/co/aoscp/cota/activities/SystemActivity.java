package co.aoscp.cota.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import co.aoscp.cota.R;
import co.aoscp.cota.helpers.DownloadHelper;
import co.aoscp.cota.helpers.RebootHelper;
import co.aoscp.cota.helpers.RecoveryHelper;
import co.aoscp.cota.receivers.DownloadReceiver;
import co.aoscp.cota.services.UpdateService;
import co.aoscp.cota.updater.RomUpdater;
import co.aoscp.cota.updater.Updater.PackageInfo;
import co.aoscp.cota.updater.Updater.UpdaterListener;
import co.aoscp.cota.utils.Constants;
import co.aoscp.cota.utils.DeviceInfoUtils;
import co.aoscp.cota.utils.FileUtils;

import org.piwik.sdk.DownloadTracker;
import org.piwik.sdk.PiwikApplication;
import org.piwik.sdk.TrackHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SystemActivity extends AppCompatActivity implements UpdaterListener, 
    DownloadHelper.DownloadCallback {
    private static final String TAG = "COTA:SystemActivity";

    private int mState;
    private static final int STATE_CHECK = 0;
    private static final int STATE_FOUND = 1;
    private static final int STATE_DOWNLOADING = 2;
    private static final int STATE_INSTALL = 3;
    private static final int STATE_ERROR = 4;

    private RomUpdater mRomUpdater;
    private RebootHelper mRebootHelper;

    private PackageInfo mUpdatePackage;
    private List<File> mFiles = new ArrayList<>();

	private DeviceInfoUtils mDeviceUtils;
	
    private UpdateService.NotificationInfo mNotificationInfo;
      
    private Context mContext;
      
    protected Context getContext() {
        return mContext;
    }

    private CoordinatorLayout mCoordinatorLayout;
    private TextView mMessage;
    private TextView mSize;
    private Button mButton;
    private TextView mHeader;
    private ProgressBar bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	UpdateService.start(this);
        setContentView(R.layout.activity_system);

        mHeader = (TextView) findViewById(R.id.header);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mMessage = (TextView) findViewById(R.id.message);
	mSize = (TextView) findViewById(R.id.size);
        mButton = (Button) findViewById(R.id.action);
	
	bar = (ProgressBar) findViewById(R.id.progress_bar);

        mUpdatePackage = null;
        DownloadHelper.init(this, this);
        mRomUpdater = new RomUpdater(this, true);
        mRebootHelper = new RebootHelper(new RecoveryHelper(SystemActivity.this));

        mButton.setOnClickListener(mButtonListener);
        mRomUpdater.addUpdaterListener(this);

        // Check for M permission to write on external
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        if (mNotificationInfo != null) {
            if (mNotificationInfo.mNotificationId == UpdateService.NOTIFICATION_UPDATE) {
                mRomUpdater.setLastUpdates(mNotificationInfo.mPackageInfosRom);
            } else {
                mRomUpdater.check(true);
            }
        } else if (DownloadHelper.isDownloading()) {
            mState = STATE_DOWNLOADING;
            updateMessages((PackageInfo) null);
        } else {
            mRomUpdater.check(true);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mNotificationInfo = null;
        if (intent != null && intent.getExtras() != null) {
            mNotificationInfo = (UpdateService.NotificationInfo) intent.getSerializableExtra(UpdateService.FILES_INFO);
            if (intent.getBooleanExtra(DownloadReceiver.CHECK_DOWNLOADS_FINISHED, false)) {
                DownloadHelper.checkDownloadFinished(this,
                        intent.getLongExtra(DownloadReceiver.CHECK_DOWNLOADS_ID, -1L));
            }
        }
    }
	
    @SuppressLint("MissingSuperCall")
    @Override
    protected void onStart() {
        super.onStart();
	overridePendingTransition(R.anim.slide_next_in, R.anim.slide_next_out);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onResume() {
        super.onResume();
        DownloadHelper.registerCallback(this);
	overridePendingTransition(R.anim.slide_next_in, R.anim.slide_next_out);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onPause() {
        super.onPause();
        DownloadHelper.unregisterCallback();
    }
	
    @Override
    public void startChecking() {
        mState = STATE_CHECK;
        updateMessages(mUpdatePackage);
    }

    @Override
    public void versionFound(PackageInfo[] info) {
	//An update has been found
        mState = STATE_FOUND;
	if (info != null && info.length > 0) {
            if(FileUtils.isOnDownloadList(this, info[0].getFilename())) {
                //Now that the package is download, lets queue the install
                mState = STATE_INSTALL;
                addFile(FileUtils.getFile(this, info[0].getFilename()), info[0].getMd5());
            }
        }
        updateMessages(info);
    }

    private void updateMessages(PackageInfo[] info) {
        if (info != null && info.length > 0) {
            updateMessages(info.length > 0 ? info[0] : null);
        }
    }
      
    public void setProgress(int max, int progress) {
        if (mState != STATE_DOWNLOADING) {
            return;
        }
    }

    private void updateMessages(PackageInfo info) {
        mUpdatePackage = info;
        switch (mState) {
            default:
            case STATE_CHECK:
                if (mUpdatePackage == null) {
                    mHeader.setText(R.string.no_updates_title);
		    mMessage.setText(String.format(
                            getResources().getString(R.string.no_updates_text),
		            mDeviceUtils.getVersionDisplay(),
	                    mDeviceUtils.getVersionRelease(),
	                    mDeviceUtils.getRealTime()));
                    mButton.setText(R.string.no_updates_check);
	            bar.setVisibility(View.GONE);
                    Log.v(TAG, "updateMessages:STATE_CHECK = mUpdatePackage != null");
                }
                Log.v(TAG, "updateMessages:STATE_CHECK = mUpdatePackage == null");
                break;
            case STATE_FOUND:
                if (!mRomUpdater.isScanning() && mUpdatePackage != null) {
                    mHeader.setText(R.string.update_found_title);
                    mMessage.setText(String.format(
                            getResources().getString(R.string.update_found_text),
	                    mUpdatePackage.getVersion(),
		            mDeviceUtils.getModel(),
		            mUpdatePackage.getText()));
		    mSize.setText(String.format(
                            getResources().getString(R.string.update_found_size),
                            Formatter.formatShortFileSize(this, Long.decode(mUpdatePackage.getSize()))));
                    mButton.setText(R.string.update_found_download);
	            bar.setVisibility(View.GONE);
                    Log.v(TAG, "updateMessages:STATE_FOUND = " + Formatter.formatShortFileSize(this, Long.decode(mUpdatePackage.getSize())));
                }
                Log.v(TAG, "updateMessages:STATE_FOUND = mRomUpdater.isScanning || mRom == null");
                break;
            case STATE_DOWNLOADING:
	        UpdateService.stopNotificationUpdate();
                mHeader.setText(R.string.downloading_title);
                mMessage.setText(R.string.downloading_text);
                mButton.setText(R.string.downloading_cancel);
		bar.setVisibility(View.VISIBLE);
                Log.v(TAG, "updateMessages:STATE_DOWNLOADING = " + (R.string.downloading_text));
                break;
            case STATE_ERROR:
	        UpdateService.stopNotificationUpdate();
                mHeader.setText(R.string.download_failed_title);
                mMessage.setText(R.string.download_failed_text);
                mButton.setText(R.string.no_updates_check);
		bar.setVisibility(View.GONE);
                Log.v(TAG, "updateMessages:STATE_ERROR");
                break;
            case STATE_INSTALL:
		UpdateService.stopNotificationUpdate();
                mHeader.setText(R.string.install_title);
                mMessage.setText(R.string.install_text);
                mButton.setText(R.string.install_action);
		bar.setVisibility(View.GONE);
                Log.v(TAG, "updateMessages:STATE_INSTALL");
                break;
        }
    }
    
    private final Button.OnClickListener mButtonListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
	if (Constants.DEBUG) Log.d(TAG, "Button clicked. mState = " + mState);
            switch (mState) {
                default:
                case STATE_CHECK:
                    mState = STATE_CHECK;
                    mRomUpdater.check(true);
		    updateMessages((PackageInfo) null);
                    Log.v(TAG, "onClick:STATE_CHECK");
                    break;
                case STATE_FOUND:
                    if (!mRomUpdater.isScanning() && mUpdatePackage != null) {
                        mState = STATE_DOWNLOADING;
                        DownloadHelper.registerCallback(SystemActivity.this);
                        DownloadHelper.downloadFile(mUpdatePackage.getPath(),
                                mUpdatePackage.getFilename(), mUpdatePackage.getMd5());
                        updateMessages(mUpdatePackage);
                        TrackHelper.track().download().version(mUpdatePackage.getFilename()).with(((PiwikApplication) getApplication()).getTracker());
                        Log.v(TAG, "onClick:STATE_FOUND = " + DeviceInfoUtils.getDevice() + ":" + mUpdatePackage.getFilename());
                    }
                    Log.v(TAG, "onClick:STATE_FOUND = mRomUpdater.isScanning || mUpdatePackage == null");
                    break;
                case STATE_DOWNLOADING:
                    mState = STATE_CHECK;
                    DownloadHelper.clearDownloads();
                    updateMessages((PackageInfo) null);
                    Log.v(TAG, "onClick:STATE_DOWNLOADING");
                    break;
                case STATE_ERROR:
                    mState = STATE_CHECK;
                    mRomUpdater.check(true);
                    Log.v(TAG, "onClick:STATE_ERROR");
                    break;
                case STATE_INSTALL:
                    String[] items = new String[mFiles.size()];
                    for (int i = 0; i < mFiles.size(); i++) {
                        File file = mFiles.get(i);
                        items[i] = file.getAbsolutePath();
                    }
                    mRebootHelper.showRebootDialog(SystemActivity.this, items);
                    Log.v(TAG, "onClick:STATE_INSTALL = " + android.text.TextUtils.join(", ", items));
                    break;
            }
        }
    };

    @Override
    public void onDownloadStarted() {
        mState = STATE_DOWNLOADING;
	onDownloadProgress(-1);
    }

    @Override
    public void onDownloadError(String reason) {
        mState = STATE_ERROR;
        updateMessages((PackageInfo) null);
    }
	
	 @Override
    public void onDownloadProgress(int progress) {
	if (progress >= 0 && progress <= 100) {
            bar.setProgress(progress);
        }
    }

    @Override
    public void onDownloadFinished(Uri uri, final String md5) {
        if (uri != null) {
            mState = STATE_INSTALL;
            updateMessages((PackageInfo) null);
            addFile(uri, md5);
	    UpdateService.startNotificationInstall(getContext());
        } else {
            mState = STATE_CHECK;
            mRomUpdater.check(true);
        }
    }

    public void addFile(Uri uri, final String md5) {
        String filePath = uri.toString().replace("file://", "");
        File file = new File(filePath);
        addFile(file, md5);
    }

    private void addFile(final File file, final String md5) {
        if (md5 != null && !"".equals(md5)) {
            final Snackbar md5Snackbar = Snackbar.make(mCoordinatorLayout, R.string.calculating_md5, Snackbar.LENGTH_INDEFINITE);
            md5Snackbar.show();
            new Thread() {
                public void run() {
                    final String calculatedMd5 = FileUtils.md5(file);
                    md5Snackbar.dismiss();
                    runOnUiThread(new Runnable() {

                        public void run() {
                            if (md5.equals(calculatedMd5)) {
                                reallyAddFile(file);
                            }
                            else {
                                showMd5Mismatch(file);
                            }
                        }
                    });
                }
            }.start();

        } else {
            reallyAddFile(file);
        }
    }

    private void reallyAddFile(final File file) {
        mFiles.add(file);
    }

    private void showMd5Mismatch(final File file) {
        Snackbar.make(mCoordinatorLayout, R.string.md5_mismatch, Snackbar.LENGTH_LONG)
                .setAction(R.string.md5_install_anyway, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reallyAddFile(file);
                    }
                })
                .show();
    }

    @Override
    public void checkError(String cause) {
    }
}