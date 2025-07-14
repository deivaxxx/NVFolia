import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import io.papermc.paperweight.tasks.RebuildGitPatches
import io.papermc.paperweight.tasks.RebuildBaseGitPatches

plugins {
    java
    id("io.canvasmc.weaver.patcher") version "2.1.3-SNAPSHOT"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

paperweight {
    upstreams.register("folia") {
        repo = github("PaperMC", "Folia")
        ref = providers.gradleProperty("foliaCommit")

        patchFile {
            path = "folia-server/build.gradle.kts"
            outputFile = file("canvas-server/build.gradle.kts")
            patchFile = file("canvas-server/build.gradle.kts.patch")
        }
        patchFile {
            path = "folia-api/build.gradle.kts"
            outputFile = file("canvas-api/build.gradle.kts")
            patchFile = file("canvas-api/build.gradle.kts.patch")
        }
        patchRepo("paperApi") {
            upstreamPath = "paper-api"
            patchesDir = file("canvas-api/paper-patches")
            outputDir = file("paper-api")
        }
        patchDir("foliaApi") {
            upstreamPath = "folia-api"
            excludes = listOf("build.gradle.kts", "build.gradle.kts.patch", "paper-patches")
            patchesDir = file("canvas-api/folia-patches")
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

project(":canvas-api") {
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
                        name.set("canvas-api")
                        description.set(this.description)
                        url.set("https://github.com/CraftCanvasMC/Canvas")
                        licenses {
                            license {
                                name.set("GNU Affero General Public License v3.0")
                                url.set("https://github.com/CraftCanvasMC/Canvas/blob/HEAD/LICENSE")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("canvas-team")
                                name.set("Canvas Team")
                                organization.set("CanvasMC")
                                organizationUrl.set("https://canvasmc.io")
                                roles.add("developer")
                            }
                        }
                        scm {
                            url.set("https://github.com/CraftCanvasMC/Canvas")
                        }
                    }
                }
            }
        }
    }
}

// build publication
tasks.register<Jar>("createMojmapClipboardJar") {
    dependsOn(":canvas-server:createMojmapPaperclipJar")
}

tasks.register("buildPublisherJar") {
    dependsOn(":createMojmapClipboardJar")

    doLast {
        val buildNumber = System.getenv("BUILD_NUMBER") ?: "local"

        val paperclipJarTask = project(":canvas-server").tasks.getByName("createMojmapPaperclipJar")
        val outputJar = paperclipJarTask.outputs.files.singleFile
        val outputDir = outputJar.parentFile

        if (outputJar.exists()) {
            val newJarName = "canvas-build.$buildNumber.jar"
            val newJarFile = File(outputDir, newJarName)

            outputDir.listFiles()
                ?.filter { it.name.startsWith("canvas-build.") && it.name.endsWith(".jar") }
                ?.forEach { it.delete() }
            outputJar.renameTo(newJarFile)
            println("Renamed ${outputJar.name} to $newJarName in ${outputDir.absolutePath}")
        }
    }
}

// patching scripts
tasks.register("fixupMinecraftFilePatches") {
    dependsOn(":canvas-server:fixupMinecraftSourcePatches")
}

allprojects {
    tasks.withType<RebuildGitPatches>().configureEach {
        filterPatches = false
    }
    tasks.withType<RebuildBaseGitPatches>().configureEach {
        filterPatches = false
    }
}
