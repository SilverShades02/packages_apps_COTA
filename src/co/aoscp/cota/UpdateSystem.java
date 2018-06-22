/*
 * Copyright (C) 2018 CypherOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package co.aoscp.cota;

import android.Manifest;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settingslib.core.lifecycle.ObservableActivity;
import com.android.setupwizardlib.GlifLayout;

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
import co.aoscp.cota.utils.Utils;

import org.piwik.sdk.DownloadTracker;
import org.piwik.sdk.PiwikApplication;
import org.piwik.sdk.TrackHelper;

import java.io.File;
import java.lang.CharSequence;
import java.util.ArrayList;
import java.util.List;

public class UpdateSystem extends ObservableActivity implements UpdaterListener, 
    DownloadHelper.DownloadCallback {

    private static final String TAG = "UpdateSystem";

    private int mState;
    private static final int STATE_CHECK = 0;
    private static final int STATE_FOUND = 1;
    private static final int STATE_DOWNLOADING = 2;
    private static final int STATE_INSTALL = 3;
    private static final int STATE_ERROR = 4;

    private boolean mIsUpdate = false;
    private boolean mIsDownloading = false;

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

    private TextView mAction;
    private String mPreAction;
    private RelativeLayout mActionButton;
    private ImageView mActionIcon;

    private TextView mLearnMoreButton;

    private String mPreDescription;
    private TextView mDescription;
    private TextView mUpdateSize;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UpdateService.start(this);
        setTheme(R.style.Theme_UpdateSystem);
        setContentView(R.layout.update_system);

        mDescription = (TextView) findViewById(R.id.description_text);
        mUpdateSize = (TextView) findViewById(R.id.update_size);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mAction = (TextView) findViewById(R.id.action_text);
        mActionButton = (RelativeLayout) findViewById(R.id.action_button);
        mActionIcon = (ImageView) findViewById(R.id.action_icon);

        //mLearnMoreButton = (TextView) findViewById(R.id.update_learn_more_button);

        mUpdatePackage = null;
        DownloadHelper.init(this, this);
        mRomUpdater = new RomUpdater(this, true);
        mRebootHelper = new RebootHelper(new RecoveryHelper(UpdateSystem.this));
        mRomUpdater.addUpdaterListener(this);

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
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = Utils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initViews();
    }

    protected void initViews() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (mActionButton != null) {
            mActionButton.setOnClickListener(mActionListener);
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
    protected void onResume() {
        super.onResume();
        DownloadHelper.registerCallback(this);
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
        mState = STATE_FOUND;
        if (info != null && info.length > 0) {
            if(FileUtils.isOnDownloadList(this, info[0].getFilename())) {
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
        mIsUpdate = false;
        mIsDownloading = false;
        getLayout();
        switch (mState) {
            default:
            case STATE_CHECK:
                if (mUpdatePackage == null) {
                    setHeaderText(R.string.update_system_header_update_to_date);
                    mPreDescription = String.format(getResources().getString(
                            R.string.update_system_brief_description_update_to_date), 
                            mDeviceUtils.getVersionDisplay(),
                            mDeviceUtils.getRealTime());
                    mActionIcon.setImageResource(R.drawable.ic_action_check);
                    mPreAction = getResources().getString(R.string.update_system_action_check);
                }
                break;
            case STATE_FOUND:
                if (!mRomUpdater.isScanning() && mUpdatePackage != null) {
                    mIsUpdate = true;
                    setHeaderText(R.string.update_system_header_update_available);
                    mPreDescription = String.format(getResources().getString(
                            R.string.update_system_brief_description_update_available), 
                            mUpdatePackage.getText());
                    mActionIcon.setImageResource(R.drawable.ic_action_download_install);
                    mPreAction = getResources().getString(R.string.update_system_action_download);
                    mUpdateSize.setText(String.format(
                            getResources().getString(R.string.update_system_update_size),
                            Formatter.formatShortFileSize(this, Long.decode(mUpdatePackage.getSize()))));
                }
                break;
            case STATE_DOWNLOADING:
                mIsUpdate = true;
                mIsDownloading = true;
                setHeaderText(R.string.update_system_header_update_downloading);
                mPreDescription = String.format(getResources().getString(
                        R.string.update_system_brief_description_update_available), 
                        mUpdatePackage.getText());
                mActionIcon.setImageResource(R.drawable.ic_action_cancel);
                mPreAction = getResources().getString(R.string.update_system_action_cancel);
                mUpdateSize.setText(String.format(
                        getResources().getString(R.string.update_system_update_size),
                        Formatter.formatShortFileSize(this, Long.decode(mUpdatePackage.getSize()))));
                break;
            case STATE_ERROR:
                setHeaderText(R.string.update_system_header_update_downloading_failed);
                mPreDescription = getResources().getString(
                        R.string.update_system_brief_description_update_downloading_failed);
                mActionIcon.setImageResource(R.drawable.ic_action_check);
                mPreAction = getResources().getString(R.string.update_system_action_check);
                break;
            case STATE_INSTALL:
                setHeaderText(R.string.update_system_header_update_install);
                mPreDescription = getResources().getString(
                        R.string.update_system_brief_description_update_install);
                mActionIcon.setImageResource(R.drawable.ic_action_download_install);
                mPreAction = getResources().getString(R.string.update_system_action_install);
                break;
        }
        CharSequence styledAction = Html.fromHtml(mPreAction);
        CharSequence styledDesc = Html.fromHtml(mPreDescription);
        mAction.setText(styledAction);
        mDescription.setText(styledDesc);

        mProgressBar.setVisibility(mIsDownloading ? View.VISIBLE : View.GONE);
        mUpdateSize.setVisibility(mIsUpdate ? View.VISIBLE : View.GONE);
    }

    private final View.OnClickListener mActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (mState) {
                default:
                case STATE_CHECK:
                    mState = STATE_CHECK;
                    mRomUpdater.check(true);
                    updateMessages((PackageInfo) null);
                    break;
                case STATE_FOUND:
                    if (!mRomUpdater.isScanning() && mUpdatePackage != null) {
                        mState = STATE_DOWNLOADING;
                        DownloadHelper.registerCallback(UpdateSystem.this);
                        DownloadHelper.downloadFile(mUpdatePackage.getPath(),
                                mUpdatePackage.getFilename(), mUpdatePackage.getMd5());
                        updateMessages(mUpdatePackage);
                        TrackHelper.track().download().version(mUpdatePackage.getFilename()).with(((PiwikApplication) getApplication()).getTracker());
                    }
                    break;
                case STATE_DOWNLOADING:
                    mState = STATE_CHECK;
                    DownloadHelper.clearDownloads();
                    updateMessages((PackageInfo) null);
                    break;
                case STATE_ERROR:
                    mState = STATE_CHECK;
                    mRomUpdater.check(true);
                    break;
                case STATE_INSTALL:
                    String[] items = new String[mFiles.size()];
                    for (int i = 0; i < mFiles.size(); i++) {
                        File file = mFiles.get(i);
                        items[i] = file.getAbsolutePath();
                    }
                    mRebootHelper.showRebootDialog(UpdateSystem.this, items);
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
            mProgressBar.setProgress(progress);
        }
    }

    @Override
    public void onDownloadFinished(Uri uri, final String md5) {
        if (uri != null) {
            mState = STATE_INSTALL;
            updateMessages((PackageInfo) null);
            addFile(uri, md5);
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
            new Thread() {
                public void run() {
                    final String calculatedMd5 = FileUtils.md5(file);
                    runOnUiThread(new Runnable() {

                        public void run() {
                            if (md5.equals(calculatedMd5)) {
                                reallyAddFile(file);
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

    @Override
    public void checkError(String cause) {
    }

    protected GlifLayout getLayout() {
        return (GlifLayout) findViewById(R.id.setup_wizard_layout);
    }

    protected void setHeaderText(int resId, boolean force) {
        TextView layoutTitle = getLayout().getHeaderTextView();
        CharSequence previousTitle = layoutTitle.getText();
        CharSequence title = getText(resId);
        if (previousTitle != title || force) {
            if (!TextUtils.isEmpty(previousTitle)) {
                layoutTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            }
            getLayout().setHeaderText(title);
            setTitle(title);
        }
    }

    protected void setHeaderText(int resId) {
        setHeaderText(resId, true /* force */);
    }
}
