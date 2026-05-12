package io.github.ohsoou.transmd.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class TransMdSettingsConfigurable : Configurable {

    private var component: TransMdSettingsComponent? = null

    private val credAttrs = CredentialAttributes("io.github.ohsoou.transmd", "google-translate-api-key")

    private fun readApiKey(): String = PasswordSafe.instance.getPassword(credAttrs) ?: ""

    private fun writeApiKey(key: String) {
        PasswordSafe.instance.set(credAttrs, if (key.isBlank()) null else Credentials("google-translate-api-key", key))
    }

    override fun getDisplayName(): String = "Markdown Translator"

    override fun createComponent(): JComponent {
        val comp = TransMdSettingsComponent()
        component = comp
        return comp.panel
    }

    override fun isModified(): Boolean {
        val comp = component ?: return false
        return comp.targetLang != TransMdSettings.getInstance().targetLang
            || comp.apiKey != readApiKey()
    }

    override fun apply() {
        val comp = component ?: return
        TransMdSettings.getInstance().targetLang = comp.targetLang
        writeApiKey(comp.apiKey)
    }

    override fun reset() {
        val comp = component ?: return
        comp.targetLang = TransMdSettings.getInstance().targetLang
        comp.apiKey = readApiKey()
    }

    override fun disposeUIResources() {
        component = null
    }
}