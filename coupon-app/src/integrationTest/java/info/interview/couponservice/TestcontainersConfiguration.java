package info.interview.couponservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Starts PostgreSQL with the same init script as Docker Compose, so the application connects
 * as the dedicated {@code empik} user, not as the superuser.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    private static final String DATABASE = "empik";
    private static final String USERNAME = "empik";
    private static final String PASSWORD = "empik";

    @Bean
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../docker/postgres/create-app-db.sh"),
                        "/docker-entrypoint-initdb.d/create-app-db.sh")
                .withEnv("APP_DB_NAME", DATABASE)
                .withEnv("APP_DB_USER", USERNAME)
                .withEnv("APP_DB_PASSWORD", PASSWORD);
    }

    @Bean
    DynamicPropertyRegistrar databaseProperties(PostgreSQLContainer postgres) {
        return registry -> {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/%s".formatted(
                    postgres.getHost(), postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), DATABASE));
            registry.add("spring.datasource.username", () -> USERNAME);
            registry.add("spring.datasource.password", () -> PASSWORD);
        };
    }
}
