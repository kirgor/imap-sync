plugins {
    id("org.jetbrains.kotlin.jvm").version("1.3.20")
    application
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-compiler")
    implementation("org.jetbrains.kotlin:kotlin-script-util")
    implementation("com.sun.mail:javax.mail:1.6.2")
}

application {
    mainClassName = "org.kirgor.imapsync.AppKt"
}
