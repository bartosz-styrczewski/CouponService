FROM amazoncorretto:25-alpine-jdk AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew :coupon-app:bootJar --no-daemon

FROM amazoncorretto:25-alpine-jdk
RUN adduser -S app
WORKDIR /app
COPY --from=build /workspace/coupon-app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
