/**
 * Copyright 2018, FleetTLC. All rights reserved
 */
package com.cartlc.tracker.fresh.model.core.data

import com.cartlc.tracker.fresh.model.core.table.DatabaseTable

/**
 * Created by dug on 5/10/17.
 */

class DataCollectionEquipmentEntry(
        private val db: DatabaseTable,
        collectionId: Long
) : DataCollectionEquipment(db, collectionId) {

    fun addChecked() {
        equipmentListIds = db.tableEquipment.queryIdsChecked().toMutableList()
    }

    fun setChecked() {
        db.tableEquipment.setChecked(equipmentListIds)
    }

    val hasChecked: Boolean
        get() = db.tableEquipment.queryIdsChecked().isNotEmpty()
}
