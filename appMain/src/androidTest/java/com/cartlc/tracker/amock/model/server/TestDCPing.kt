/**
 * Copyright 2018, FleetTLC. All rights reserved
 */
package com.cartlc.tracker.amock.model.server

import android.content.Context
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cartlc.tracker.model.CarRepository
import com.cartlc.tracker.model.pref.PrefHelper
import com.cartlc.tracker.model.server.DCPing
import com.cartlc.tracker.model.server.ServerHelper
import com.cartlc.tracker.model.table.*
import com.cartlc.tracker.ui.app.TBApplication
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.spy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mockito
import org.mockito.Mockito.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection

@RunWith(AndroidJUnit4::class)
class TestDCPing {

    lateinit var context: Context

    lateinit var ping: DCPing
    lateinit var prefHelper: PrefHelper

    @Mock
    lateinit var mockHttpConnection: HttpURLConnection

    @Mock
    lateinit var db: DatabaseTable

    @Mock
    lateinit var tableEntry: TableEntry

    @Mock
    lateinit var tableCrash: TableCrash

    @Mock
    lateinit var tableVehicle: TableVehicle

    @Mock
    lateinit var tableString: TableString

    @Mock
    lateinit var tablePictureCollection: TablePictureCollection

    lateinit var repo: CarRepository

    @Before
    fun onBefore() {
        TBApplication.DEBUG_TREE = true
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().targetContext
        prefHelper = PrefHelper(context, db)
        repo = spy(CarRepository(context, db, prefHelper))
        doReturn(true).`when`(repo).isDevelopment
        ping = DCPing(context, repo)
        ping.openConnection = { target -> mockHttpConnection }
        ServerHelper("0xDEVICE")
    }

    fun initStream(response: String) {
        val inputStream = ByteArrayInputStream(response.toByteArray())
        val outputStream = ByteArrayOutputStream()
        Mockito.`when`(mockHttpConnection.inputStream).thenReturn(inputStream)
        Mockito.`when`(mockHttpConnection.outputStream).thenReturn(outputStream)
    }

    @Test
    fun verifyRegistration() {
        prefHelper.registrationHasChanged = true
        initStream("23:72")
        ping.sendRegistration()
        assertFalse(prefHelper.registrationHasChanged)
        assertEquals(23, prefHelper.techID)
        assertEquals(72, prefHelper.secondaryTechID)
    }

    private fun initBasic() {
        Mockito.`when`(db.tableEntry).thenReturn(tableEntry)
        Mockito.`when`(tableEntry.queryPendingDataToUploadToMaster()).thenReturn(emptyList())
        Mockito.`when`(db.tablePictureCollection).thenReturn(tablePictureCollection)
        Mockito.`when`(db.tableCrash).thenReturn(tableCrash)
        Mockito.`when`(tableCrash.queryNeedsUploading()).thenReturn(emptyList())
        Mockito.`when`(db.tableVehicle).thenReturn(tableVehicle)
        Mockito.`when`(tableVehicle.queryNotUploaded()).thenReturn(emptyList())
        Mockito.`when`(db.tableString).thenReturn(tableString)
        Mockito.`when`(tableString.queryNotUploaded()).thenReturn(emptyList())
    }

    @Test
    fun verifyPingRetrigger() {
        val response = "{\"reset_upload\":true,\"version_project\":75,\"version_company\":789,\"version_equipment\":448,\"version_note\":294,\"version_truck\":335,\"version_vehicle_names\":2}"
        initStream(response)
        initBasic()
        doNothing().`when`(repo).clearUploaded()
        ping.ping()
        Mockito.verify(repo).clearUploaded()
    }

    @Test
    fun verifyPingReregister() {
        val response = "{\"re-register\":true,\"version_project\":75,\"version_company\":789,\"version_equipment\":448,\"version_note\":294,\"version_truck\":335,\"version_vehicle_names\":2}"
        initStream(response)
        initBasic()
        ping.ping()
        Mockito.verify(repo).clearUploaded()
    }
}