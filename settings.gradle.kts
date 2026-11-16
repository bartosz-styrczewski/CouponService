pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "coupon-service"

include(
    "coupon-app",
    "coupon-web",
    "coupon-service-api",
    "coupon-service-impl",
    "coupon-persistence",
)
