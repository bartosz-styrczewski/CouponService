import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("coupon.spring-boot-application-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    integrationTestImplementation("org.springframework.boot:spring-boot-testcontainers")
    integrationTestImplementation("org.testcontainers:testcontainers-postgresql")
}

tasks.named<BootRun>("bootTestRun") {
    description = "Runs the application against a PostgreSQL container started by Testcontainers."
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mainClass = "info.interview.couponservice.TestCouponServiceApplication"
}
