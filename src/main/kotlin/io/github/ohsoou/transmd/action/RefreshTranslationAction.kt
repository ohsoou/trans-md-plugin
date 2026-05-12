package io.github.ohsoou.transmd.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import io.github.ohsoou.transmd.editor.TranslatedPreviewFileEditor

class RefreshTranslationAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.extension?.lowercase() == "md"
        e.presentation.text = "🔄"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = e.project ?: return
        FileEditorManager.getInstance(project)
            .getEditors(file)
            .filterIsInstance<TranslatedPreviewFileEditor>()
            .forEach { it.startTranslation() }
    }
}