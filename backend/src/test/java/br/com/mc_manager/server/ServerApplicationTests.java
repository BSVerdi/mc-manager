package br.com.mc_manager.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "mcmanager.data_root_directory=./target/test-data")
class ServerApplicationTests {

  @Test
  void contextLoads() {
  }

}
