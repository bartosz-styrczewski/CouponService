plugins {
    id("coupon.spring-boot-application-conventions")
}

dependencies {
    implementation(project(":coupon-web"))
    implementation(project(":coupon-service-impl"))
    implementation(project(":coupon-persistence"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    integrationTestImplementation("org.springframework:spring-jdbc")
    integrationTestImplementation("org.springframework.boot:spring-boot-testcontainers")
    integrationTestImplementation("org.testcontainers:testcontainers-postgresql")
}
