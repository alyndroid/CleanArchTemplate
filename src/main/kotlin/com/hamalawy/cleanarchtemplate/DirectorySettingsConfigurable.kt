package com.hamalawy.cleanarchtemplate

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DirectorySettingsConfigurable : Configurable {
    private var settingsPanel: JPanel? = null
    private var appDirectoryField: TextFieldWithBrowseButton? = null
    private var dataDirectoryField: TextFieldWithBrowseButton? = null
    private var domainDirectoryField: TextFieldWithBrowseButton? = null

    override fun createComponent(): JComponent? {
        settingsPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridy = GridBagConstraints.RELATIVE
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.NORTHWEST
            insets = JBUI.insets(5)
            weightx = 1.0
        }

        appDirectoryField = TextFieldWithBrowseButton()
        dataDirectoryField = TextFieldWithBrowseButton()
        domainDirectoryField = TextFieldWithBrowseButton()

        val appDirectoryLabel = JLabel("App Directory")
        val dataDirectoryLabel = JLabel("Data Directory")
        val domainDirectoryLabel = JLabel("Domain Directory")

        // Add labels and fields to the panel
        settingsPanel?.add(appDirectoryLabel, constraints)
        settingsPanel?.add(appDirectoryField, constraints)

        settingsPanel?.add(dataDirectoryLabel, constraints)
        settingsPanel?.add(dataDirectoryField, constraints)

        settingsPanel?.add(domainDirectoryLabel, constraints)
        settingsPanel?.add(domainDirectoryField, constraints)

        // Set the saved directories to the text fields
        val settings = DirectorySettings.instance
        appDirectoryField?.text = settings.appDirectory ?: ""
        dataDirectoryField?.text = settings.dataDirectory ?: ""
        domainDirectoryField?.text = settings.domainDirectory ?: ""

        // Add file choosers for directory selection
        appDirectoryField?.addBrowseFolderListener(
            "Select App Directory",
            "Select the directory where app-related classes will be created",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )

        dataDirectoryField?.addBrowseFolderListener(
            "Select Data Directory",
            "Select the directory where data-related classes will be created",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )

        domainDirectoryField?.addBrowseFolderListener(
            "Select Domain Directory",
            "Select the directory where domain-related classes will be created",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )

        return settingsPanel
    }

    override fun isModified(): Boolean {
        val settings = DirectorySettings.instance
        return settings.appDirectory != appDirectoryField?.text ||
                settings.dataDirectory != dataDirectoryField?.text ||
                settings.domainDirectory != domainDirectoryField?.text
    }

    override fun apply() {
        val settings = DirectorySettings.instance
        settings.appDirectory = appDirectoryField?.text
        settings.dataDirectory = dataDirectoryField?.text
        settings.domainDirectory = domainDirectoryField?.text
    }

    override fun getDisplayName(): String {
        return "Directory Settings"
    }

    override fun reset() {
        val settings = DirectorySettings.instance
        appDirectoryField?.text = settings.appDirectory ?: ""
        dataDirectoryField?.text = settings.dataDirectory ?: ""
        domainDirectoryField?.text = settings.domainDirectory ?: ""
    }
}
