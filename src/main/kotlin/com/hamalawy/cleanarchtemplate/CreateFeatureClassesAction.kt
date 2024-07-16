package com.hamalawy.cleanarchtemplate

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.io.IOException
import javax.swing.Icon

class CreateFeatureClassesAction : AnAction() {

    private val customIcon: Icon = IconLoader.getIcon("/icons/unnamed.png", CreateFeatureClassesAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: run {
            Messages.showErrorDialog("Project not found", "Error")
            return
        }

        val settings = DirectorySettings.instance
        val appDirectoryPath = settings.appDirectory ?: run {
            Messages.showErrorDialog("No app directory selected in settings", "Error")
            return
        }
        val dataDirectoryPath = settings.dataDirectory ?: run {
            Messages.showErrorDialog("No data directory selected in settings", "Error")
            return
        }
        val domainDirectoryPath = settings.domainDirectory ?: run {
            Messages.showErrorDialog("No domain directory selected in settings", "Error")
            return
        }

        val featureName = Messages.showInputDialog(project, "Enter the feature name:", "Create New Feature", customIcon)
        if (featureName.isNullOrEmpty()) {
            Messages.showErrorDialog("Feature name cannot be empty", "Error")
            return
        }

        val featureNameLower = featureName[0].lowercase() + featureName.substring(1)

        val appPath = "$appDirectoryPath/$featureNameLower"
        val dataPath = "$dataDirectoryPath/$featureNameLower"
        val domainPath = "$domainDirectoryPath/$featureNameLower"

        try {
            createDirectories(appPath, dataPath, domainPath, featureNameLower)

            val appPackage = getPackageNameFromPath(appPath, appDirectoryPath)
            val dataPackage = getPackageNameFromPath(dataPath, dataDirectoryPath)
            val domainPackage = getPackageNameFromPath(domainPath, domainDirectoryPath)

            if (appPackage == null || dataPackage == null || domainPackage == null) {
                Messages.showErrorDialog("Failed to determine package name", "Error")
                return
            }

            createFiles(appPath, dataPath, domainPath, featureName, featureNameLower, appPackage, dataPackage, domainPackage)

            Messages.showInfoMessage("Feature $featureName created successfully", "Success")
            refreshProjectView(project, appPath, domainPath, dataPath, appDirectoryPath, dataDirectoryPath, domainDirectoryPath)
        } catch (ex: IOException) {
            Messages.showErrorDialog("Failed to create feature: ${ex.message}", "Error")
        }
    }

    private fun getPackageNameFromPath(path: String, basePath: String): String? {
        val relativePath = File(basePath).toURI().relativize(File(path).toURI()).path
        return relativePath.replace(File.separatorChar, '.').removeSuffix(".")
    }

    private fun createDirectories(appPath: String, dataPath: String, domainPath: String, featureNameLower: String) {
        val dirs = listOf(
            appPath,
            dataPath,
            domainPath,
            "$appPath/viewmodel",
            "$appPath/di",
            "$domainPath/entity",
            "$domainPath/repo",
            "$domainPath/usecase",
            "$dataPath/remote",
            "$dataPath/repo",
            "$dataPath/mapper"
        )

        for (dir in dirs) {
            val file = File(dir)
            if (!file.exists()) {
                file.mkdirs()
            }
        }
    }

    private fun createFiles(
        appPath: String,
        dataPath: String,
        domainPath: String,
        featureName: String,
        featureNameLower: String,
        appPackage: String,
        dataPackage: String,
        domainPackage: String
    ) {
        val filesContent = mapOf(
            // Domain Layer
            "$domainPath/entity/${featureName}Entity.kt" to """
                package $domainPackage.entity

                data class ${featureName}Entity(
                    val id: Int,
                    val name: String
                )
            """.trimIndent(),

            "$domainPath/repo/${featureName}Repository.kt" to """
                package $domainPackage.repo
                
                import $domainPackage.entity.${featureName}Entity

                interface ${featureName}Repository {
                    fun get${featureName}s(): List<${featureName}Entity>
                }
            """.trimIndent(),

            "$domainPath/usecase/${featureName}UseCase.kt" to """
                package $domainPackage.usecase

                import $domainPackage.repo.${featureName}Repository
                import javax.inject.Inject
                import $domainPackage.entity.${featureName}Entity

                class ${featureName}UseCase @Inject constructor(
                    private val ${featureNameLower}Repository: ${featureName}Repository
                ) {
                    operator fun invoke(): List<${featureName}Entity> {
                        return ${featureNameLower}Repository.get${featureName}s()
                    }
                }
            """.trimIndent(),

            // Data Layer
            "$dataPath/remote/${featureName}RemoteService.kt" to """
                package $dataPackage.remote
                
                import $dataPackage.remote.${featureName}Dto

                interface ${featureName}RemoteService {
                    fun fetch${featureName}s(): List<${featureName}Dto>
                }
            """.trimIndent(),

            "$dataPath/remote/${featureName}Dto.kt" to """
                package $dataPackage.remote

                data class ${featureName}Dto(
                    val id: Int,
                    val name: String
                )
            """.trimIndent(),

            "$dataPath/repo/${featureName}RepositoryImpl.kt" to """
                package $dataPackage.repo

                import $domainPackage.entity.${featureName}Entity
                import $domainPackage.repo.${featureName}Repository
                import $dataPackage.remote.${featureName}RemoteService
                import $dataPackage.mapper.${featureName}Mapper
                import javax.inject.Inject

                class ${featureName}RepositoryImpl @Inject constructor(
                    private val remoteService: ${featureName}RemoteService,
                    private val ${featureNameLower}Mapper: ${featureName}Mapper
                ) : ${featureName}Repository {
                    override fun get${featureName}s(): List<${featureName}Entity> {
                        return remoteService.fetch${featureName}s().map { dto ->
                            ${featureNameLower}Mapper.mapFromDto(dto)
                        }
                    }
                }
            """.trimIndent(),

            "$dataPath/mapper/${featureName}Mapper.kt" to """
                package $dataPackage.mapper

                import $domainPackage.entity.${featureName}Entity
                import $dataPackage.remote.${featureName}Dto
                import javax.inject.Inject

                class ${featureName}Mapper @Inject constructor() {
                    fun mapFromDto(dto: ${featureName}Dto): ${featureName}Entity {
                        return ${featureName}Entity(dto.id, dto.name)
                    }

                    fun mapToDto(entity: ${featureName}Entity): ${featureName}Dto {
                        return ${featureName}Dto(entity.id, entity.name)
                    }
                }
            """.trimIndent(),

            // App Layer
            "$appPath/viewmodel/${featureName}ViewModel.kt" to """
                package $appPackage.viewmodel

                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.liveData
                import $domainPackage.usecase.${featureName}UseCase
                import javax.inject.Inject

                class ${featureName}ViewModel @Inject constructor(
                    private val ${featureNameLower}UseCase: ${featureName}UseCase
                ) : ViewModel() {
                    val ${featureNameLower}s = liveData {
                        val data = ${featureNameLower}UseCase()
                        emit(data)
                    }
                }
            """.trimIndent(),

            "$appPath/di/FeatureModule.kt" to """
            package $appPackage.di

            import $domainPackage.repo.${featureName}Repository
            import $dataPackage.repo.${featureName}RepositoryImpl
            import $domainPackage.usecase.${featureName}UseCase
            import $dataPackage.mapper.${featureName}Mapper
            import $dataPackage.remote.${featureName}RemoteService
            import $dataPackage.remote.${featureName}RemoteServiceImpl
            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ViewModelComponent

            @Module
            @InstallIn(ViewModelComponent::class)
            object FeatureModule {
                @Provides
                fun provide${featureName}UseCase(
                    repository: ${featureName}Repository
                ): ${featureName}UseCase {
                    return ${featureName}UseCase(repository)
                }

                @Provides
                fun provide${featureName}Repository(
                    remoteService: ${featureName}RemoteService,
                    mapper: ${featureName}Mapper
                ): ${featureName}Repository {
                    return ${featureName}RepositoryImpl(remoteService, mapper)
                }

                @Provides
                fun provide${featureName}Mapper(): ${featureName}Mapper {
                    return ${featureName}Mapper()
                }
            }
        """.trimIndent()
        )

        for ((path, content) in filesContent) {
            val file = File(path)
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText(content)
        }
    }

    private fun refreshProjectView(
        project: Project,
        appPath: String,
        domainPath: String,
        dataPath: String,
        appDirectoryPath: String,
        dataDirectoryPath: String,
        domainDirectoryPath: String
    ) {
        LocalFileSystem.getInstance().refreshIoFiles(
            listOf(File(appPath), File(domainPath), File(dataPath), File(appDirectoryPath), File(dataDirectoryPath), File(domainDirectoryPath)),
            true,
            true,
            null
        )
    }
}
