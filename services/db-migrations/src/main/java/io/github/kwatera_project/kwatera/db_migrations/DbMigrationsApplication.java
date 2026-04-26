package io.github.kwatera_project.kwatera.db_migrations;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DbMigrationsApplication {

  public static void main(String[] args) {
    SpringApplication.run(DbMigrationsApplication.class, args);
  }

  @Bean
  @org.springframework.context.annotation.Profile("!test")
  public Flyway flyway(DataSource dataSource) {
    Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();
    flyway.migrate();
    return flyway;
  }
}
