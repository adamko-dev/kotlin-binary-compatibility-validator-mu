/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    //id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("dev.adamko.kotlin.binary-compatibility-validator") version "0.0.2-SNAPSHOT"
}

//repositories {
//    mavenCentral()
//}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}
