package io.github.ohsoou.transmd.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "TransMdSettings",
    storages = [Storage("TransMdSettings.xml")]
)
@Service(Service.Level.APP)
class TransMdSettings : PersistentStateComponent<TransMdSettings.State> {

    data class State(
        var targetLang: String = "ko"
    )

    private var state = State()

    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    var targetLang: String
        get() = state.targetLang
        set(value) { state.targetLang = value }

    companion object {
        fun getInstance(): TransMdSettings =
            ApplicationManager.getApplication().getService(TransMdSettings::class.java)
    }
}