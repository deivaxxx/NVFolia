import java.util.Locale

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

if (!file(".git").exists()) {
    val errorText = """
        
        =====================[ ERROR ]=====================
         The EtheriumMC project directory is not a properly cloned Git repository.
         
         In order to build EtheriumMC from source you must clone
         the EtheriumMC repository using Git, not download a code
         zip from GitHub.
         
         Built EtheriumMC jars are available for download at
         https://github.com/deivaxxx/EtheriumMC/ or 
         at https://github.com/deivaxxx/EtheriumMC/
         
         See https://bxteam.org/docs/divinemc/development/contributing
         for further information on building and modifying EtheriumMC.
        ===================================================
    """.trimIndent()
    error(errorText)
}

rootProject.name = "EtheriumMC"

for (name in listOf("etheriumMC-api", "nvfolia-server", "etheriumMC-checkstyle")) {
    val projName = name.lowercase(Locale.ENGLISH)
    include(projName)
    findProject(":$projName")!!.projectDir = file(name)
}

gradle.lifecycle.beforeProject {
    val mcVersion = providers.gradleProperty("mcVersion").get().trim()
    val divinemcChannel = providers.gradleProperty("channel").get().trim()
    val divinemcBuildNumber = providers.environmentVariable("BUILD_NUMBER").orNull?.trim()?.toInt()
    val versionString = if (divinemcBuildNumber == null) {
        "$mcVersion.local-SNAPSHOT"
    } else {
        "$mcVersion.build.$divinemcBuildNumber-${divinemcChannel.lowercase()}"
    }
    version = versionString
}
