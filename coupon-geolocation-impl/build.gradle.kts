plugins {
    id("coupon.java-conventions")
}

dependencies {
    implementation(project(":coupon-geolocation-api"))
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    runtimeOnly("com.github.ben-manes.caffeine:caffeine")
}
