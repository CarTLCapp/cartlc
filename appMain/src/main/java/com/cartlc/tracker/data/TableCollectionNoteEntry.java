package com.cartlc.tracker.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.cartlc.tracker.app.TBApplication;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by dug on 5/16/17.
 */

public class TableCollectionNoteEntry {

    static final String TABLE_NAME = "note_entry_collection";

    static final String KEY_ROWID         = "_id";
    static final String KEY_COLLECTION_ID = "collection_id";
    static final String KEY_NOTE_ID       = "note_id";
    static final String KEY_VALUE         = "value";

    static TableCollectionNoteEntry sInstance;

    static void Init(SQLiteDatabase db) {
        new TableCollectionNoteEntry(db);
    }

    public static TableCollectionNoteEntry getInstance() {
        return sInstance;
    }

    final SQLiteDatabase mDb;

    public TableCollectionNoteEntry(SQLiteDatabase db) {
        mDb = db;
        sInstance = this;
    }

    public void create() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("create table ");
        sbuf.append(TABLE_NAME);
        sbuf.append(" (");
        sbuf.append(KEY_ROWID);
        sbuf.append(" integer primary key autoincrement, ");
        sbuf.append(KEY_COLLECTION_ID);
        sbuf.append(" long, ");
        sbuf.append(KEY_NOTE_ID);
        sbuf.append(" long, ");
        sbuf.append(KEY_VALUE);
        sbuf.append(" text)");

        mDb.execSQL(sbuf.toString());
    }

    public int countNotes(long noteId) {
        int count = 0;
        try {
            String where = KEY_NOTE_ID + "=?";
            String[] whereArgs = new String[]{Long.toString(noteId)};
            Cursor cursor = mDb.query(TABLE_NAME, null, where, whereArgs, null, null, null);
            count = cursor.getCount();
            cursor.close();
        } catch (Exception ex) {
            TBApplication.ReportError(ex, TableCollectionNoteEntry.class, "countNotes(id)", "db");
        }
        return count;
    }

    // There are TWO note tables. One is TableCollectionnNoteProject which stores the
    // defined notes for each project. The other is this one which stores the values.
    //
    // The values are stored right now in the TableNote table which is represented by the incoming
    // notes. We want them to also be stored now into this table.
    public void save(long collectionId, List<DataNote> notes) {
        mDb.beginTransaction();
        try {
            removeCollection(collectionId);
            ContentValues values = new ContentValues();
            for (DataNote note : notes) {
                if (!TextUtils.isEmpty(note.value)) {
                    values.clear();
                    values.put(KEY_COLLECTION_ID, collectionId);
                    values.put(KEY_NOTE_ID, note.id);
                    values.put(KEY_VALUE, note.value);
                    mDb.insert(TABLE_NAME, null, values);
                }
            }
            mDb.setTransactionSuccessful();
        } catch (Exception ex) {
            TBApplication.ReportError(ex, TableCollectionNoteEntry.class, "save()", "db");
        } finally {
            mDb.endTransaction();
        }
    }

    // Get list of notes the associated collection id in this table and their values.
    public List<DataNote> query(long collectionId) {
        List<DataNote> list = new ArrayList();
        try {
            final String[] columns = {KEY_NOTE_ID, KEY_VALUE};
            final String selection = KEY_COLLECTION_ID + " =?";
            final String[] selectionArgs = {Long.toString(collectionId)};
            Cursor cursor = mDb.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
            int idxNoteId = cursor.getColumnIndex(KEY_NOTE_ID);
            int idxValueId = cursor.getColumnIndex(KEY_VALUE);
            DataNote note;
            while (cursor.moveToNext()) {
                note = TableNote.getInstance().query(cursor.getLong(idxNoteId)); // Fill out with original values.
                note.value = cursor.getString(idxValueId); // override
                list.add(note);
            }
            cursor.close();
        } catch (Exception ex) {
            TBApplication.ReportError(ex, TableCollectionNoteEntry.class, "query()", "db");
        }
        return list;
    }

    void removeCollection(long collection_id) {
        String where = KEY_COLLECTION_ID + "=?";
        String[] whereArgs = {Long.toString(collection_id)};
        mDb.delete(TABLE_NAME, where, whereArgs);
    }

}
