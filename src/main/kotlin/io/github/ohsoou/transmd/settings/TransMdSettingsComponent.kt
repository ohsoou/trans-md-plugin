package io.github.ohsoou.transmd.settings

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JPasswordField

class TransMdSettingsComponent {

    private val targetLangCombo = JComboBox(SUPPORTED_LANGUAGES.map { it.label }.toTypedArray())
    private val apiKeyField = JPasswordField(30)

    val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Target Language:"), targetLangCombo, 1, false)
        .addLabeledComponent(JBLabel("Google Translate API Key:"), apiKeyField, 1, false)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    var targetLang: String
        get() = SUPPORTED_LANGUAGES[targetLangCombo.selectedIndex].code
        set(value) {
            val index = SUPPORTED_LANGUAGES.indexOfFirst { it.code == value }
            if (index >= 0) targetLangCombo.selectedIndex = index
        }

    var apiKey: String
        get() = String(apiKeyField.password)
        set(value) { apiKeyField.text = value }

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            Language("ko", "한국어"),
            Language("en", "English"),
            Language("ja", "日本語"),
            Language("zh-CN", "中文 (简体)"),
            Language("es", "Español"),
            Language("fr", "Français"),
            Language("de", "Deutsch"),
        )
    }

    data class Language(val code: String, val label: String)
}