import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import io.papermc.paperweight.tasks.RebuildGitPatches
import io.papermc.paperweight.tasks.RebuildBaseGitPatches

plugins {
    java
    id("io.nvfolia.weaver.patcher") version "1.1.1-SNAPSHOT"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

paperweight {
    upstreams.register("folia") {
        repo = github("PaperMC", "Folia")
        ref = providers.gradleProperty("foliaCommit")

        patchFile {
            path = "folia-server/build.gradle.kts"
            outputFile = file("nvfolia-server/build.gradle.kts")
            patchFile = file("nvfolia-server/build.gradle.kts.patch")
        }
        patchFile {
            path = "folia-api/build.gradle.kts"
            outputFile = file("nvfolia-api/build.gradle.kts")
            patchFile = file("nvfolia-api/build.gradle.kts.patch")
        }
        patchRepo("paperApi") {
            upstreamPath = "paper-api"
            patchesDir = file("nvfolia-api/paper-patches")
            outputDir = file("paper-api")
        }
        patchDir("foliaApi") {
            upstreamPath = "folia-api"
            excludes = listOf("build.gradle.kts", "build.gradle.kts.patch", "paper-patches")
            patchesDir = file("nvfolia-api/folia-patches")
            outputDir = file("folia-api")
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(22)
        }
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = 22
        options.isFork = true
        options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
    }
    tasks.withType<Javadoc>().configureEach {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<Test>().configureEach {
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }
}

project(":nvfolia-api") {
    extensions.configure<PublishingExtension> {
        repositories {
            maven("https://central.sonatype.com/repository/maven-snapshots/") {
                name = "central"
	            credentials(PasswordCredentials::class) {
                    username = System.getenv("PUBLISH_USER")
                    password = System.getenv("PUBLISH_TOKEN")
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])

                afterEvaluate {
                    pom {
                        name.set("nvfolia-api")
                        description.set(this.description)
                        url.set("https://github.com/Craftnvfolia/nvfolia")
                        licenses {
                            license {
                                name.set("GNU Affero General Public License v3.0")
                                url.set("https://github.com/Craftnvfolia/nvfolia/blob/HEAD/LICENSE")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("nvfolia-team")
                                name.set("nvfolia Team")
                                organization.set("nvfolia")
                                organizationUrl.set("https://papermc.io")
                                roles.add("developer")
                            }
                        }
                        scm {
                            url.set("https://github.com/Craftnvfolia/nvfolia")
                        }
                    }
                }
            }
        }
    }
}

// build publication
tasks.register<Jar>("createMojmapClipboardJar") {
    dependsOn(":nvfolia-server:createMojmapPaperclipJar")
}

tasks.register("buildPublisherJar") {
    dependsOn(":createMojmapClipboardJar")

    doLast {
        val buildNumber = System.getenv("BUILD_NUMBER") ?: "local"

        val paperclipJarTask = project(":nvfolia-server").tasks.getByName("createMojmapPaperclipJar")
        val outputJar = paperclipJarTask.outputs.files.singleFile
        val outputDir = outputJar.parentFile

        if (outputJar.exists()) {
            val newJarName = "nvfolia-build.$buildNumber.jar"
            val newJarFile = File(outputDir, newJarName)

            outputDir.listFiles()
                ?.filter { it.name.startsWith("nvfolia-build.") && it.name.endsWith(".jar") }
                ?.forEach { it.delete() }
            outputJar.renameTo(newJarFile)
            println("Renamed ${outputJar.name} to $newJarName in ${outputDir.absolutePath}")
        }
    }
}

// patching scripts
tasks.register("fixupMinecraftFilePatches") {
    dependsOn(":nvfolia-server:fixupMinecraftSourcePatches")
}

allprojects {
    tasks.withType<RebuildGitPatches>().configureEach {
        filterPatches = false
    }
    tasks.withType<RebuildBaseGitPatches>().configureEach {
        filterPatches = false
    }
}
