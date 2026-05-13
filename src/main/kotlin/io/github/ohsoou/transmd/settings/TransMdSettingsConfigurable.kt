package io.github.ohsoou.transmd.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class TransMdSettingsConfigurable : Configurable {

    private var component: TransMdSettingsComponent? = null
    private val credAttrs = CredentialAttributes("io.github.ohsoou.transmd", "google-translate-api-key")
    private var originalApiKey: String? = null  // null until loaded from PasswordSafe

    private fun loadApiKeyAsync(onLoaded: (String) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val key = PasswordSafe.instance.getPassword(credAttrs) ?: ""
            ApplicationManager.getApplication().invokeLater { onLoaded(key) }
        }
    }

    override fun getDisplayName(): String = "Trans Md"

    override fun createComponent(): JComponent {
        val comp = TransMdSettingsComponent()
        component = comp
        return comp.panel
    }

    override fun isModified(): Boolean {
        val comp = component ?: return false
        val key = originalApiKey ?: return false  // not yet loaded → treat as unmodified
        return comp.targetLang != TransMdSettings.getInstance().targetLang
            || comp.apiKey != key
    }

    override fun apply() {
        val comp = component ?: return
        TransMdSettings.getInstance().targetLang = comp.targetLang
        val key = comp.apiKey
        originalApiKey = key
        ApplicationManager.getApplication().executeOnPooledThread {
            PasswordSafe.instance.set(credAttrs, if (key.isBlank()) null else Credentials("google-translate-api-key", key))
        }
    }

    override fun reset() {
        val comp = component ?: return
        comp.targetLang = TransMdSettings.getInstance().targetLang
        loadApiKeyAsync { key ->
            originalApiKey = key
            comp.apiKey = key
        }
    }

    override fun disposeUIResources() {
        component = null
        originalApiKey = null
    }
}