plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.imjustdoom.libwork"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.esotericsoftware:kryo:5.6.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<Jar> {
    manifest {
//        attributes["Main-Class"] = "com.example.MainKt" // Your main class
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}

tasks.test {
    useJUnitPlatform()
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}