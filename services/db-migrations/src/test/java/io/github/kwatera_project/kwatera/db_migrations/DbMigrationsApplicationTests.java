package io.github.kwatera_project.kwatera.db_migrations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
    })
class DbMigrationsApplicationTests {

  @Test
  void contextLoads() {}
}
