/*
 * *
 *   * Copyright 2019, FleetTLC. All rights reserved
 *
 */
package com.cartlc.tracker.fresh.ui.app

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.VisibleForTesting
import androidx.core.content.FileProvider
import androidx.multidex.MultiDex
import com.cartlc.tracker.BuildConfig
import com.cartlc.tracker.R
import com.cartlc.tracker.fresh.model.CarRepository
import com.cartlc.tracker.fresh.model.VehicleRepository
import com.cartlc.tracker.fresh.model.core.sql.DatabaseManager
import com.cartlc.tracker.fresh.model.core.table.DatabaseTable
import com.cartlc.tracker.fresh.model.event.EventError
import com.cartlc.tracker.fresh.model.flow.FlowUseCase
import com.cartlc.tracker.fresh.model.flow.FlowUseCaseImpl
import com.cartlc.tracker.fresh.model.pref.PrefHelper
import com.cartlc.tracker.fresh.service.endpoint.DCPing
import com.cartlc.tracker.fresh.service.endpoint.DCServerRx
import com.cartlc.tracker.fresh.service.endpoint.DCServerRxImpl
import com.cartlc.tracker.fresh.service.endpoint.DCService
import com.cartlc.tracker.fresh.service.help.AmazonHelper
import com.cartlc.tracker.fresh.service.help.ServerHelper
import com.cartlc.tracker.fresh.ui.app.dependencyinjection.ComponentRoot
import com.cartlc.tracker.fresh.ui.common.PermissionHelper.PermissionListener
import com.cartlc.tracker.fresh.ui.common.PermissionHelper.PermissionRequest
import com.cartlc.tracker.ui.util.CheckError
import com.cartlc.tracker.ui.util.helper.LocationHelper
import com.cartlc.tracker.viewmodel.vehicle.VehicleViewModel
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File

class TBApplication : Application() {

    companion object {

        private val OVERRIDE_IS_DEVELOPMENT_SERVER: Boolean? = true

        @VisibleForTesting
        var DEBUG_TREE = BuildConfig.DEBUG

        const val REPORT_LOCATION = false

        val PERMISSIONS = arrayOf(
                PermissionRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        R.string.perm_read_external_storage),
                PermissionRequest(Manifest.permission.READ_EXTERNAL_STORAGE,
                        R.string.perm_write_external_storage),
                PermissionRequest(Manifest.permission.ACCESS_FINE_LOCATION,
                        R.string.perm_location))

        fun IsDevelopmentServer(): Boolean {
            return OVERRIDE_IS_DEVELOPMENT_SERVER
                    ?: BuildConfig.DEBUG
        }

        fun getUri(ctx: Context, file: File): Uri {
            return FileProvider.getUriForFile(ctx, "com.cartcl.tracker.fileprovider", file)
        }

        fun hideKeyboard(ctx: Context, v: View) {
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }

        fun ReportError(ex: Exception, claz: Class<*>, function: String, type: String): String {
            return ReportError(ex.message
                    ?: "unknown", claz, function, type)
        }

        fun ReportError(msg: String, claz: Class<*>, function: String, type: String): String {
            val sbuf = StringBuilder()
            sbuf.append("Class:")
            sbuf.append(claz.simpleName)
            sbuf.append(".")
            sbuf.append(function)
            sbuf.append(" ")
            sbuf.append(type)
            sbuf.append(": ")
            sbuf.append(msg)
            Timber.e(sbuf.toString())
            return sbuf.toString()
        }

        fun ReportServerError(ex: Exception, claz: Class<*>, function: String, type: String): String {
            val msg = ReportError(ex, claz, function, type)
            ShowError(msg)
            return msg
        }

        fun ShowError(msg: String) {
            EventBus.getDefault().post(EventError(msg))
        }
    }

    private lateinit var carRepo: CarRepository
    private lateinit var prefHelper: PrefHelper
    private lateinit var dm: DatabaseManager
    private lateinit var dcRx: DCServerRx
    private lateinit var vehicleRepository: VehicleRepository

    lateinit var componentRoot: ComponentRoot
    lateinit var amazonHelper: AmazonHelper
    lateinit var flowUseCase: FlowUseCase
    lateinit var ping: DCPing
    lateinit var vehicleViewModel: VehicleViewModel

    val repo: CarRepository
        get() = carRepo

    val db: DatabaseTable
        get() = dm

    val version: String
        get() = componentRoot.deviceHelper.version

    override fun onCreate() {
        super.onCreate()

        dm = DatabaseManager(this)
        prefHelper = PrefHelper(this, dm)
        flowUseCase = FlowUseCaseImpl()
        carRepo = CarRepository(
                dm,
                prefHelper,
                flowUseCase
        )
        ping = DCPing(this, repo)
        dcRx = DCServerRxImpl(ping)
        componentRoot = ComponentRoot(this,
                prefHelper,
                flowUseCase,
                carRepo,
                ping,
                dcRx)
        carRepo.computeCurStage()
        carRepo.db.tablePicture.removeFileDoesNotExist()

        vehicleRepository = VehicleRepository(this, dm)
        vehicleViewModel = VehicleViewModel(vehicleRepository)

        if (IsDevelopmentServer() && DEBUG_TREE) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree(db))
        }
        ServerHelper.Init(this)
        amazonHelper = AmazonHelper(db, prefHelper, componentRoot.eventController)
        CheckError.Init()
        LocationHelper.Init(this, db)

        if (prefHelper.detectOneTimeReloadFromServerCheck()) {
            reloadFromServer()
        }
    }

    fun reloadFromServer() {
        prefHelper.reloadFromServer()
        ping()
    }

    fun ping() {
        if (ServerHelper.instance.hasConnection(this)) {
            startService(Intent(this, DCService::class.java))
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    fun checkPermissions(act: Activity, listener: PermissionListener) {
        componentRoot.permissionHelper.checkPermissions(act, PERMISSIONS, listener)
    }

//    fun requestZipCode(tableZipCode: String) {
//        val data = db.tableZipCode.query(tableZipCode)
//        if (data != null) {
//            data.check()
//            EventBus.getDefault().post(data)
//        } else if (ServerHelper.instance.hasConnection(this)) {
//            val intent = Intent(this, DCService::class.java)
//            intent.action = DCService.ACTION_ZIP_CODE
//            intent.putExtra(DCService.DATA_ZIP_CODE, tableZipCode)
//            startService(intent)
//        }
//    }

}