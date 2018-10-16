package com.cartlc.tracker.model

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.cartlc.tracker.model.data.DataEntry
import com.cartlc.tracker.model.flow.Flow
import com.cartlc.tracker.model.flow.LoginFlow
import com.cartlc.tracker.model.flow.Stage
import com.cartlc.tracker.model.pref.PrefHelper
import com.cartlc.tracker.model.sql.DatabaseManager
import com.cartlc.tracker.model.table.DatabaseTable

class CarRepository(
        val context: Context
) {
    private val dm: DatabaseManager = DatabaseManager(context)
    val db: DatabaseTable
        get() = dm
    val prefHelper: PrefHelper = PrefHelper(context, db)
    val curFlow: MutableLiveData<Flow> by lazy {
        MutableLiveData<Flow>()
    }
    init {
        curFlow.value = LoginFlow()
    }

    fun checkProjectErrors(): Boolean {
        val entries = db.projectAddressCombo.query()
        for (combo in entries) {
            if (!combo.hasValidState) {
                val address = combo.fix()
                if (address != null) {
                    db.address.update(address)
                }
            }
        }
        return false
    }

    fun checkEntryErrors(): DataEntry? {
        val entries = db.entry.query()
        for (entry in entries) {
            if (entry.hasError) {
                return entry
            }
        }
        return null
    }

    fun clearUploaded() {
        dm.clearUploaded()
        prefHelper.clearUploaded()
    }

    fun add(entry: DataEntry) {
        if (db.entry.add(entry)) {
            prefHelper.incNextEquipmentCollectionID()
            prefHelper.incNextPictureCollectionID()
            prefHelper.incNextNoteCollectionID()
        }
    }
}