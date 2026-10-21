package br.com.mc_manager.server.service.runtime.local;

import br.com.mc_manager.server.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalProcessLifeCycleTest {

  private static final String JAVA_EXECUTABLE = "/usr/bin/java";
  private static final Path SERVER_DIRECTORY = Path.of("/tmp/server");
  private static final String MIN_RAM = "2G";
  private static final String MAX_RAM = "4G";
  private static final Long SERVER_ID = 1L;

  @Mock
  private Server server;

  @Mock
  private ExecutorService executor;

  @Mock
  private LocalServerProcessFactory processFactory;

  @Mock
  private Process process;

  private LocalProcessLifeCycle localProcessLifeCycle;

  @BeforeEach
  void setUp() {
    localProcessLifeCycle = new LocalProcessLifeCycle(
            server, JAVA_EXECUTABLE, SERVER_DIRECTORY, executor, processFactory
    );
  }

  private void iniciarProcesso() throws IOException {
    when(server.getMinRam()).thenReturn(MIN_RAM);
    when(server.getMaxRam()).thenReturn(MAX_RAM);
    when(processFactory.create(JAVA_EXECUTABLE, MIN_RAM, MAX_RAM, SERVER_DIRECTORY))
            .thenReturn(process);
    when(process.isAlive()).thenReturn(true);
    localProcessLifeCycle.start();
  }

  @Nested
  @DisplayName("start()")
  class Start {

    @Test
    @DisplayName("deve iniciar o processo e submeter o monitor quando a factory criar com sucesso")
    void deveIniciarOPrcessoESubmeterOMonitorQuandoACriacaoTerSucesso() throws IOException {
      when(server.getMinRam()).thenReturn(MIN_RAM);
      when(server.getMaxRam()).thenReturn(MAX_RAM);
      when(processFactory.create(JAVA_EXECUTABLE, MIN_RAM, MAX_RAM, SERVER_DIRECTORY))
              .thenReturn(process);
      when(process.isAlive()).thenReturn(true);

      localProcessLifeCycle.start();

      verify(executor).submit(any(Runnable.class));
      assertThat(localProcessLifeCycle.isRunning()).isTrue();
    }

    @Test
    @DisplayName("deve lançar IllegalStateException com o id do servidor quando a factory falhar com IOException")
    void deveLancarIllegalStateExceptionComOIdDoServidorQuandoAFalharComIOException() throws IOException {
      when(server.getMinRam()).thenReturn(MIN_RAM);
      when(server.getMaxRam()).thenReturn(MAX_RAM);
      when(server.getId()).thenReturn(SERVER_ID);
      IOException ioException = new IOException("permissão negada");
      when(processFactory.create(JAVA_EXECUTABLE, MIN_RAM, MAX_RAM, SERVER_DIRECTORY))
              .thenThrow(ioException);

      assertThatThrownBy(() -> localProcessLifeCycle.start())
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Erro ao iniciar processo do servidor " + SERVER_ID)
              .hasCause(ioException);
    }
  }

  @Nested
  @DisplayName("isRunning()")
  class IsRunning {

    @Test
    @DisplayName("deve retornar false quando o processo ainda não foi criado")
    void deveRetornarFalseQuandoOPrcessoAindaNaoFoiCriado() {
      assertThat(localProcessLifeCycle.isRunning()).isFalse();
    }

    @Test
    @DisplayName("deve retornar false quando o processo não está mais vivo")
    void deveRetornarFalseQuandoOPrcessoNaoEstaMaisVivo() throws IOException {
      when(server.getMinRam()).thenReturn(MIN_RAM);
      when(server.getMaxRam()).thenReturn(MAX_RAM);
      when(processFactory.create(JAVA_EXECUTABLE, MIN_RAM, MAX_RAM, SERVER_DIRECTORY))
              .thenReturn(process);
      when(process.isAlive()).thenReturn(false);
      localProcessLifeCycle.start();

      assertThat(localProcessLifeCycle.isRunning()).isFalse();
    }

    @Test
    @DisplayName("deve retornar true quando o processo está vivo")
    void deveRetornarTrueQuandoOPrcessoEstaVivo() throws IOException {
      when(server.getMinRam()).thenReturn(MIN_RAM);
      when(server.getMaxRam()).thenReturn(MAX_RAM);
      when(processFactory.create(JAVA_EXECUTABLE, MIN_RAM, MAX_RAM, SERVER_DIRECTORY))
              .thenReturn(process);
      when(process.isAlive()).thenReturn(true);
      localProcessLifeCycle.start();

      assertThat(localProcessLifeCycle.isRunning()).isTrue();
    }
  }

  @Nested
  @DisplayName("stop(int timeoutSeconds)")
  class Stop {

    @Test
    @DisplayName("deve retornar imediatamente sem interagir com o processo quando ele não existe")
    void deveRetornarImediatamenteSemInteragirComOPrcessoQuandoEleNaoExiste() {
      localProcessLifeCycle.stop(5);

      verifyNoInteractions(process);
    }

    @Test
    @DisplayName("deve aguardar o encerramento natural quando o processo encerra dentro do timeout")
    void deveAguardarOEncerramentoNaturalQuandoOPrcessoEncerraDentroDoTimeout() throws IOException, InterruptedException {
      iniciarProcesso();
      when(process.waitFor(10, TimeUnit.SECONDS)).thenReturn(true);

      localProcessLifeCycle.stop(10);

      verify(process).waitFor(10, TimeUnit.SECONDS);
      verify(process, never()).destroyForcibly();
    }

    @Test
    @DisplayName("deve destruir o processo forçadamente quando ele não encerra dentro do timeout")
    void deveDestruirOPrcessoForcamenteQuandoEleNaoEncerraDentroDoTimeout() throws IOException, InterruptedException {
      iniciarProcesso();
      when(process.waitFor(5, TimeUnit.SECONDS)).thenReturn(false);
      when(process.destroyForcibly()).thenReturn(process);

      localProcessLifeCycle.stop(5);

      verify(process).destroyForcibly();
      verify(process).waitFor();
    }

    @Test
    @DisplayName("deve restaurar o flag de interrupção quando a thread for interrompida durante o waitFor")
    void deveRestaurarOFlagDeInterrupcaoQuandoAThreadForInterrompidaDuranteOWaitFor() throws IOException, InterruptedException {
      iniciarProcesso();
      when(process.waitFor(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

      localProcessLifeCycle.stop(5);

      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      verify(process, never()).destroyForcibly();
      Thread.interrupted();
    }
  }

  @Nested
  @DisplayName("onExit(Runnable)")
  class OnExit {

    @Test
    @DisplayName("deve executar o callback registrado quando o processo encerra")
    void deveExecutarOCallbackRegistradoQuandoOPrcessoEncerra() throws IOException {
      when(server.getMinRam()).thenReturn(MIN_RAM);
      when(server.getMaxRam()).thenReturn(MAX_RAM);
      when(processFactory.create(JAVA_EXECUTABLE, MIN_RAM, MAX_RAM, SERVER_DIRECTORY))
              .thenReturn(process);
      localProcessLifeCycle.start();

      ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
      verify(executor).submit(runnableCaptor.capture());

      AtomicBoolean callbackExecuted = new AtomicBoolean(false);
      localProcessLifeCycle.onExit(() -> callbackExecuted.set(true));

      runnableCaptor.getValue().run();

      assertThat(callbackExecuted).isTrue();
    }

    @Test
    @DisplayName("deve encerrar o monitor normalmente sem efeito colateral quando nenhum callback foi registrado")
    void deveEncerrarOMonitorNormalmenteSemEfeitoColateralQuandoNenhumCallbackFoiRegistrado() throws IOException, InterruptedException {
      when(server.getMinRam()).thenReturn(MIN_RAM);
      when(server.getMaxRam()).thenReturn(MAX_RAM);
      when(processFactory.create(JAVA_EXECUTABLE, MIN_RAM, MAX_RAM, SERVER_DIRECTORY))
              .thenReturn(process);
      localProcessLifeCycle.start();

      ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
      verify(executor).submit(runnableCaptor.capture());

      runnableCaptor.getValue().run();

      verify(process).waitFor();
    }
  }
}