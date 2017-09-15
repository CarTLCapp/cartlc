package com.cartlc.tracker.etc;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.cartlc.tracker.app.TBApplication;
import com.cartlc.tracker.data.DataAddress;
import com.cartlc.tracker.data.DataCollectionEquipmentEntry;
import com.cartlc.tracker.data.DataCollectionEquipmentProject;
import com.cartlc.tracker.data.DataEntry;
import com.cartlc.tracker.data.DataNote;
import com.cartlc.tracker.data.DataProjectAddressCombo;
import com.cartlc.tracker.data.DataTruck;
import com.cartlc.tracker.data.TableAddress;
import com.cartlc.tracker.data.TableCollectionEquipmentProject;
import com.cartlc.tracker.data.TableEntry;
import com.cartlc.tracker.data.TableEquipment;
import com.cartlc.tracker.data.TableNote;
import com.cartlc.tracker.data.TablePictureCollection;
import com.cartlc.tracker.data.TableProjectAddressCombo;
import com.cartlc.tracker.data.TableProjects;
import com.cartlc.tracker.data.TableTruck;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

/**
 * Created by dug on 4/17/17.
 */

public class PrefHelper extends PrefHelperBase {

    static PrefHelper sInstance;

    public static PrefHelper getInstance() {
        return sInstance;
    }

    public static void Init(Context ctx) {
        new PrefHelper(ctx);
    }

    static public final String KEY_PROJECT                       = "project";
    static public final String KEY_COMPANY                       = "company";
    static public final String KEY_STREET                        = "street";
    static public final String KEY_STATE                         = "state";
    static public final String KEY_CITY                          = "city";
    static public final String KEY_ZIPCODE                       = "zipcode";
    static public final String KEY_TRUCK                         = "truck";
    static public final String KEY_STATUS                        = "status";
    static final        String KEY_CURRENT_PROJECT_GROUP_ID      = "current_project_group_id";
    static final        String KEY_SAVED_PROJECT_GROUP_ID        = "saved_project_group_id";
    static final        String KEY_FIRST_NAME                    = "first_name";
    static final        String KEY_LAST_NAME                     = "last_name";
    static final        String KEY_TRUCK_NUMBER                  = "truck_number";
    static final        String KEY_LICENSE_PLATE                 = "license_plate";
    static final        String KEY_EDIT_ENTRY_ID                 = "edit_id";
    static final        String KEY_NEXT_PICTURE_COLLECTION_ID    = "next_picture_collection_id";
    static final        String KEY_NEXT_EQUIPMENT_COLLECTION_ID  = "next_equipment_collection_id";
    static final        String KEY_NEXT_NOTE_COLLECTION_ID       = "next_note_collection_id";
    static final        String KEY_CURRENT_PICTURE_COLLECTION_ID = "picture_collection_id";
    static final        String KEY_TECH_ID                       = "tech_id";
    static final        String KEY_REGISTRATION_CHANGED          = "registration_changed";
    static final        String KEY_IS_DEVELOPMENT                = "is_development";
    static final        String KEY_SPECIAL_UPDATE_CHECK          = "special_update_check";

    public static final String VERSION_PROJECT   = "version_project";
    public static final String VERSION_COMPANY   = "version_company";
    public static final String VERSION_EQUIPMENT = "version_equipment";
    public static final String VERSION_NOTE      = "version_note";
    public static final String VERSION_TRUCK     = "version_truck";

    static final String PICTURE_DATE_FORMAT = "yy-MM-dd_HH:mm:ss";
    static final int    VERSION_RESET       = -1;

    PrefHelper(Context ctx) {
        super(ctx);
        sInstance = this;
    }

    public int getTechID() {
        return getInt(KEY_TECH_ID, 0);
    }

    public void setTechID(int id) {
        setInt(KEY_TECH_ID, id);
    }

    public String getStreet() {
        return getString(KEY_STREET, null);
    }

    public void setStreet(String value) {
        setString(KEY_STREET, value);
    }

    public String getState() {
        return getString(KEY_STATE, null);
    }

    public void setState(String value) {
        setString(KEY_STATE, value);
    }

    public String getCompany() {
        return getString(KEY_COMPANY, null);
    }

    public void setCompany(String value) {
        setString(KEY_COMPANY, value);
    }

    public String getCity() {
        return getString(KEY_CITY, null);
    }

    public void setCity(String value) {
        setString(KEY_CITY, value);
    }

    public String getZipCode() {
        return getString(KEY_ZIPCODE, null);
    }

    public void setZipCode(String value) {
        setString(KEY_ZIPCODE, value);
    }

    public String getProjectName() {
        return getString(KEY_PROJECT, null);
    }

    public Long getProjectId() {
        long projectNameId = TableProjects.getInstance().queryProjectName(getProjectName());
        if (projectNameId >= 0) {
            return projectNameId;
        }
        return null;
    }

    public void setProject(String value) {
        setString(KEY_PROJECT, value);
    }

    public long getCurrentProjectGroupId() {
        return getLong(KEY_CURRENT_PROJECT_GROUP_ID, -1L);
    }

    public void setCurrentProjectGroupId(long id) {
        setLong(KEY_CURRENT_PROJECT_GROUP_ID, id);
    }

    public long getSavedProjectGroupId() {
        return getLong(KEY_SAVED_PROJECT_GROUP_ID, -1L);
    }

    public void setSavedProjectGroupId(long id) {
        setLong(KEY_SAVED_PROJECT_GROUP_ID, id);
    }

    public void setCurrentPictureCollectionId(long id) {
        setLong(KEY_CURRENT_PICTURE_COLLECTION_ID, id);
    }

    public long getCurrentPictureCollectionId() {
        return getLong(KEY_CURRENT_PICTURE_COLLECTION_ID, 0);
    }

    public void setFirstName(String name) {
        setString(KEY_FIRST_NAME, name);
    }

    public String getFirstName() {
        return getString(KEY_FIRST_NAME, null);
    }

    public void setLastName(String name) {
        setString(KEY_LAST_NAME, name);
    }

    public String getLastName() {
        return getString(KEY_LAST_NAME, null);
    }

    public boolean hasName() {
        return !TextUtils.isEmpty(getFirstName()) && !TextUtils.isEmpty(getLastName());
    }

    public long getTruckNumber() {
        return getLong(KEY_TRUCK_NUMBER, 0);
    }

    public void setTruckNumber(long id) {
        setLong(KEY_TRUCK_NUMBER, id);
    }

    public String getLicensePlate() {
        return getString(KEY_LICENSE_PLATE, null);
    }

    public void setLicensePlate(String id) {
        setString(KEY_LICENSE_PLATE, id);
    }

    public int getVersionProject() {
        return getInt(VERSION_PROJECT, VERSION_RESET);
    }

    public void setVersionProject(int value) {
        setInt(VERSION_PROJECT, value);
    }

    public int getVersionEquipment() {
        return getInt(VERSION_EQUIPMENT, VERSION_RESET);
    }

    public void setVersionEquipment(int value) {
        setInt(VERSION_EQUIPMENT, value);
    }

    public int getVersionNote() {
        return getInt(VERSION_NOTE, VERSION_RESET);
    }

    public void setVersionNote(int value) {
        setInt(VERSION_NOTE, value);
    }

    public int getVersionCompany() {
        return getInt(VERSION_COMPANY, VERSION_RESET);
    }

    public void setVersionCompany(int value) {
        setInt(VERSION_COMPANY, value);
    }

    public int getVersionTruck() {
        return getInt(VERSION_TRUCK, VERSION_RESET);
    }

    public void setVersionTruck(int value) {
        setInt(VERSION_TRUCK, value);
    }

    public String getKeyValue(String key) {
        return getString(key, null);
    }

    public void setKeyValue(String key, String value) {
        setString(key, value);
    }

    public void setStatus(TruckStatus status) {
        if (status == null) {
            setInt(KEY_STATUS, TruckStatus.UNKNOWN.ordinal());
        } else {
            setInt(KEY_STATUS, status.ordinal());
        }
    }

    public TruckStatus getStatus() {
        return TruckStatus.from(getInt(KEY_STATUS, TruckStatus.UNKNOWN.ordinal()));
    }

    public void setCurrentEditEntryId(long id) {
        setLong(KEY_EDIT_ENTRY_ID, id);
    }

    public long getCurrentEditEntryId() {
        return getLong(KEY_EDIT_ENTRY_ID, 0);
    }

    public DataEntry getCurrentEditEntry() {
        return TableEntry.getInstance().query(getCurrentEditEntryId());
    }

    public void reloadFromServer() {
        setVersionEquipment(VERSION_RESET);
        setVersionProject(VERSION_RESET);
        setVersionNote(VERSION_RESET);
        setVersionCompany(VERSION_RESET);
    }

    public void detectSpecialUpdateCheck() {
        int value = getInt(KEY_SPECIAL_UPDATE_CHECK, 0);
        if (value < 1) {
            reloadFromServer();
            setInt(KEY_SPECIAL_UPDATE_CHECK, 1);
        }
    }

    public boolean hasRegistrationChanged() {
        return getInt(KEY_REGISTRATION_CHANGED, 0) != 0;
    }

    public void setRegistrationChanged(boolean flag) {
        setInt(KEY_REGISTRATION_CHANGED, flag ? 1 : 0);
    }

    public boolean isDevelopment() {
        return getInt(KEY_IS_DEVELOPMENT, TBApplication.IsDevelopmentServer() ? 1 : 0) != 0;
    }

    // Return true if added.
    public List<String> addIfNotFound(List<String> list, String element) {
        if (element != null && !list.contains(element)) {
            list.add(element);
            Collections.sort(list);
        }
        return list;
    }

    public DataProjectAddressCombo getCurrentProjectGroup() {
        long projectGroupId = getCurrentProjectGroupId();
        return TableProjectAddressCombo.getInstance().query(projectGroupId);
    }

    public void setupFromCurrentProjectId() {
        DataProjectAddressCombo projectGroup = getCurrentProjectGroup();
        if (projectGroup != null) {
            setProject(projectGroup.getProjectName());
            DataAddress address = projectGroup.getAddress();
            if (address != null) {
                setState(address.state);
                setCity(address.city);
                setCompany(address.company);
                setStreet(address.street);
            }
        }
    }

    public void recoverProject() {
        setCurrentProjectGroupId(getSavedProjectGroupId());
        setupFromCurrentProjectId();
    }

    public void clearCurProject() {
        clearLastEntry();
        setState(null);
        setCity(null);
        setCompany(null);
        setStreet(null);
        setProject(null);
        setSavedProjectGroupId(getCurrentProjectGroupId());
        setCurrentProjectGroupId(-1L);
        TableEquipment.getInstance().clearChecked();
    }

    public void clearLastEntry() {
        setTruckNumber(0);
        setLicensePlate(null);
        setStatus(null);
        setCurrentEditEntryId(0);
        setCurrentPictureCollectionId(0);
        TablePictureCollection.getInstance().clearPendingPictures();
        TableNote.getInstance().clearValues();
        TableEquipment.getInstance().clearChecked();
    }

//    public boolean hasCurProject() {
//        long projectGroupId = getCurrentProjectGroupId();
//        if (projectGroupId < 0) {
//            return false;
//        }
//        DataProjectAddressCombo projectGroup = TableProjectAddressCombo.getInstance().query(projectGroupId);
//        if (projectGroup == null) {
//            return false;
//        }
//        return true;
//    }

    public boolean saveProjectAndAddressCombo() {
        String project = getProjectName();
        if (TextUtils.isEmpty((project))) {
            Timber.i("saveProjectAndAddressCombo(): quit on empty project");
            return false;
        }
        String company = getCompany();
        if (TextUtils.isEmpty(company)) {
            Timber.i("saveProjectAndAddressCombo(): quit on empty company");
            return false;
        }
        String state = getState();
        String street = getStreet();
        String city = getCity();
        String zipcode = getZipCode();
        long addressId;
        addressId = TableAddress.getInstance().queryAddressId(company, street, city, state, zipcode);
        if (addressId < 0) {
            DataAddress address = new DataAddress(company, street, city, state, zipcode);
            address.isLocal = true;
            addressId = TableAddress.getInstance().add(address);
            if (addressId < 0) {
                Timber.i("saveProjectAndAddressCombo(): could not find address: " + address.toString());
            }
        }
        long projectNameId = TableProjects.getInstance().queryProjectName(project);
        if (addressId >= 0 && projectNameId >= 0) {
            long projectGroupId = TableProjectAddressCombo.getInstance().queryProjectGroupId(projectNameId, addressId);
            if (projectGroupId < 0) {
                projectGroupId = TableProjectAddressCombo.getInstance().add(new DataProjectAddressCombo(projectNameId, addressId));
            } else {
                TableProjectAddressCombo.getInstance().updateUsed(projectGroupId);
            }
            setCurrentProjectGroupId(projectGroupId);
            return true;
        }
        Timber.i("saveProjectAndAddressCombo(): could not find project: " + project);
        return false;
    }

    public void setCurrentProjectGroup(DataProjectAddressCombo group) {
        setCurrentProjectGroupId(group.id);
        setProject(group.getProjectName());
        setAddress(group.getAddress());
    }

    void setAddress(DataAddress address) {
        setCompany(address.company);
        setStreet(address.street);
        setCity(address.city);
        setState(address.state);
    }

    public long getNextPictureCollectionID() {
        // Note: ID zero has a special meaning, it means that the set is pending.
        return getLong(KEY_NEXT_PICTURE_COLLECTION_ID, 1L);
    }

    public void incNextPictureCollectionID() {
        setLong(KEY_NEXT_PICTURE_COLLECTION_ID, getNextPictureCollectionID() + 1);
    }

    public long getNextEquipmentCollectionID() {
        return getLong(KEY_NEXT_EQUIPMENT_COLLECTION_ID, 0L);
    }

    public void incNextEquipmentCollectionID() {
        setLong(KEY_NEXT_EQUIPMENT_COLLECTION_ID, getNextEquipmentCollectionID() + 1);
    }

    public long getNextNoteCollectionID() {
        return getLong(KEY_NEXT_NOTE_COLLECTION_ID, 1L);
    }

    public void incNextNoteCollectionID() {
        setLong(KEY_NEXT_NOTE_COLLECTION_ID, getNextEquipmentCollectionID() + 1);
    }

    public DataEntry createEntry() {
        long projectGroupId = getCurrentProjectGroupId();
        if (projectGroupId < 0) {
            return null;
        }
        DataProjectAddressCombo projectGroup = TableProjectAddressCombo.getInstance().query(projectGroupId);
        if (projectGroup == null) {
            return null;
        }
        DataEntry entry = new DataEntry();
        entry.projectAddressCombo = projectGroup;
        entry.equipmentCollection = new DataCollectionEquipmentEntry(getNextEquipmentCollectionID());
        entry.equipmentCollection.addChecked();
        entry.pictureCollection = TablePictureCollection.getInstance().createCollectionFromPending();
        entry.truckId = TableTruck.getInstance().save(getTruckNumber(), getLicensePlate());
        entry.status = getStatus();
        entry.saveNotes(getNextNoteCollectionID());
        entry.date = System.currentTimeMillis();
        return entry;
    }

    public void setFromEntry(DataEntry entry) {
        setCurrentEditEntryId(entry.id);
        setCurrentProjectGroupId(entry.projectAddressCombo.id);
        setCurrentPictureCollectionId(entry.pictureCollection.id);
        entry.equipmentCollection.setChecked();
        DataTruck truck = entry.getTruck();
        if (truck != null) {
            setTruckNumber(truck.truckNumber);
            setLicensePlate(truck.licensePlateNumber);
        }
        setStatus(entry.status);
        TableNote.getInstance().clearValues();
    }

    public DataEntry saveEntry() {
        DataEntry entry = getCurrentEditEntry();
        if (entry == null) {
            return createEntry();
        }
        entry.equipmentCollection.addChecked();
        DataTruck truck = entry.getTruck();
        truck.truckNumber = (int) getTruckNumber();
        truck.licensePlateNumber = getLicensePlate();
        TableTruck.getInstance().save(truck);
        entry.status = getStatus();
        entry.uploadedMaster = false;
        entry.uploadedAws = false;
        // Be careful here: I use the date to match an entry when looking up the server id for older APP versions.
        if (entry.serverId > 0) {
            entry.date = System.currentTimeMillis();
        }
        entry.saveNotes();
        return entry;
    }

    public String genPictureFilename() {
        long tech_id = getTechID();
        long project_id = getProjectId();
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("picture_t");
        sbuf.append(tech_id);
        sbuf.append("_p");
        sbuf.append(project_id);
        sbuf.append("_d");
        SimpleDateFormat fmt = new SimpleDateFormat(PICTURE_DATE_FORMAT);
        sbuf.append(fmt.format(new Date(System.currentTimeMillis())));
        sbuf.append(".jpg");
        return sbuf.toString();
    }

    public File genFullPictureFile() {
        return new File(mCtx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), genPictureFilename());
    }

    public void clearUploaded() {
        setVersionNote(VERSION_RESET);
        setVersionProject(VERSION_RESET);
        setVersionEquipment(VERSION_RESET);
        setVersionEquipment(VERSION_RESET);
    }

    public String getAddress() {
        StringBuilder sbuf = new StringBuilder();
        String company = getCompany();
        if (company != null) {
            sbuf.append(company);
        }
        String city = getCity();
        if (city != null) {
            if (sbuf.length() > 0) {
                sbuf.append("\n");
            }
            sbuf.append(city);
        }
        String state = getState();
        if (state != null) {
            if (sbuf.length() > 0) {
                sbuf.append(", ");
            }
            sbuf.append(state);
        }
        String zip = getZipCode();
        if (zip != null) {
            if (sbuf.length() > 0) {
                sbuf.append(" ");
            }
            sbuf.append(zip);
        }
        return sbuf.toString();
    }

    public String getTruckValue() {
        String value;
        String license = getLicensePlate();
        long number = getTruckNumber();
        if (TextUtils.isEmpty(license)) {
            if (number > 0) {
                value = Long.toString(number);
            } else {
                value = "";
            }
        } else {
            if (number > 0) {
                value = DataTruck.toString(number, license);
            } else {
                value = license;
            }
        }
        return value;
    }

    public void parseTruckValue(String value) {
        if (value.contains(":")) {
            String[] ele = value.split(":");
            if (ele.length >= 2) {
                setTruckNumber(Integer.parseInt(ele[0].trim()));
                setLicensePlate(ele[1].trim());
            } else if (ele.length == 1) {
                if (TextUtils.isDigitsOnly(ele[0])) {
                    setTruckNumber(Integer.parseInt(ele[0].trim()));
                } else {
                    setLicensePlate(ele[0].trim());
                }
            } else {
                setTruckNumber(0);
                setLicensePlate(null);
            }
        } else if (TextUtils.isDigitsOnly(value)) {
            setTruckNumber(Long.parseLong(value));
        } else {
            setLicensePlate(value);
        }
    }

    public int getNumPicturesTaken() {
        long picture_collection_id = getCurrentPictureCollectionId();
        return TablePictureCollection.getInstance().countPictures(picture_collection_id);
    }

    public int getNumEquipPossible() {
        DataProjectAddressCombo curGroup = PrefHelper.getInstance().getCurrentProjectGroup();
        DataCollectionEquipmentProject collection = TableCollectionEquipmentProject.getInstance().queryForProject(curGroup.projectNameId);
        return collection.getEquipment().size();
    }

}