package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.ManagedServerProcess;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ExecutorService;

import static br.com.mc_manager.server.factory.ServerFactory.createServerWithAllAttributes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("DockerManagedServerProcessFactory")
class DockerManagedServerProcessFactoryTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DockerClient dockerClient;

  @Mock
  private SimpMessagingTemplate template;

  @Mock
  private ExecutorService executor;

  @Mock
  private PlayerService playerService;

  @Mock
  private Clock clock;

  @Mock
  private CreateContainerResponse containerResponse;

  private DockerManagedServerProcessFactory factory;

  @BeforeEach
  void setUp() {
    factory = new DockerManagedServerProcessFactory(
            dockerClient,
            template,
            executor,
            playerService,
            clock,
            "./host/target/test"
    );
  }

  @Nested
  @DisplayName("Validação de Domínio")
  class ValidacaoDeDominioTests {

    @Test
    @DisplayName("lança IllegalStateException quando domain é nulo")
    void deveLancarExcecaoQuandoDomainENulo() {
      // Arrange
      Server server = mock(Server.class);
      given(server.getId()).willReturn(1L);
      given(server.getDomain()).willReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> factory.create(server, "/usr/bin/java", Path.of("/data")))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("1");
    }

    @Test
    @DisplayName("lança IllegalStateException quando domain é vazio")
    void deveLancarExcecaoQuandoDomainEhVazio() {
      // Arrange
      Server server = mock(Server.class);
      given(server.getId()).willReturn(1L);
      given(server.getDomain()).willReturn("   ");

      // Act & Assert
      assertThatThrownBy(() -> factory.create(server, "/usr/bin/java", Path.of("/data")))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("1");
    }
  }

  @Nested
  @DisplayName("Criação do Container")
  class CriacaoDoContainerTests {

    private Server server;

    @BeforeEach
    void setUp() {
      server = createServerWithAllAttributes();

      given(dockerClient.createContainerCmd("eclipse-temurin:21-jre")
              .withName("mc-1")
              .withHostConfig(any())
              .withLabels(any())
              .withWorkingDir("/data")
              .withAttachStdin(true)
              .withStdinOpen(true)
              .withCmd("Java", "-Xms512M", "-Xmx1G", "-jar", "server.jar", "nogui")
              .exec())
              .willReturn(containerResponse);
    }

    @Test
    @DisplayName("retorna DockerManagedServerProcess com sucesso")
    void deveRetornarProcessoComSucesso() {
      // Act
      ManagedServerProcess result = factory.create(server, "/usr/bin/java", Path.of("/data"));

      // Assert
      assertThat(result).isInstanceOf(DockerManagedServerProcess.class);
    }

    @Test
    @DisplayName("usa imagem correta quando javaVersion tem prefixo JAVA_")
    void deveUsarImagemCorretaQuandoJavaVersionTemPrefixo() {
      // Act
      ManagedServerProcess result = factory.create(server, "/usr/bin/java", Path.of("/data"));

      // Assert
      assertThat(result).isInstanceOf(DockerManagedServerProcess.class);
    }

    @Test
    @DisplayName("usa imagem correta quando javaVersion já vem sem prefixo")
    void deveUsarImagemCorretaQuandoJavaVersionSemPrefixo() {
      // Act
      ManagedServerProcess result = factory.create(server, "/usr/bin/java", Path.of("/data"));

      // Assert
      assertThat(result).isInstanceOf(DockerManagedServerProcess.class);
    }

    @Test
    @DisplayName("define nome do container com o id do servidor")
    void deveDefinirNomeDoContainerComIdDoServidor() {
      // Act
      ManagedServerProcess result = factory.create(server, "/usr/bin/java", Path.of("/data"));

      // Assert
      assertThat(result).isInstanceOf(DockerManagedServerProcess.class);
    }

    @Test
    @DisplayName("define labels de infraestrutura corretamente")
    void deveDefinirLabelsDeInfraestrutura() {
      // Act
      ManagedServerProcess result = factory.create(server, "/usr/bin/java", Path.of("/data"));

      // Assert
      assertThat(result).isInstanceOf(DockerManagedServerProcess.class);
    }

    @Test
    @DisplayName("define comando com flags de RAM corretamente")
    void deveDefinirComandoComFlagsDeRam() {
      // Act
      ManagedServerProcess result = factory.create(server, "/usr/bin/java", Path.of("/data"));

      // Assert
      assertThat(result).isInstanceOf(DockerManagedServerProcess.class);
    }

    @Test
    @DisplayName("define workingDir, attachStdin e stdinOpen corretamente")
    void deveDefinirWorkingDirEAttachStdin() {
      // Act
      ManagedServerProcess result = factory.create(server, "/usr/bin/java", Path.of("/data"));

      // Assert
      assertThat(result).isInstanceOf(DockerManagedServerProcess.class);
    }
  }
}