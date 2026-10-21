package br.com.mc_manager.server.service.runtime.local;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.ManagedServerProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import oshi.SystemInfo;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;

import static br.com.mc_manager.server.factory.ServerFactory.createServerWithAllAttributes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalManagedServerProcessFactory")
class LocalManagedServerProcessFactoryTest {

  private static final Clock FIXED_CLOCK =
          Clock.fixed(Instant.parse("2025-01-15T10:30:00Z"), ZoneOffset.UTC);

  private Server server;
  
  @Mock
  private SimpMessagingTemplate template;

  @Mock
  private ExecutorService executor;

  @Mock
  private PlayerService playerService;

  @Mock
  private SystemInfo systemInfo;

  @Mock
  private LocalServerProcessFactory processFactory;

  @InjectMocks
  private LocalManagedServerProcessFactory factory;

  @BeforeEach
  void setUp() {
    server = createServerWithAllAttributes();
  }

  @Nested
  @DisplayName("create")
  class Create {

    private static Object readField(Object target, String name) throws Exception {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(target);
    }

    @Test
    @DisplayName("deveCriarUmProcessoLocalDeServidorQuandoReceberConfigValida")
    void deveCriarUmProcessoLocalDeServidorQuandoReceberConfigValida() {
      // Arrange
      String javaExecutablePath = "/opt/java/bin/java";
      Path directory = Path.of("/tmp/server-dir");

      // Act
      ManagedServerProcess result = factory.create(server, javaExecutablePath, directory);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isInstanceOf(LocalManagedServerProcess.class);
    }

    @Test
    @DisplayName("deveRepassarTodasAsDependenciasInjetadasAoProcessoCriado")
    void deveRepassarTodasAsDependenciasInjetadasAoProcessoCriado() throws Exception {
      // Arrange
      String javaExecutablePath = "/opt/java/bin/java";
      Path directory = Path.of("/tmp/server-dir");

      // Act
      LocalManagedServerProcess result =
              (LocalManagedServerProcess) factory.create(server, javaExecutablePath, directory);

      // Assert
      assertThat(readField(result, "systemInfo")).isSameAs(systemInfo);
      assertThat(readField(result, "processFactory")).isSameAs(processFactory);
    }

    @Test
    @DisplayName("deveRepassarConfigCaminhoDoJavaEDiretorioAoProcessoCriado")
    void deveRepassarConfigCaminhoDoJavaEDiretorioAoProcessoCriado() throws Exception {
      // Arrange
      String javaExecutablePath = "/opt/java/bin/java";
      Path directory = Path.of("/tmp/server-dir");

      // Act
      LocalManagedServerProcess result =
              (LocalManagedServerProcess) factory.create(server, javaExecutablePath, directory);

      // Assert
      assertThat(readField(result, "javaExecutablePath")).isEqualTo(javaExecutablePath);
      assertThat(readField(result, "serverDirectory")).isSameAs(directory);
    }

    @Test
    @DisplayName("devePropagarNPEQuandoConfigForNulo")
    void devePropagarNPEQuandoConfigForNulo() {
      // Arrange
      String javaExecutablePath = "/opt/java/bin/java";
      Path directory = Path.of("/tmp/server-dir");

      // Act / Assert
      assertThatThrownBy(() -> factory.create(null, javaExecutablePath, directory))
              .isInstanceOf(NullPointerException.class);
    }
  }
}