plugins {
    id("coupon.spring-boot-application-conventions")
    `jacoco-report-aggregation`
    `test-report-aggregation`
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

tasks.testCodeCoverageReport {
    dependsOn(tasks.integrationTest)
    executionData(tasks.integrationTest.get())
}

tasks.testAggregateTestReport {
    testResults.from(tasks.integrationTest.flatMap { it.binaryResultsDirectory })
}

tasks.test {
    finalizedBy(tasks.testAggregateTestReport)
}

tasks.register("codeCoverageReport") {
    group = "verification"
    description = "Runs all tests and creates the test report and the code coverage report for the whole project."
    dependsOn(tasks.testAggregateTestReport, tasks.testCodeCoverageReport)
}
