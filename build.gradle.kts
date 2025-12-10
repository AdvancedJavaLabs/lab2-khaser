plugins {
    // kotlin("jvm") version "1.9.20"
    application
}

group = "org.itmo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.rabbitmq:amqp-client:5.20.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("Main")
}
