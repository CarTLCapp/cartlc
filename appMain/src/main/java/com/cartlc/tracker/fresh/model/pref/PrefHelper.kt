/**
 * Copyright 2018, FleetTLC. All rights reserved
 */
package com.cartlc.tracker.fresh.model.pref

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import com.cartlc.tracker.fresh.model.core.data.*
import com.cartlc.tracker.fresh.model.misc.TruckStatus
import com.cartlc.tracker.fresh.model.core.table.DatabaseTable
import com.cartlc.tracker.fresh.model.flow.Stage
import com.cartlc.tracker.fresh.ui.app.TBApplication

import java.io.File
import java.text.SimpleDateFormat

import timber.log.Timber
import java.util.*

/**
 * Created by dug on 4/17/17.
 */

class PrefHelper constructor(
        ctx: Context,
        private val db: DatabaseTable
) : PrefHelperBase(ctx) {

    companion object {

        const val KEY_ROOT_PROJECT = "root_project"
        const val KEY_SUB_PROJECT = "sub_project"
        const val KEY_COMPANY = "company"
        const val KEY_STREET = "street"
        const val KEY_STATE = "state"
        const val KEY_CITY = "city"
        const val KEY_ZIPCODE = "zipcode"
        const val KEY_TRUCK = "truck" // Number & License
        const val KEY_STATUS = "status"

        private const val KEY_CURRENT_PROJECT_GROUP_ID = "current_project_group_id"
        private const val KEY_SAVED_PROJECT_GROUP_ID = "saved_project_group_id"
        private const val KEY_FIRST_TECH_CODE = "first_tech_code"
        private const val KEY_SECONDARY_TECH_CODE = "secondary_tech_code"
        private const val KEY_TRUCK_DAMAGE_EXISTS = "truck_damage_exists"
        private const val KEY_EDIT_ENTRY_ID = "edit_id"
        private const val KEY_NEXT_PICTURE_COLLECTION_ID = "next_picture_collection_id"
        private const val KEY_NEXT_EQUIPMENT_COLLECTION_ID = "next_equipment_collection_id"
        private const val KEY_NEXT_NOTE_COLLECTION_ID = "next_note_collection_id"
        private const val KEY_CURRENT_PICTURE_COLLECTION_ID = "picture_collection_id"
        private const val KEY_TECH_ID = "tech_id"
        private const val KEY_TECH_FIRST_NAME = "tech_first_name"
        private const val KEY_TECH_LAST_NAME = "tech_last_name"
        private const val KEY_SECONDARY_TECH_ID = "secondary_tech_id"
        private const val KEY_SECONDARY_TECH_FIRST_NAME = "secondary_tech_first_name"
        private const val KEY_SECONDARY_TECH_LAST_NAME = "secondary_tech_last_name"
        private const val KEY_IS_DEVELOPMENT = "is_development"
        private const val KEY_RELOAD_FROM_SERVER = "reload_from_server"
        private const val KEY_DO_ERROR_CHECK = "do_error_check"
        private const val KEY_AUTO_ROTATE_PICTURE = "auto_rotate_picture"

        const val VERSION_PROJECT = "version_project"
        const val VERSION_COMPANY = "version_company"
        const val VERSION_EQUIPMENT = "version_equipment"
        const val VERSION_NOTE = "version_note"
        const val VERSION_TRUCK = "version_truck"
        const val VERSION_FLOW = "version_flow"
        const val VERSION_VEHICLE_NAMES = "version_vehicle_names"

        private const val PICTURE_DATE_FORMAT = "yy-MM-dd_HH:mm:ss"
        private const val VERSION_RESET = -1

        private const val CONFIRM_PROMPT_PREFIX = "PROMPT__"
    }

    val isLocalCompany: Boolean
        get() = db.tableAddress.isLocalCompanyOnly(company)

    var techID: Int
        get() = getInt(KEY_TECH_ID, 0)
        set(id) = setInt(KEY_TECH_ID, id)

    var techFirstName: String?
        get() = getString(KEY_TECH_FIRST_NAME, null)
        set(value) { setString(KEY_TECH_FIRST_NAME, value) }

    var techLastName: String?
        get() = getString(KEY_TECH_LAST_NAME, null)
        set(value) { setString(KEY_TECH_LAST_NAME, value) }

    val techName: String
        get() {
            return if (techFirstName != null && techLastName != null) {
                "$techFirstName $techLastName"
            } else ""
        }

    var secondaryTechID: Int
        get() = getInt(KEY_SECONDARY_TECH_ID, 0)
        set(id) = setInt(KEY_SECONDARY_TECH_ID, id)

    var secondaryTechFirstName: String?
        get() = getString(KEY_SECONDARY_TECH_FIRST_NAME, null)
        set(value) { setString(KEY_SECONDARY_TECH_FIRST_NAME, value) }

    var secondaryTechLastName: String?
        get() = getString(KEY_SECONDARY_TECH_LAST_NAME, null)
        set(value) { setString(KEY_SECONDARY_TECH_LAST_NAME, value) }

    val secondaryTechName: String
        get() {
            return if (secondaryTechFirstName != null && secondaryTechLastName != null) {
                "$secondaryTechFirstName $secondaryTechLastName"
            } else ""
        }

    var street: String?
        get() = getString(KEY_STREET, null)
        set(value) = setString(KEY_STREET, value)

    var state: String?
        get() = getString(KEY_STATE, null)
        set(value) = setString(KEY_STATE, value)

    var company: String?
        get() = getString(KEY_COMPANY, null)
        set(value) = setString(KEY_COMPANY, value)

    var city: String?
        get() = getString(KEY_CITY, null)
        set(value) = setString(KEY_CITY, value)

    var zipCode: String?
        get() = getString(KEY_ZIPCODE, null)
        set(value) = setString(KEY_ZIPCODE, value)

    val projectDashName: String
        get() {
            val rootName = projectRootName ?: ""
            val subName = projectSubName ?: return rootName
            if (subName.isEmpty()) {
                return rootName
            }
            return "$rootName - $subName"
        }

    var projectRootName: String?
        get() = getString(KEY_ROOT_PROJECT, null)
        set(value) = setString(KEY_ROOT_PROJECT, value)

    var projectSubName: String?
        get() = getString(KEY_SUB_PROJECT, null)
        set(value) = setString(KEY_SUB_PROJECT, value)

    val projectId: Long?
        get() {
            val rootName = projectRootName
            val subName = projectSubName
            return if (rootName != null && subName != null) {
                val id = db.tableProjects.queryProjectId(rootName, subName)
                if (id >= 0) id else null
            } else null
        }

    var currentProjectGroupId: Long
        get() = getLong(KEY_CURRENT_PROJECT_GROUP_ID, -1L)
        set(id) = setLong(KEY_CURRENT_PROJECT_GROUP_ID, id)

    private var savedProjectGroupId: Long
        get() = getLong(KEY_SAVED_PROJECT_GROUP_ID, -1L)
        set(id) = setLong(KEY_SAVED_PROJECT_GROUP_ID, id)

    var currentPictureCollectionId: Long
        get() = getLong(KEY_CURRENT_PICTURE_COLLECTION_ID, 0)
        set(id) = setLong(KEY_CURRENT_PICTURE_COLLECTION_ID, id)

    var firstTechCode: String?
        get() = getString(KEY_FIRST_TECH_CODE, null)
        set(name) = setString(KEY_FIRST_TECH_CODE, name)

    var secondaryTechCode: String?
        get() = getString(KEY_SECONDARY_TECH_CODE, null)
        set(name) = setString(KEY_SECONDARY_TECH_CODE, name)

    var versionProject: Int
        get() = getInt(VERSION_PROJECT, VERSION_RESET)
        set(value) = setInt(VERSION_PROJECT, value)

    var versionEquipment: Int
        get() = getInt(VERSION_EQUIPMENT, VERSION_RESET)
        set(value) = setInt(VERSION_EQUIPMENT, value)

    var versionNote: Int
        get() = getInt(VERSION_NOTE, VERSION_RESET)
        set(value) = setInt(VERSION_NOTE, value)

    var versionCompany: Int
        get() = getInt(VERSION_COMPANY, VERSION_RESET)
        set(value) = setInt(VERSION_COMPANY, value)

    var versionTruck: Int
        get() = getInt(VERSION_TRUCK, VERSION_RESET)
        set(value) = setInt(VERSION_TRUCK, value)

    var versionFlow: Int
        get() = getInt(VERSION_FLOW, VERSION_RESET)
        set(value) = setInt(VERSION_FLOW, value)

    var versionVehicleNames: Int
        get() = getInt(VERSION_VEHICLE_NAMES, VERSION_RESET)
        set(value) = setInt(VERSION_VEHICLE_NAMES, value)

    var status: TruckStatus?
        get() = TruckStatus.from(getInt(KEY_STATUS, TruckStatus.UNKNOWN.ordinal))
        set(status) = if (status == null) {
            setInt(KEY_STATUS, TruckStatus.UNKNOWN.ordinal)
        } else {
            setInt(KEY_STATUS, status.ordinal)
        }

    var currentEditEntryId: Long
        get() = getLong(KEY_EDIT_ENTRY_ID, 0)
        set(id) = setLong(KEY_EDIT_ENTRY_ID, id)

    val currentEditEntry: DataEntry?
        get() = db.tableEntry.query(currentEditEntryId)

    var doErrorCheck: Boolean
        get() = getInt(KEY_DO_ERROR_CHECK, 1) != 0
        set(flag) = setInt(KEY_DO_ERROR_CHECK, if (flag) 1 else 0)

    val isDevelopment: Boolean
        get() = getInt(KEY_IS_DEVELOPMENT, if (TBApplication.IsDevelopmentServer()) 1 else 0) != 0

    var currentProjectGroup: DataProjectAddressCombo?
        get() = db.tableProjectAddressCombo.query(currentProjectGroupId)
        set(group) {
            group?.let {
                currentProjectGroupId = group.id
                val projectName = group.projectName
                projectName?.let {
                    projectRootName = projectName.first
                    projectSubName = projectName.second
                }
                setAddress(group.address)
            }
            onCurrentProjecGroupChanged.invoke(group)
        }

    // TODO: This look likes something that can be improved:
    var onCurrentProjecGroupChanged: (group: DataProjectAddressCombo?) -> Unit = {}

    // Note: ID zero has a special meaning, it means that the set is pending.
    private val nextPictureCollectionID: Long
        get() = getLong(KEY_NEXT_PICTURE_COLLECTION_ID, 1L)

    private val nextEquipmentCollectionID: Long
        get() = getLong(KEY_NEXT_EQUIPMENT_COLLECTION_ID, 0L)

    private val nextNoteCollectionID: Long
        get() = getLong(KEY_NEXT_NOTE_COLLECTION_ID, 1L)

    val address: String
        get() {
            val sbuf = StringBuilder()
            val company = company
            if (company != null) {
                sbuf.append(company)
            }
            val street = street
            if (street != null) {
                if (sbuf.isNotEmpty()) {
                    sbuf.append("\n")
                }
                sbuf.append(street)
            }
            val city = city
            if (city != null) {
                if (sbuf.isNotEmpty()) {
                    sbuf.append("\n")
                }
                sbuf.append(city)
            }
            val state = state
            if (state != null) {
                if (sbuf.isNotEmpty()) {
                    sbuf.append(", ")
                }
                sbuf.append(state)
            }
            val zip = zipCode
            if (zip != null) {
                if (sbuf.isNotEmpty()) {
                    sbuf.append(" ")
                }
                sbuf.append(zip)
            }
            return sbuf.toString()
        }

    var truckNumberValue: String?
        get() = db.tableNote.noteTruckNumber?.value
        set(value) { db.tableNote.noteTruckNumber?.value = value }

    var truckHasDamage: Boolean?
        get() {
            return when (getInt(KEY_TRUCK_DAMAGE_EXISTS, 2)) {
                0 -> false
                1 -> true
                else -> null
            }
        }
        set(value) {
            value?.let {
                setInt(KEY_TRUCK_DAMAGE_EXISTS, if (value) 1 else 0)
            } ?: setInt(KEY_TRUCK_DAMAGE_EXISTS, 2)
        }

    private val truckNumberPictureId: Int
        get() {
            val items = db.tablePicture.removeFileDoesNotExist(
                    db.tablePicture.query(currentPictureCollectionId, Stage.TRUCK_NUMBER_PICTURE)
            )
            if (items.isNotEmpty()) {
                return items[0].id.toInt()
            }
            return 0
        }

    private val truckDamagePictureId: Int
        get() {
            val items = db.tablePicture.removeFileDoesNotExist(
                    db.tablePicture.query(currentPictureCollectionId, Stage.TRUCK_DAMAGE_PICTURE)
            )
            if (items.isNotEmpty()) {
                return items[0].id.toInt()
            }
            return 0
        }

    val numPicturesTaken: Int
        get() = db.tablePicture.countPictures(currentPictureCollectionId, null)

    val numEquipPossible: Int
        get() {
            val collection = db.tableCollectionEquipmentProject.queryForProject(currentProjectGroup!!.projectNameId)
            return collection.equipment.size
        }

    val autoRotatePicture: Int
        get() = getInt(KEY_AUTO_ROTATE_PICTURE, 0)

    val hasCode: Boolean
        get() = !TextUtils.isEmpty(firstTechCode)

    val hasSecondary: Boolean
        get() = !TextUtils.isEmpty(secondaryTechCode)

    fun getKeyValue(key: String): String? {
        return getString(key, null)
    }

    fun setKeyValue(key: String, value: String?) {
        setString(key, value)
    }

    // region CONFIRM

    fun getConfirmValue(prompt: String): Boolean {
        return getInt(CONFIRM_PROMPT_PREFIX + prompt, 0) != 0
    }

    fun setConfirmValue(prompt: String, value: Boolean) {
        setInt(CONFIRM_PROMPT_PREFIX + prompt, if (value) 1 else 0)
    }

    fun clearConfirmValues() {
        val editor = prefs.edit()
        val map = prefs.all
        for (key in map.keys) {
            if (key.startsWith(CONFIRM_PROMPT_PREFIX)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    // endregion CONFIRM

    fun reloadFromServer() {
        versionEquipment = VERSION_RESET
        versionProject = VERSION_RESET
        versionNote = VERSION_RESET
        versionCompany = VERSION_RESET
        versionTruck = VERSION_RESET
    }

    fun detectOneTimeReloadFromServerCheck(): Boolean {
        val value = getInt(KEY_RELOAD_FROM_SERVER, 0)
        if (value < 2) {
            setInt(KEY_RELOAD_FROM_SERVER, 2)
            return true
        }
        return false
    }

    fun setFromCurrentProjectId(): Boolean {
        val projectGroup = currentProjectGroup
        if (projectGroup != null) {
            val name = projectGroup.projectName
            name?.let {
                projectRootName = it.first
                projectSubName = it.second
            }
            val address = projectGroup.address
            if (address != null) {
                setAddress(address)
                return true
            }
        }
        return false
    }

    fun clearCurProject() {
        clearLastEntry()
        state = null
        city = null
        company = null
        street = null
        zipCode = null
        projectRootName = null
        projectSubName = null
        savedProjectGroupId = currentProjectGroupId
        currentProjectGroupId = -1L
        db.tableEquipment.clearChecked()
    }

    fun clearCurProjectIfMatching(rootName: String?, subName: String?) {
        if (projectRootName == rootName && projectSubName == subName) {
            clearCurProject()
        }
    }

    fun clearLastEntry() {
        truckNumberValue = null
        truckHasDamage = null
        setKeyValue(KEY_TRUCK, null)
        status = null
        currentEditEntryId = 0
        currentPictureCollectionId = 0
        db.tablePicture.clearPendingPictures()
        db.tableNote.clearValues()
        db.tableEquipment.clearChecked()
        clearConfirmValues()
    }

    fun saveProjectAndAddressCombo(modifyCurrent: Boolean, needsValidServerId: Boolean = false): Boolean {
        val rootName = projectRootName ?: return false
        val subName = projectSubName ?: ""
        val company = company
        val state = state
        val street = street
        val city = city
        if (company.isNullOrBlank() || state.isNullOrBlank()|| street.isNullOrBlank() || city.isNullOrBlank()) {
            setFromCurrentProjectId()
            if (company.isNullOrBlank() || state.isNullOrBlank() || street.isNullOrBlank() || city.isNullOrBlank()) {
                return false  // Okay to have nothing selected
            }
        }
        val zipCode = zipCode
        var addressId: Long
        addressId = db.tableAddress.queryAddressId(company, street, city, state, zipCode)
        if (addressId < 0) {
            val address = DataAddress(company, street, city, state, zipCode)
            address.isLocal = true
            addressId = db.tableAddress.add(address)
            if (addressId < 0) {
                Timber.e("saveProjectAndAddressCombo(): could not find address: $address")
                clearCurProject()
                return false
            }
        }
        val project = db.tableProjects.queryByName(rootName, subName)
        val projectNameId: Long
        if (project == null) {
            if (subName.isEmpty()) {
                projectNameId = db.tableProjects.add(rootName)
            } else {
                Timber.e("saveProjectAndAddressCombo(): could not find project: $rootName - $subName")
                clearCurProject()
                return false
            }
        } else {
            if (project.disabled) {
                project.disabled = false
                db.tableProjects.update(project)
            }
            projectNameId = project.id
        }
        if (needsValidServerId) {
            if (!db.tableProjects.hasServerId(rootName, subName)) {
                Timber.e("saveProjectAndAddressCombo(): current project does not associated with the server: $rootName - $subName")
                clearCurProject()
                return false
            }
        }
        var projectGroupId: Long
        if (modifyCurrent) {
            projectGroupId = currentProjectGroupId
            if (projectGroupId < 0) {
                Timber.e("saveProjectAndAddressCombo(): could not modify current project, none is alive")
                return false
            }
            val combo = db.tableProjectAddressCombo.query(projectGroupId) ?: return false
            combo.reset(projectNameId, addressId)
            if (!db.tableProjectAddressCombo.save(combo)) {
                Timber.e("saveProjectAndAddressCombo(): could not update project combo")
                return false
            }
            db.tableProjectAddressCombo.mergeIdenticals(combo)
            val count = db.tableEntry.reUploadEntries(combo)
            if (count > 0) {
                Timber.i("saveProjectAddressCombo(): re-upload $count entries")
            }
            db.tableProjectAddressCombo.updateUsed(projectGroupId)
        } else {
            projectGroupId = db.tableProjectAddressCombo.queryProjectGroupId(projectNameId, addressId)
            if (projectGroupId < 0) {
                projectGroupId = db.tableProjectAddressCombo.add(DataProjectAddressCombo(db, projectNameId, addressId))
            } else {
                db.tableProjectAddressCombo.updateUsed(projectGroupId)
            }
            currentProjectGroupId = projectGroupId
        }
        return true
    }

    private fun setAddress(address: DataAddress?) {
        company = address!!.company
        street = address.street
        city = address.city
        state = address.state
        zipCode = address.zipcode
    }

    fun incNextPictureCollectionID() {
        setLong(KEY_NEXT_PICTURE_COLLECTION_ID, nextPictureCollectionID + 1)
    }

    fun incNextEquipmentCollectionID() {
        setLong(KEY_NEXT_EQUIPMENT_COLLECTION_ID, nextEquipmentCollectionID + 1)
    }

    fun incNextNoteCollectionID() {
        setLong(KEY_NEXT_NOTE_COLLECTION_ID, nextEquipmentCollectionID + 1)
    }

    private fun createEntry(): DataEntry? {
        val projectGroupId = currentProjectGroupId
        if (projectGroupId < 0) {
            return null
        }
        val projectGroup = db.tableProjectAddressCombo.query(projectGroupId) ?: return null
        val entry = DataEntry(db)
        entry.projectAddressCombo = projectGroup
        entry.equipmentCollection = DataCollectionEquipmentEntry(db, nextEquipmentCollectionID)
        entry.equipmentCollection!!.addChecked()
        entry.pictures = db.tablePicture.createCollectionFromPending(nextPictureCollectionID)
        entry.truckId = db.tableTruck.save(
                truckNumberValue ?: "",
                truckNumberPictureId,
                truckHasDamage ?: false,
                truckDamagePictureId,
                projectGroup.projectNameId,
                projectGroup.companyName!!)
        entry.status = status
        entry.saveNotes(nextNoteCollectionID)
        entry.date = System.currentTimeMillis()
        return entry
    }

    fun setFromEntry(entry: DataEntry) {
        currentEditEntryId = entry.id
        currentProjectGroupId = entry.projectAddressCombo!!.id
        currentPictureCollectionId = entry.pictureCollectionId
        entry.equipmentCollection!!.setChecked()
        val truck = entry.truck
        if (truck != null) {
            truckNumberValue = truck.truckNumberValue
            // TODO: Don't think I need this anymore as it is already there to be referenced fine.
//            truckNumberPictureId = truck.truckNumberPictureId
//            truckDamagePictureId = truck.truckDamagePictureId
            truckHasDamage = truck.truckHasDamage
        } else {
            truckNumberValue = null
        }
        status = entry.status
        db.tableNote.clearValues()
    }

    fun saveEntry(): DataEntry? {
        val entry = currentEditEntry ?: return createEntry()
        entry.equipmentCollection!!.addChecked()
        var truck = entry.truck
        if (truck == null) {
            truck = DataTruck()
        }
        truck.truckNumberValue = truckNumberValue
        truck.truckNumberPictureId = truckNumberPictureId
        truck.truckDamagePictureId = truckDamagePictureId
        truck.truckHasDamage = truckHasDamage ?: false
        truck.hasEntry = true
        entry.truckId = db.tableTruck.save(truck)
        entry.status = status
        entry.uploadedMaster = false
        entry.uploadedAws = false
        entry.hasError = false
        entry.serverErrorCount = 0.toShort()
        // Be careful here: I use the date to match an tableEntry when looking up the server id for older APP versions.
        if (entry.serverId > 0) {
            entry.date = System.currentTimeMillis()
        }
        entry.saveNotes()
        return entry
    }

    private fun genPictureFilename(): String {
        val techId = techID.toLong()
        val projId = projectId
        val sbuf = StringBuilder()
        sbuf.append("picture_t")
        sbuf.append(techId)
        sbuf.append("_p")
        sbuf.append(projId ?: "bad")
        sbuf.append("_d")
        val fmt = SimpleDateFormat(PICTURE_DATE_FORMAT, Locale.getDefault())
        sbuf.append(fmt.format(Date(System.currentTimeMillis())))
        sbuf.append(".jpg")
        return sbuf.toString()
    }

    fun genFullPictureFile(): File {
        return File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), genPictureFilename())
    }

    fun clearUploaded() {
        versionNote = VERSION_RESET
        versionProject = VERSION_RESET
        versionEquipment = VERSION_RESET
        versionCompany = VERSION_RESET
        versionTruck = VERSION_RESET
    }

    fun reloadProjects() {
        versionProject = VERSION_RESET
    }

    fun reloadEquipments() {
        versionEquipment = VERSION_RESET
    }

    fun incAutoRotatePicture(degrees: Int) {
        val newValue = (autoRotatePicture + degrees) % 360
        setInt(KEY_AUTO_ROTATE_PICTURE, newValue)
    }

    fun clearAutoRotatePicture() {
        setInt(KEY_AUTO_ROTATE_PICTURE, 0)
    }

}
