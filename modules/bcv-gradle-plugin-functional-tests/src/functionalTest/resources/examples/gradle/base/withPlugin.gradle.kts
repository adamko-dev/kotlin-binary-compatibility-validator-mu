/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

plugins {
    kotlin("jvm") version "1.7.20"
    //id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("dev.adamko.kotlin.binary-compatibility-validator") version "0.0.1-SNAPSHOT"
}

//repositories {
//    mavenCentral()
//}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}
