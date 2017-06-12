package com.cartlc.tracker.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.multidex.MultiDex;
import android.support.v4.content.FileProvider;

import com.cartlc.tracker.BuildConfig;
import com.cartlc.tracker.data.DatabaseManager;
import com.cartlc.tracker.data.PrefHelper;
import com.cartlc.tracker.data.BootstrapData;
import com.cartlc.tracker.server.AmazonHelper;
import com.cartlc.tracker.server.DCService;
import com.cartlc.tracker.server.ServerHelper;

import timber.log.Timber;

import com.cartlc.tracker.util.PermissionHelper;

import java.io.File;

/**
 * Created by dug on 4/14/17.
 */

public class TBApplication extends Application {

    public static final Boolean DEVELOPMENT_SERVER = false;

    public static final String OTHER = "Other";

//    static final PermissionRequest[] PERMISSIONS = {
//            new PermissionRequest(android.Manifest.permission.READ_PHONE_STATE, R.string.perm_read_phone_state)};

    public TBApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        DatabaseManager.Init(this);
        PrefHelper.Init(this);
        ServerHelper.Init(this);
        PermissionHelper.Init();
        AmazonHelper.Init(this);

        BootstrapData.Init();
    }

    public void ping() {
        if (ServerHelper.getInstance().hasConnection()) {
            startService(new Intent(this, DCService.class));
        }
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static Uri getUri(Context ctx, File file) {
        return FileProvider.getUriForFile(ctx, "com.cartcl.tracker.fileprovider", file);
    }

//    public void checkPermissions(Activity act, PermissionListener listener) {
//        PermissionHelper.getInstance().checkPermissions(act, PERMISSIONS, listener);
//    }

}
