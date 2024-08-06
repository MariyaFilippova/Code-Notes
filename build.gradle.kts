import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.0.0"
  id("org.jetbrains.intellij.platform") version "2.0.0-rc1"
}

// Configure project's dependencies
repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

group = "org.me"
version = "1.0.0"

dependencies {
  intellijPlatform {
    create("IU", "242.20224-EAP-CANDIDATE-SNAPSHOT", useInstaller = false)
    testFramework(TestFrameworkType.Plugin.Java)
  }
  testImplementation("junit:junit:4.13.2")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")

  implementation("com.slack.api:bolt-jetty:1.40.3")
  implementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
}

intellijPlatform {
  instrumentCode.set(false)
}

tasks {
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("241")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }

  test {
    useJUnitPlatform()
  }
}
