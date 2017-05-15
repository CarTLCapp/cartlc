package com.cartlc.trackbattery.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by dug on 4/17/17.
 */

public class TableEquipment {

    static final String TABLE_NAME = "list_equipment";

    static final String KEY_ROWID = "_id";
    static final String KEY_NAME = "name";
    static final String KEY_PROJECT_ID = "project_id";
    static final String KEY_CHECKED = "is_checked";
    static final String KEY_LOCAL = "is_local";

    static TableEquipment sInstance;

    static void Init(SQLiteDatabase db) {
        new TableEquipment(db);
    }

    public static TableEquipment getInstance() {
        return sInstance;
    }

    final SQLiteDatabase mDb;

    TableEquipment(SQLiteDatabase db) {
        this.mDb = db;
        sInstance = this;
    }

    public void clear() {
        try {
            mDb.delete(TABLE_NAME, null, null);
        } catch (Exception ex) {
        }
    }

    public void create() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("create table ");
        sbuf.append(TABLE_NAME);
        sbuf.append(" (");
        sbuf.append(KEY_ROWID);
        sbuf.append(" integer primary key autoincrement, ");
        sbuf.append(KEY_NAME);
        sbuf.append(" text, ");
        sbuf.append(KEY_PROJECT_ID);
        sbuf.append(" long, ");
        sbuf.append(KEY_CHECKED);
        sbuf.append(" bit, ");
        sbuf.append(KEY_LOCAL);
        sbuf.append(" bit)");
        mDb.execSQL(sbuf.toString());
    }

    public int count() {
        int count = 0;
        try {
            Cursor cursor = mDb.query(TABLE_NAME, null, null, null, null, null, null);
            count = cursor.getCount();
            cursor.close();
        } catch (Exception ex) {
            Timber.e(ex);
        }
        return count;
    }

    public long add(String name, long projectId) {
        long id = -1L;
        mDb.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_NAME, name);
            values.put(KEY_PROJECT_ID, projectId);
            id = mDb.insert(TABLE_NAME, null, values);
            mDb.setTransactionSuccessful();
        } catch (Exception ex) {
            Timber.e(ex);
        } finally {
            mDb.endTransaction();
        }
        return id;
    }

    public long addLocal(String name, long projectId) {
        long id = -1L;
        mDb.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_NAME, name);
            values.put(KEY_PROJECT_ID, projectId);
            values.put(KEY_LOCAL, 1);
            id = mDb.insert(TABLE_NAME, null, values);
            mDb.setTransactionSuccessful();
        } catch (Exception ex) {
            Timber.e(ex);
        } finally {
            mDb.endTransaction();
        }
        return id;
    }

    public void add(List<DataEquipment> list) {
        mDb.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            for (DataEquipment value : list) {
                values.clear();
                values.put(KEY_NAME, value.name);
                values.put(KEY_PROJECT_ID, value.projectId);
                values.put(KEY_CHECKED, value.isChecked ? 1 : 0);
                values.put(KEY_LOCAL, value.isLocal ? 1 : 0);
                value.id = mDb.insert(TABLE_NAME, null, values);
            }
            mDb.setTransactionSuccessful();
        } catch (Exception ex) {
            Timber.e(ex);
        } finally {
            mDb.endTransaction();
        }
    }

    public DataEquipment query(long id) {
        DataEquipment item = null;
        try {
            final String[] columns = {KEY_PROJECT_ID, KEY_NAME, KEY_CHECKED, KEY_LOCAL};
            final String selection = KEY_ROWID + "=?";
            final String[] selectionArgs = {Long.toString(id)};

            Cursor cursor = mDb.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
            final int idxProjectId = cursor.getColumnIndex(KEY_PROJECT_ID);
            final int idxName = cursor.getColumnIndex(KEY_NAME);
            final int idxChecked = cursor.getColumnIndex(KEY_CHECKED);
            final int idxLocal = cursor.getColumnIndex(KEY_LOCAL);

            if (cursor.moveToFirst()) {
                long projectId = cursor.getLong(idxProjectId);
                String name = cursor.getString(idxName);
                boolean checked = cursor.getShort(idxChecked) != 0 ? true : false;
                boolean local = cursor.getShort(idxLocal) != 0 ? true : false;
                item = new DataEquipment(id, name, projectId, checked, local);
            }
            cursor.close();
        } catch (Exception ex) {
            Timber.e(ex);
        }
        return item;
    }

    public List<DataEquipment> queryForProject(long projectNameId) {
        ArrayList<DataEquipment> list = new ArrayList();
        try {
            final String[] columns = {KEY_ROWID, KEY_NAME, KEY_CHECKED, KEY_LOCAL};
            final String orderBy = KEY_NAME + " ASC";
            final String selection = KEY_PROJECT_ID + "=?";
            final String[] selectionArgs = {Long.toString(projectNameId)};

            Cursor cursor = mDb.query(TABLE_NAME, columns, selection, selectionArgs, null, null, orderBy, null);
            final int idxRowId = cursor.getColumnIndex(KEY_ROWID);
            final int idxName = cursor.getColumnIndex(KEY_NAME);
            final int idxChecked = cursor.getColumnIndex(KEY_CHECKED);
            final int idxLocal = cursor.getColumnIndex(KEY_LOCAL);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idxRowId);
                String name = cursor.getString(idxName);
                boolean checked = cursor.getShort(idxChecked) != 0 ? true : false;
                boolean local = cursor.getShort(idxLocal) != 0 ? true : false;
                list.add(new DataEquipment(id, name, projectNameId, checked, local));
            }
            cursor.close();
        } catch (Exception ex) {
            Timber.e(ex);
        }
        return list;
    }

    public List<Long> queryChecked(long projectNameId) {
        ArrayList<Long> list = new ArrayList();
        try {
            final String[] columns = {KEY_ROWID};
            StringBuilder sbuf = new StringBuilder();
            sbuf.append(KEY_PROJECT_ID);
            sbuf.append("=?");
            sbuf.append(" AND ");
            sbuf.append(KEY_CHECKED);
            sbuf.append("=1");
            final String selection = sbuf.toString();
            final String[] selectionArgs = {Long.toString(projectNameId)};

            Cursor cursor = mDb.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
            final int idxRowId = cursor.getColumnIndex(KEY_ROWID);

            while (cursor.moveToNext()) {
                list.add(cursor.getLong(idxRowId));
            }
            cursor.close();
        } catch (Exception ex) {
            Timber.e(ex);
        }
        return list;
    }

    public List<DataEquipment> queryForProject() {
        ArrayList<DataEquipment> list = new ArrayList();
        try {
            final String[] columns = {KEY_ROWID, KEY_PROJECT_ID, KEY_NAME, KEY_CHECKED, KEY_LOCAL};
            Cursor cursor = mDb.query(TABLE_NAME, columns, null, null, null, null, null, null);
            final int idxRowId = cursor.getColumnIndex(KEY_ROWID);
            final int idxName = cursor.getColumnIndex(KEY_NAME);
            final int idxChecked = cursor.getColumnIndex(KEY_CHECKED);
            final int idxProjectId = cursor.getColumnIndex(KEY_PROJECT_ID);
            final int idxLocal = cursor.getColumnIndex(KEY_LOCAL);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idxRowId);
                String name = cursor.getString(idxName);
                boolean checked = cursor.getShort(idxChecked) != 0 ? true : false;
                boolean local = cursor.getShort(idxLocal) != 0 ? true : false;
                long projectId = cursor.getLong(idxProjectId);
                list.add(new DataEquipment(id, name, projectId, checked, local));
            }
            cursor.close();
        } catch (Exception ex) {
            Timber.e(ex);
        }
        return list;
    }

    public void setChecked(DataEquipment item, boolean flag) {
        item.isChecked = flag;
        mDb.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            String where = KEY_ROWID + "=?";
            String[] whereArgs = {Long.toString(item.id)};
            values.put(KEY_CHECKED, item.isChecked ? 1 : 0);
            mDb.update(TABLE_NAME, values, where, whereArgs);
            mDb.setTransactionSuccessful();
        } catch (Exception ex) {
            Timber.e(ex);
        } finally {
            mDb.endTransaction();
        }
    }

    public void clearChecked() {
        mDb.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_CHECKED, 0);
            mDb.update(TABLE_NAME, values, null, null);
            mDb.setTransactionSuccessful();
        } catch (Exception ex) {
            Timber.e(ex);
        } finally {
            mDb.endTransaction();
        }
    }
}
