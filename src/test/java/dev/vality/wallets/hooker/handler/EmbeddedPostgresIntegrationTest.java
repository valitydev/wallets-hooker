package dev.vality.wallets.hooker.handler;

import dev.vality.wallets.hooker.WalletsHookerApplication;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.boot.test.util.TestPropertyValues.Type.MAP;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = {WalletsHookerApplication.class},
        initializers = EmbeddedPostgresIntegrationTest.Initializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class EmbeddedPostgresIntegrationTest {

    private static final int PORT = 15432;
    private static final String dbName = "wallets_hooker";
    private static final String dbUser = "postgres";
    private static final String dbPassword = "postgres";
    private static final String jdbcUrl = "jdbc:postgresql://localhost:" + PORT + "/" + dbName;

    private static EmbeddedPostgres postgres;

    private static void startPgServer() {
        try {
            log.info("The PG server is starting...");
            EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
            String dbDir = prepareDbDir();
            log.info("Dir for PG files: " + dbDir);
            builder.setDataDirectory(dbDir);
            builder.setPort(PORT);
            postgres = builder.start();
            log.info("The PG server was started!");
        } catch (IOException e) {
            log.error("An error occurred while starting server ", e);
            e.printStackTrace();
        }
    }

    private static void createDatabase() {
        try (Connection conn = postgres.getPostgresDatabase().getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("CREATE DATABASE " + dbName);
            statement.close();
        } catch (SQLException e) {
            log.error("An error occurred while creating the database " + dbName, e);
            e.printStackTrace();
        }
    }

    private static String prepareDbDir() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentDate = dateFormat.format(new Date());
        String dir = "target" + File.separator + "pgdata_" + currentDate;
        log.info("Postgres source files in {}", dir);
        return dir;
    }

    @AfterEach
    public void destroy() throws IOException {
        if (postgres != null) {
            postgres.close();
            postgres = null;
        }
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of("spring.datasource.url=" + jdbcUrl,
                    "spring.datasource.username=" + dbUser,
                    "spring.datasource.password=" + dbPassword,
                    "flyway.url=" + jdbcUrl,
                    "flyway.user=" + dbUser,
                    "flyway.password=" + dbPassword)
                    .applyTo(configurableApplicationContext.getEnvironment(), MAP, "testcontainers");

            if (postgres == null) {
                startPgServer();
                createDatabase();
            }
        }
    }

}