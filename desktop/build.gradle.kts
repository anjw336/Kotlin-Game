plugins {
    kotlin("jvm") version "1.9.22"
    application
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-desktop")
}

application {
    mainClass.set("com.oop.game.desktop.DesktopLauncherKt")

    // macOS에서 LWJGL3 실행 시 필요
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}
