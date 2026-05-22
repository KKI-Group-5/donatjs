plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "DonatJS"
val seleniumJavaVersion = "4.14.1"
val seleniumJupiterVersion = "5.0.1"
val webdrivermanagerVersion = "5.6.3"
val junitJupiterVersion = "5.9.1"
val serenityVersion = "4.2.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    implementation("me.paulschwarz:spring-dotenv:4.0.0")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.seleniumhq.selenium:selenium-java:$seleniumJavaVersion")
    testImplementation("io.github.bonigarcia:selenium-jupiter:$seleniumJupiterVersion")
    testImplementation("io.github.bonigarcia:webdrivermanager:$webdrivermanagerVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:${junitJupiterVersion}")
    testImplementation("net.serenity-bdd:serenity-core:$serenityVersion")
    testImplementation("net.serenity-bdd:serenity-junit5:$serenityVersion")
    testImplementation("net.serenity-bdd:serenity-rest-assured:$serenityVersion")
    testImplementation("com.h2database:h2")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Global JUnit configuration for all test tasks
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Standard "test" task configured strictly for Unit Testing
tasks.test {
    description = "Runs unit tests."
    group = "verification"

    filter {
        // Exclude Integration and Functional tests to keep unit tests fast
        excludeTestsMatching("*IntegrationTest")
        excludeTestsMatching("*FunctionalTest")
    }
    // Ensures coverage is calculated immediately after unit tests
    finalizedBy(tasks.jacocoTestReport)
}

// Task for Integration Testing (as called by CI/CD)
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    filter {
        includeTestsMatching("*IntegrationTest")
    }
}

// Task for Functional/Selenium Testing (as called by CI/CD)
tasks.register<Test>("functionalTest") {
    description = "Runs functional tests."
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    filter {
        // Corrected spelling from "Funcional" to "Functional"
        includeTestsMatching("*FunctionalTest")
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.bootRun {
    environment(env.allVariables())
    // Pass -Pprofile to capture a JFR recording for profiling analysis:
    //   ./gradlew bootRun -Pprofile
    // Output: build/donatjs-profile.jfr — open with JDK Mission Control.
    if (project.hasProperty("profile")) {
        jvmArgs("-XX:StartFlightRecording=duration=120s,filename=build/donatjs-profile.jfr,settings=profile,name=donatjs")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    // Exclude boilerplate and model code from coverage reports
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude(
                "id/ac/ui/cs/advprog/donatjs/DonatJsApplication*",
                "id/ac/ui/cs/advprog/donatjs/dto/**",
                "id/ac/ui/cs/advprog/donatjs/model/**"
            )
        }
    }))
}