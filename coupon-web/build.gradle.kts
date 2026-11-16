plugins {
    id("coupon.java-conventions")
}

dependencies {
    implementation(project(":coupon-service-api"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
