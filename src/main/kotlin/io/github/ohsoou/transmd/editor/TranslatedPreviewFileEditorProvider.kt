package io.github.ohsoou.transmd.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.github.ohsoou.transmd.service.TranslationCache
import io.github.ohsoou.transmd.service.TranslationJob
import io.github.ohsoou.transmd.service.impl.GoogleTranslateService

class TranslatedPreviewFileEditorProvider : FileEditorProvider, DumbAware {

    override fun getEditorTypeId(): String = "trans-md-translated-preview"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR

    override fun accept(project: Project, file: VirtualFile): Boolean =
        file.extension?.lowercase() == "md"

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val cache = ApplicationManager.getApplication().getService(TranslationCache::class.java)
        val job = TranslationJob(cache, providerFactory = { apiKey -> GoogleTranslateService(apiKey) })
        return TranslatedPreviewFileEditor(project, file, job)
    }
}
