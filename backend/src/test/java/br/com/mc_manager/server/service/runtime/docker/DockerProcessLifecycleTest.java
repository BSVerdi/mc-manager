package br.com.mc_manager.server.service.runtime.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    lifecycle = new DockerProcessLifecycle(dockerClient, CONTAINER_ID);
  }

  @Nested
  @DisplayName("start()")
  class StartTest {

    @Test
    @DisplayName("deve iniciar o contêiner e registrar callback de saída")
    void deveIniciarConteinerERegistrarCallbackDeSaida() {
      lifecycle.start();

      verify(dockerClient).startContainerCmd(CONTAINER_ID);
      verify(dockerClient).waitContainerCmd(CONTAINER_ID);
    }
  }

  @Nested
  @DisplayName("isRunning()")
  class IsRunningTest {

    @Test
    @DisplayName("deve retornar true quando o contêiner está rodando")
    void deveRetornarTrueQuandoConteinerRodando() {
      when(dockerClient.inspectContainerCmd(CONTAINER_ID).exec().getState().getRunning())
              .thenReturn(Boolean.TRUE);

      boolean result = lifecycle.isRunning();

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("deve retornar false quando o contêiner está parado")
    void deveRetornarFalseQuandoConteinerParado() {
      when(dockerClient.inspectContainerCmd(CONTAINER_ID).exec().getState().getRunning())
              .thenReturn(Boolean.FALSE);

      boolean result = lifecycle.isRunning();

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("deve retornar false quando o estado running for nulo")
    void deveRetornarFalseQuandoEstadoRunningNulo() {
      when(dockerClient.inspectContainerCmd(CONTAINER_ID).exec().getState().getRunning())
              .thenReturn(null);

      boolean result = lifecycle.isRunning();

      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("stop()")
  class StopTest {

    @Test
    @DisplayName("deve parar o contêiner com o timeout informado quando rodando")
    void devePararConteinerComTimeoutQuandoRodando() {
      when(dockerClient.inspectContainerCmd(CONTAINER_ID).exec().getState().getRunning())
              .thenReturn(Boolean.TRUE);

      lifecycle.stop(30);

      verify(dockerClient).stopContainerCmd(CONTAINER_ID);
      verify(dockerClient.stopContainerCmd(CONTAINER_ID)).withTimeout(30);
    }

    @Test
    @DisplayName("deve propagar timeout zero quando rodando")
    void devePropagarTimeoutZeroQuandoRodando() {
      when(dockerClient.inspectContainerCmd(CONTAINER_ID).exec().getState().getRunning())
              .thenReturn(Boolean.TRUE);

      lifecycle.stop(0);

      verify(dockerClient).stopContainerCmd(CONTAINER_ID);
      verify(dockerClient.stopContainerCmd(CONTAINER_ID)).withTimeout(0);
    }

    @Test
    @DisplayName("deve não chamar stop quando o contêiner não está rodando")
    void deveNaoChamarStopQuandoConteinerNaoRodando() {
      when(dockerClient.inspectContainerCmd(CONTAINER_ID).exec().getState().getRunning())
              .thenReturn(Boolean.FALSE);

      lifecycle.stop(10);

      verify(dockerClient, never()).stopContainerCmd(anyString());
    }
  }

  @Nested
  @DisplayName("onExit()")
  class OnExitTest {

    @Test
    @DisplayName("deve executar o callback registrado quando o contêiner sai")
    void deveExecutarCallbackRegistradoQuandoConteinerSai() {
      lifecycle.onExit(exitCallback);
      lifecycle.start();

      ArgumentCaptor<ResultCallback<WaitResponse>> captor = ArgumentCaptor.forClass(ResultCallback.class);
      var waitCmd = dockerClient.waitContainerCmd(CONTAINER_ID);
      verify(waitCmd).exec(captor.capture());

      captor.getValue().onNext(new WaitResponse());

      verify(exitCallback).run();
    }

    @Test
    @DisplayName("deve não lançar exceção quando callback de saída não foi registrado")
    void deveNaoLancarExcecaoQuandoCallbackNaoRegistrado() {
      lifecycle.start();

      ArgumentCaptor<ResultCallback<WaitResponse>> captor = ArgumentCaptor.forClass(ResultCallback.class);
      var waitCmd = dockerClient.waitContainerCmd(CONTAINER_ID);
      verify(waitCmd).exec(captor.capture());

      captor.getValue().onNext(new WaitResponse());

      verifyNoInteractions(exitCallback);
    }
  }

  @Nested
  @DisplayName("getContainerId()")
  class GetContainerIdTest {

    @Test
    @DisplayName("deve retornar o id do contêiner")
    void deveRetornarIdDoConteiner() {
      assertThat(lifecycle.getContainerId()).isEqualTo(CONTAINER_ID);
    }
  }
}