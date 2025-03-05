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
