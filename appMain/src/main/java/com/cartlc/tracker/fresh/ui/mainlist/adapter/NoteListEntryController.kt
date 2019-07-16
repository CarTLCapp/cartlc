/**
 * Copyright 2019, FleetTLC. All rights reserved
 */
package com.cartlc.tracker.fresh.ui.mainlist.adapter

import com.cartlc.tracker.fresh.model.core.data.DataEntry
import com.cartlc.tracker.fresh.model.core.data.DataNote
import com.cartlc.tracker.fresh.model.core.data.DataProjectAddressCombo
import com.cartlc.tracker.fresh.ui.mainlist.adapter.item.NoteListEntryItemViewMvc
import com.cartlc.tracker.model.CarRepository
import com.cartlc.tracker.model.flow.Flow
import com.cartlc.tracker.model.flow.Stage
import com.cartlc.tracker.model.misc.EntryHint
import java.util.ArrayList

class NoteListEntryController(
        private val repo: CarRepository,
        private val listener: Listener
) : NoteListEntryAdapter.Listener,
        NoteListEntryUseCase {

    interface Listener {
        fun onEntryHintChanged(entryHint: EntryHint)
        fun onNotesChanged(items: List<DataNote>)
    }

    private val prefHelper = repo.prefHelper
    private val currentEditEntry: DataEntry?
        get() = prefHelper.currentEditEntry
    private val currentProjectGroup: DataProjectAddressCombo?
        get() = prefHelper.currentProjectGroup

    private var currentFocus: DataNote? = null

    private val curFlowValue: Flow
        get() = repo.curFlowValue

    private val isInNotes: Boolean
        get() = curFlowValue.stage == Stage.NOTES

    private var items: MutableList<DataNote> = mutableListOf()

    // region NoteListEntryAdapter.Listener

    override fun onBindViewHolder(viewMvc: NoteListEntryItemViewMvc, position: Int) {
        val item = items[position]
        viewMvc.label = item.name
        viewMvc.entryText = item.value
        viewMvc.bind(object : NoteListEntryItemViewMvc.Listener {
            override fun afterTextChanged(text: String) {
                onAfterTextChanged(viewMvc, item, text)
            }

            override fun onEntryFocused(hasFocus: Boolean) {
                viewMvc.isSelected = hasFocus
                onNoteFocused(item)
            }
        })
        when {
            item.type === DataNote.Type.ALPHANUMERIC -> {
                viewMvc.inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                viewMvc.maxLines = 1
            }
            item.type === DataNote.Type.NUMERIC_WITH_SPACES -> {
                viewMvc.inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_CLASS_NUMBER
                viewMvc.maxLines = 1
            }
            item.type === DataNote.Type.NUMERIC -> {
                viewMvc.inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_CLASS_NUMBER
                viewMvc.maxLines = 1
            }
            item.type === DataNote.Type.MULTILINE -> {
                viewMvc.inputType = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                viewMvc.maxLines = 3
                viewMvc.numLines = 3
            }
            else -> {
                viewMvc.inputType = android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                viewMvc.maxLines = 1
            }
        }

    }

    private fun onAfterTextChanged(viewMvc: NoteListEntryItemViewMvc, item: DataNote, text: String?) {
        if (viewMvc.isSelected) {
            item.value = text
            updateNoteValue(item)
            onNoteChanged(item)
        }
    }

    private fun onNoteChanged(note: DataNote) {
        if (currentFocus === note) {
            display(note)
        }
    }

    private fun onNoteFocused(note: DataNote) {
        currentFocus = note
        display(note)
    }

    private fun display(note: DataNote) {
        if (!isInNotes) {
            return
        }
        val entryHint: EntryHint

        if (note.num_digits > 0) {
            if (note.value != null && note.value!!.isNotBlank()) {
                val sbuf = StringBuilder()
                val count = note.value!!.length
                sbuf.append(count)
                sbuf.append("/")
                sbuf.append(note.num_digits.toInt())
                entryHint = EntryHint(sbuf.toString(), (count > note.num_digits))
            } else {
                entryHint = EntryHint("", false)
            }
        } else {
            entryHint = EntryHint("", false)
        }
        listener.onEntryHintChanged(entryHint)
    }

    private fun updateNoteValue(note: DataNote) {
        repo.db.tableNote.updateValue(note)
    }

    // endregion NoteListEntryAdapter.Listener

    // region NoteListEntryUseCase

    override val numNotes: Int
        get() = items.size

    override val notes: List<DataNote>
        get() = items

    override fun onNoteDataChanged() {
        items = currentEditEntry?.let {
            it.notesAllWithValuesOverlaid.toMutableList()
        } ?: run {
            queryNotes().toMutableList()
        }
        pushToBottom("Other")
        listener.onNotesChanged(items)
    }

    private fun queryNotes(): List<DataNote> =
            currentProjectGroup?.let { repo.db.tableCollectionNoteProject.getNotes(it.projectNameId) }
                    ?: emptyList()

    private fun pushToBottom(name: String) {
        val others = ArrayList<DataNote>()
        for (item in items) {
            if (item.name.startsWith(name)) {
                others.add(item)
                break
            }
        }
        for (item in others) {
            items.remove(item)
            items.add(item)
        }
    }

    // endregion NoteListEntryUseCase
}