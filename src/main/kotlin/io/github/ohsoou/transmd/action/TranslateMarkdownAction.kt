package io.github.ohsoou.transmd.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import io.github.ohsoou.transmd.editor.TranslatedPreviewFileEditor
import io.github.ohsoou.transmd.settings.TransMdSettings
import io.github.ohsoou.transmd.settings.TransMdSettingsComponent

class TranslateMarkdownAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val isMarkdown = file?.extension?.lowercase() == "md"
        e.presentation.isEnabledAndVisible = isMarkdown
        if (isMarkdown) {
            val targetLang = TransMdSettings.getInstance().targetLang
            val label = TransMdSettingsComponent.SUPPORTED_LANGUAGES.find { it.code == targetLang }?.label ?: targetLang
            e.presentation.text = "🌐 Auto → $label"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val languages = TransMdSettingsComponent.SUPPORTED_LANGUAGES
        val settings = TransMdSettings.getInstance()

        val popup = JBPopupFactory.getInstance().createListPopup(
            object : BaseListPopupStep<TransMdSettingsComponent.Language>("Target Language", languages) {
                override fun getTextFor(value: TransMdSettingsComponent.Language) = value.label
                override fun onChosen(value: TransMdSettingsComponent.Language, finalChoice: Boolean): PopupStep<*>? {
                    settings.targetLang = value.code
                    // Retranslate if Translated Preview tab is currently open
                    retranslateIfOpen(e)
                    return FINAL_CHOICE
                }
            }
        )
        popup.showInBestPositionFor(e.dataContext)
    }

    private fun retranslateIfOpen(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = e.project ?: return
        val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
        fileEditorManager.getEditors(file)
            .filterIsInstance<TranslatedPreviewFileEditor>()
            .forEach { it.startTranslation() }
    }
}