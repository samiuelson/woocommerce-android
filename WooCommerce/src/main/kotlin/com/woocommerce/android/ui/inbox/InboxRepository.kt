package com.woocommerce.android.ui.inbox

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.inbox.domain.InboxNote
import com.woocommerce.android.ui.inbox.domain.InboxNoteAction
import com.woocommerce.android.ui.inbox.domain.MarkNoteAsActioned
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions
import org.wordpress.android.fluxc.store.WCInboxStore
import javax.inject.Inject

class InboxRepository @Inject constructor(
    private val inboxStore: WCInboxStore,
    private val selectedSite: SelectedSite
) {
    suspend fun fetchInboxNotes() {
        inboxStore.fetchInboxNotes(selectedSite.get())
    }

    fun observeInboxNotes(): Flow<List<InboxNote>> =
        inboxStore.observeInboxNotes(selectedSite.get().siteId)
            .map { inboxNotesWithActions ->
                inboxNotesWithActions.map { it.toInboxNote() }
            }

    suspend fun markInboxNoteAsActioned(noteId: Long, noteActionId: Long): MarkNoteAsActioned.MarkNoteActionedResult {
        val result = inboxStore.markInboxNoteAsActioned(
            selectedSite.get(),
            noteId,
            noteActionId
        )
        return when {
            result.isError -> MarkNoteAsActioned.Fail
            else -> MarkNoteAsActioned.Success
        }
    }

    suspend fun dismissNote(noteId: Long) {
        inboxStore.deleteNote(selectedSite.get(), noteId)
    }

    private fun InboxNoteWithActions.toInboxNote() =
        InboxNote(
            id = inboxNote.remoteId,
            title = inboxNote.title,
            description = inboxNote.content,
            dateCreated = inboxNote.dateCreated,
            status = inboxNote.status.toInboxNoteStatus(),
            actions = noteActions.map { it.toInboxAction() },
        )

    private fun InboxNoteActionEntity.toInboxAction() =
        InboxNoteAction(
            id = remoteId,
            label = label,
            isPrimary = primary,
            url = url,
        )

    private fun InboxNoteEntity.LocalInboxNoteStatus.toInboxNoteStatus() =
        when (this) {
            InboxNoteEntity.LocalInboxNoteStatus.Unactioned -> InboxNote.Status.Unactioned
            InboxNoteEntity.LocalInboxNoteStatus.Actioned -> InboxNote.Status.Actioned
            InboxNoteEntity.LocalInboxNoteStatus.Snoozed -> InboxNote.Status.Snoozed
            InboxNoteEntity.LocalInboxNoteStatus.Unknown -> InboxNote.Status.Unknown
        }
}
