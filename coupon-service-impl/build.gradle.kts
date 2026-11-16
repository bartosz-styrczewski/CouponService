plugins {
    id("coupon.java-conventions")
}

dependencies {
    implementation(project(":coupon-service-api"))
    implementation(project(":coupon-persistence"))
}
