package br.com.mc_manager.server.service.runtime.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.WaitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DockerProcessLifecycle")
class DockerProcessLifecycleTest {

  private static final String CONTAINER_ID = "test-container-abc123";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DockerClient dockerClient;

  @Mock
  private Runnable exitCallback;

  private DockerProcessLifecycle lifecycle;

  @BeforeEach
  void setUp() {
    lifecycle = new DockerProcessLifecycle(dockerClient, () -> CONTAINER_ID);
  }

  private void givenRunning(Boolean running) {
    when(dockerClient.inspectContainerCmd(CONTAINER_ID).exec().getState().getRunning())
            .thenReturn(running);
  }

  @Nested
  @DisplayName("start()")
  class StartTest {

    @Test
    @DisplayName("inicia o container e registra o watcher de saída")
    void deveIniciarContainerERegistrarWatcher() {
      lifecycle.start();

      verify(dockerClient).startContainerCmd(CONTAINER_ID);
      verify(dockerClient).waitContainerCmd(CONTAINER_ID);
      assertThat(lifecycle.getContainerId()).isEqualTo(CONTAINER_ID);
    }

    @Test
    @DisplayName("remove o container e relança a exceção quando o start falha")
    void deveRemoverContainerQuandoStartFalha() {
      when(dockerClient.startContainerCmd(CONTAINER_ID).exec()).thenThrow(new RuntimeException("boom"));

      assertThatThrownBy(() -> lifecycle.start())
              .isInstanceOf(RuntimeException.class)
              .hasMessage("boom");

      verify(dockerClient).removeContainerCmd(CONTAINER_ID);
      verify(dockerClient, never()).waitContainerCmd(anyString());
    }

    @Test
    @DisplayName("gera um id novo a cada start")
    void deveGerarIdNovoACadaStart() {
      var ids = List.of("id-1", "id-2").iterator();
      DockerProcessLifecycle multi = new DockerProcessLifecycle(dockerClient, ids::next);

      multi.start();
      assertThat(multi.getContainerId()).isEqualTo("id-1");

      multi.start();
      assertThat(multi.getContainerId()).isEqualTo("id-2");
    }
  }

  @Nested
  @DisplayName("isRunning()")
  class IsRunningTest {

    @Test
    @DisplayName("retorna false antes do start (sem id)")
    void deveRetornarFalseAntesDoStart() {
      assertThat(lifecycle.isRunning()).isFalse();
      verify(dockerClient, never()).inspectContainerCmd(anyString());
    }

    @Test
    @DisplayName("retorna true quando o container está rodando")
    void deveRetornarTrueQuandoRodando() {
      lifecycle.start();
      givenRunning(Boolean.TRUE);

      assertThat(lifecycle.isRunning()).isTrue();
    }

    @Test
    @DisplayName("retorna false quando o container está parado")
    void deveRetornarFalseQuandoParado() {
      lifecycle.start();
      givenRunning(Boolean.FALSE);

      assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    @DisplayName("retorna false quando o estado running é nulo")
    void deveRetornarFalseQuandoRunningNulo() {
      lifecycle.start();
      givenRunning(null);

      assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    @DisplayName("retorna false quando o container não existe mais")
    void deveRetornarFalseQuandoNotFound() {
      lifecycle.start();
      when(dockerClient.inspectContainerCmd(CONTAINER_ID).exec())
              .thenThrow(new NotFoundException("sem container"));

      assertThat(lifecycle.isRunning()).isFalse();
    }
  }

  @Nested
  @DisplayName("stop()")
  class StopTest {

    @Test
    @DisplayName("não faz nada antes do start")
    void deveIgnorarAntesDoStart() {
      lifecycle.stop(10);

      verify(dockerClient, never()).stopContainerCmd(anyString());
      verify(dockerClient, never()).removeContainerCmd(anyString());
    }

    @Test
    @DisplayName("para com o timeout informado e remove o container")
    void devePararERemoverQuandoRodando() {
      lifecycle.start();
      givenRunning(Boolean.TRUE);

      lifecycle.stop(30);

      verify(dockerClient.stopContainerCmd(CONTAINER_ID)).withTimeout(30);
      verify(dockerClient.removeContainerCmd(CONTAINER_ID)).withForce(true);
    }

    @Test
    @DisplayName("propaga timeout zero")
    void devePropagarTimeoutZero() {
      lifecycle.start();
      givenRunning(Boolean.TRUE);

      lifecycle.stop(0);

      verify(dockerClient.stopContainerCmd(CONTAINER_ID)).withTimeout(0);
    }

    @Test
    @DisplayName("não chama stop com container parado, mas ainda remove")
    void deveApenasRemoverQuandoParado() {
      lifecycle.start();
      givenRunning(Boolean.FALSE);

      lifecycle.stop(10);

      verify(dockerClient, never()).stopContainerCmd(anyString());
      verify(dockerClient.removeContainerCmd(CONTAINER_ID)).withForce(true);
    }
  }

  @Nested
  @DisplayName("cleanup()")
  class CleanupTest {

    @Test
    @DisplayName("remove o container com force")
    void deveRemoverContainer() {
      lifecycle.start();

      lifecycle.cleanup();

      verify(dockerClient.removeContainerCmd(CONTAINER_ID)).withForce(true);
    }

    @Test
    @DisplayName("ignora NotFoundException")
    void deveIgnorarNotFound() {
      lifecycle.start();
      when(dockerClient.removeContainerCmd(CONTAINER_ID).withForce(true).exec())
              .thenThrow(new NotFoundException("já removido"));

      lifecycle.cleanup(); // não deve lançar
    }

    @Test
    @DisplayName("não faz nada antes do start")
    void deveIgnorarAntesDoStart() {
      lifecycle.cleanup();

      verify(dockerClient, never()).removeContainerCmd(anyString());
    }
  }

  @Nested
  @DisplayName("onExit()")
  class OnExitTest {

    @SuppressWarnings("unchecked")
    private ResultCallback<WaitResponse> captureWatcher() {
      ArgumentCaptor<ResultCallback<WaitResponse>> captor = ArgumentCaptor.forClass(ResultCallback.class);
      verify(dockerClient.waitContainerCmd(CONTAINER_ID)).exec(captor.capture());
      return captor.getValue();
    }

    @Test
    @DisplayName("executa o callback registrado quando o container sai")
    void deveExecutarCallback() {
      lifecycle.onExit(exitCallback);
      lifecycle.start();

      captureWatcher().onNext(new WaitResponse());

      verify(exitCallback).run();
    }

    @Test
    @DisplayName("não lança quando nenhum callback foi registrado")
    void deveNaoLancarSemCallback() {
      lifecycle.start();

      captureWatcher().onNext(new WaitResponse());

      verifyNoInteractions(exitCallback);
    }
  }

  @Nested
  @DisplayName("getContainerId()")
  class GetContainerIdTest {

    @Test
    @DisplayName("retorna null antes do start")
    void deveRetornarNullAntesDoStart() {
      assertThat(lifecycle.getContainerId()).isNull();
    }

    @Test
    @DisplayName("retorna o id depois do start")
    void deveRetornarIdDepoisDoStart() {
      lifecycle.start();

      assertThat(lifecycle.getContainerId()).isEqualTo(CONTAINER_ID);
    }
  }
}