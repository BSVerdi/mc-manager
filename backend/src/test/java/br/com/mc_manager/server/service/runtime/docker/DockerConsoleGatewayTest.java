package br.com.mc_manager.server.service.runtime.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.*;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;

@ExtendWith(MockitoExtension.class)
class DockerConsoleGatewayTest {

  private static final String CONTAINER_ID = "test-container";
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DockerClient dockerClient;
  private DockerConsoleGateway gateway;

  @BeforeEach
  void setUp() {
    gateway = new DockerConsoleGateway(dockerClient, CONTAINER_ID);
  }

  @Nested
  @DisplayName("attach(Consumer<String>)")
  class AttachTests {

    @Test
    @DisplayName("anexa com sucesso e prepara estado para write")
    void deveAnexarComSucesso() {
      // Arrange
      List<String> received = new ArrayList<>();

      // Act
      gateway.attach(received::add);

      // Assert
      assertThat(getField(gateway, "stdinPipe")).isNotNull();
      assertThat(getField(gateway, "logStream")).isNotNull();
      assertThat(getField(gateway, "attachStream")).isNotNull();

      assertThatCode(() -> gateway.write("test")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("lança UncheckedIOException quando attach do stdin falha")
    void deveLancarExcecaoQuandoAttachStdinFalha() {
      // Arrange
      given(dockerClient.attachContainerCmd(CONTAINER_ID)
              .withStdIn(any())
              .withStdOut(false)
              .withStdErr(false)
              .withFollowStream(true)
              .exec(any()))
              .willThrow(new UncheckedIOException("Falha ao anexar stdion do container " + CONTAINER_ID, new IOException()));

      // Act & Assert
      assertThatThrownBy(() -> gateway.attach(ignored -> {
      }))
              .isInstanceOf(UncheckedIOException.class)
              .hasMessageContaining("Falha ao anexar stdion do container " + CONTAINER_ID);
    }

    @Test
    @DisplayName("lança IOException quando attach de logs falha")
    void deveLancarExcecaoQuandoAttachLogsFalha() {
      // Arrange
      when(dockerClient.logContainerCmd(CONTAINER_ID)
              .withStdOut(true)
              .withStdErr(true)
              .withFollowStream(true)
              .withTail(0)
              .exec(any()))
              .thenThrow(new UncheckedIOException("falha simulada nos logs", new IOException()));

      // Act & Assert
      assertThatThrownBy(() -> gateway.attach(ignored -> {
      }))
              .isInstanceOf(UncheckedIOException.class)
              .hasMessage("falha simulada nos logs");
    }
  }

  @Nested
  @DisplayName("write(String)")
  class WriteTests {

    @Test
    @DisplayName("lança IllegalStateException quando console não está anexado")
    void deveLancarExcecaoQuandoNaoAnexado() {
      // Act & Assert
      assertThatThrownBy(() -> gateway.write("any command"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Console não está anexado");
    }

    @Test
    @DisplayName("escreve comando no pipe com sucesso após attach")
    void deveEscreverComandoNoPipe() {
      // Arrange
      List<String> received = new ArrayList<>();
      gateway.attach(received::add);

      // Act
      gateway.write("say hello");

      // Assert — pipe permanece aberto e funcional
      assertThat(getField(gateway, "stdinPipe")).isNotNull();
    }

    @Test
    @DisplayName("lança UncheckedIOException quando pipe está fechado")
    void deveLancarExcecaoQuandoPipeFechado() throws IOException {
      // Arrange
      List<String> received = new ArrayList<>();
      gateway.attach(received::add);

      // Fecha o pipe via reflection para simular erro de I/O
      var pipe = (PipedOutputStream) getField(gateway, "stdinPipe");
      pipe.close();

      // Act & Assert
      assertThatThrownBy(() -> gateway.write("cmd"))
              .isInstanceOf(UncheckedIOException.class)
              .hasMessage("Erro ao enviar comando ao container");
    }
  }

  @Nested
  @DisplayName("detach()")
  class DetachTests {

    @Test
    @DisplayName("fecha todos os streams e nula os campos após attach")
    void deveFecharStreamsApósAttach() {
      // Arrange
      List<String> received = new ArrayList<>();
      gateway.attach(received::add);

      // Act
      gateway.detach();

      // Assert — todos os campos nulos
      assertThat(getField(gateway, "logStream")).isNull();
      assertThat(getField(gateway, "attachStream")).isNull();
      assertThat(getField(gateway, "stdinPipe")).isNull();

      // Efeito colateral: write() agora falha (pipe null)
      assertThatThrownBy(() -> gateway.write("cmd"))
              .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("não lança exceção quando chamado sem attach prévio")
    void deveNaoLancarExcecaoQuandoNaoAnexado() {
      // Act & Assert — closeQuietly ignora nulls
      assertThatCode(() -> gateway.detach()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ignora IOException em close e continua o cleanup")
    void deveIgnorarExcecaoEmClose() throws IOException {
      // Arrange

      @SuppressWarnings("unchecked")
      ResultCallback<Frame> failingCallback = mock(ResultCallback.class);
      doThrow(new IOException("falha no close", new IOException())).when(failingCallback).close();

      given(dockerClient.logContainerCmd(CONTAINER_ID)
              .withStdOut(true)
              .withStdErr(true)
              .withFollowStream(true)
              .withTail(0)
              .exec(any()))
              .willReturn(failingCallback);


      List<String> received = new ArrayList<>();
      gateway.attach(received::add);

      // Act
      gateway.detach();

      // Assert
      assertThat(getField(gateway, "logStream")).isNull();
      assertThat(getField(gateway, "attachStream")).isNull();
      assertThat(getField(gateway, "stdinPipe")).isNull();
    }
  }
}