package com.hamalawy.cleanarchtemplate

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "DirectorySettings", storages = [Storage("DirectorySettings.xml")])
class DirectorySettings : PersistentStateComponent<DirectorySettings.State> {
    var appDirectory: String? = null
    var dataDirectory: String? = null
    var domainDirectory: String? = null

    override fun getState(): State {
        val state = State()
        state.appDirectory = this.appDirectory
        state.dataDirectory = this.dataDirectory
        state.domainDirectory = this.domainDirectory
        return state
    }

    override fun loadState(state: State) {
        this.appDirectory = state.appDirectory
        this.dataDirectory = state.dataDirectory
        this.domainDirectory = state.domainDirectory
    }

    class State {
        var appDirectory: String? = null
        var dataDirectory: String? = null
        var domainDirectory: String? = null
    }

    companion object {
        val instance: DirectorySettings
            get() = ServiceManager.getService(DirectorySettings::class.java)
    }
}
