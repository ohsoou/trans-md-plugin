package io.github.ohsoou.transmd.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class TranslatedPreviewFileEditorProvider : FileEditorProvider, DumbAware {

    override fun getEditorTypeId(): String = "trans-md-translated-preview"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR

    override fun accept(project: Project, file: VirtualFile): Boolean =
        file.extension?.lowercase() == "md"

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        TranslatedPreviewFileEditor(project, file)
}