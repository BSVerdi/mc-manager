package br.com.mc_manager.server.service.runtime.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalConsoleGatewayTest {

  private final List<String> receivedLines = new ArrayList<>();
  @Mock
  private LocalProcessLifeCycle lifeCycle;
  @Mock
  private Process process;
  @Mock
  private ExecutorService executor;
  private LocalConsoleGateway gateway;
  private ByteArrayOutputStream outputStream;

  @BeforeEach
  void setUp() {
    gateway = new LocalConsoleGateway(lifeCycle, executor);
    outputStream = new ByteArrayOutputStream();

    lenient().doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return CompletableFuture.completedFuture(null);
    }).when(executor).submit(any(Runnable.class));
  }

  @Nested
  @DisplayName("attach(Consumer<String>)")
  class Attach {

    @Test
    @DisplayName("deveAnexarAoConsoleEDispararCallbacksParaCadaLinhaQuandoProcessoExiste")
    void deveAnexarAoConsoleEDispararCallbacksParaCadaLinhaQuandoProcessoExiste() {
      // Arrange
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.getOutputStream()).thenReturn(outputStream);
      when(process.getInputStream()).thenReturn(
              new ByteArrayInputStream("linha1\nlinha2\n".getBytes(StandardCharsets.UTF_8)));

      // Act
      gateway.attach(receivedLines::add);

      // Assert
      assertThat(receivedLines).containsExactly("linha1", "linha2");
    }

    @Test
    @DisplayName("deveLancarIllegalStateExceptionQuandoProcessoAindaNaoFoiIniciado")
    void deveLancarIllegalStateExceptionQuandoProcessoAindaNaoFoiIniciado() {
      // Arrange
      when(lifeCycle.getProcess()).thenReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> gateway.attach(receivedLines::add))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Processo ainda não foi iniciado");
    }
  }

  @Nested
  @DisplayName("write(String)")
  class Write {

    @Test
    @DisplayName("deveEscreverComandoComNovaLinhaNoOutputStream")
    void deveEscreverComandoComNovaLinhaNoOutputStream() {
      // Arrange
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.getOutputStream()).thenReturn(outputStream);
      when(process.getInputStream()).thenReturn(
              new ByteArrayInputStream(new byte[0]));
      gateway.attach(receivedLines::add);

      // Act
      gateway.write("abc");

      // Assert
      String written = outputStream.toString(StandardCharsets.UTF_8);
      assertThat(written).isEqualTo("abc" + System.lineSeparator());
    }

    @Test
    @DisplayName("deveLancarIllegalStateExceptionQuandoConsoleNaoEstaAnexado")
    void deveLancarIllegalStateExceptionQuandoConsoleNaoEstaAnexado() {
      // Act & Assert
      assertThatThrownBy(() -> gateway.write("x"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Console não está anexado");
    }

    @Test
    @DisplayName("deveLancarIllegalStateExceptionQuandoTentarEscreverAposDetach")
    void deveLancarIllegalStateExceptionQuandoTentarEscreverAposDetach() {
      // Arrange
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.getOutputStream()).thenReturn(outputStream);
      when(process.getInputStream()).thenReturn(
              new ByteArrayInputStream(new byte[0]));
      gateway.attach(receivedLines::add);
      gateway.detach();

      // Act & Assert
      assertThatThrownBy(() -> gateway.write("x"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Console não está anexado");
    }

    @Test
    @DisplayName("deveLancarUncheckedIOExceptionQuandoOutputStreamFalha")
    void deveLancarUncheckedIOExceptionQuandoOutputStreamFalha() {
      // Arrange
      OutputStream failingStream = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          throw new IOException("Falha simulada");
        }
      };
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.getOutputStream()).thenReturn(failingStream);
      when(process.getInputStream()).thenReturn(
              new ByteArrayInputStream(new byte[0]));
      gateway.attach(receivedLines::add);

      // Act & Assert
      assertThatThrownBy(() -> gateway.write("x"))
              .isInstanceOf(UncheckedIOException.class)
              .hasMessage("Erro ao enviar comando ao processo");
    }
  }

  @Nested
  @DisplayName("detach()")
  class Detach {

    @Test
    @DisplayName("deveFecharBufferedWriterEPermitirNovoAttach")
    void deveFecharBufferedWriterEPermitirNovoAttach() {
      // Arrange
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.getOutputStream()).thenReturn(outputStream);
      when(process.getInputStream()).thenReturn(
              new ByteArrayInputStream(new byte[0]));
      gateway.attach(receivedLines::add);

      // Act
      gateway.detach();

      // Assert
      assertThatThrownBy(() -> gateway.write("x"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Console não está anexado");
    }

    @Test
    @DisplayName("deveTerComportamentoDeNoOpQuandoNuncaAnexou")
    void deveTerComportamentoDeNoOpQuandoNuncaAnexou() {
      // Arrange
      gateway.detach();

      // Act & Assert
      assertThatThrownBy(() -> gateway.write("x"))
              .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("deveIgnorarIOExceptionNoCloseSilenciosamente")
    void deveIgnorarIOExceptionNoCloseSilenciosamente() {
      // Arrange
      OutputStream failingStream = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          throw new IOException("Falha no close");
        }
      };
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.getOutputStream()).thenReturn(failingStream);
      when(process.getInputStream()).thenReturn(
              new ByteArrayInputStream(new byte[0]));
      gateway.attach(receivedLines::add);

      // Act
      gateway.detach();

      // Assert
      assertThatThrownBy(() -> gateway.write("x"))
              .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("pumpOutput (indiretamente via attach)")
  class PumpOutput {

    @Test
    @DisplayName("deveEntregarCadaLinhaSeparadaAoCallback")
    void deveEntregarCadaLinhaSeparadaAoCallback() {
      // Arrange
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.getOutputStream()).thenReturn(outputStream);
      when(process.getInputStream()).thenReturn(
              new ByteArrayInputStream("alpha\nbeta\ngamma\n".getBytes(StandardCharsets.UTF_8)));

      // Act
      gateway.attach(receivedLines::add);

      // Assert
      assertThat(receivedLines).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    @DisplayName("deveEntregarMensagemDeEncerramentoQuandoStreamFalha")
    void deveEntregarMensagemDeEncerramentoQuandoStreamFalha() {
      // Arrange
      InputStream failingStream = new InputStream() {
        private final byte[] data = "ultima linha\n".getBytes(StandardCharsets.UTF_8);
        private int pos = 0;

        @Override
        public int read() throws IOException {
          if (pos >= data.length) {
            throw new IOException("Stream encerrado");
          }
          return data[pos++];
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
          if (pos >= data.length) {
            throw new IOException("Stream encerrado");
          }
          int toRead = Math.min(len, data.length - pos);
          System.arraycopy(data, pos, b, off, toRead);
          pos += toRead;
          return toRead;
        }
      };

      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.getOutputStream()).thenReturn(outputStream);
      when(process.getInputStream()).thenReturn(failingStream);

      // Act
      gateway.attach(receivedLines::add);

      // Assert
      assertThat(receivedLines).isNotEmpty();
      String last = receivedLines.get(receivedLines.size() - 1);
      assertThat(last).startsWith("[CONSOLE] Stream de saída encerrado");
    }

    @Test
    @DisplayName("deveEncerrarGracefullyQuandoStreamEstiverVazio")
    void deveEncerrarGracefullyQuandoStreamEstiverVazio() {
      // Arrange
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.getOutputStream()).thenReturn(outputStream);
      when(process.getInputStream()).thenReturn(
              new ByteArrayInputStream(new byte[0]));

      // Act
      gateway.attach(receivedLines::add);

      // Assert
      assertThat(receivedLines).isEmpty();
    }
  }
}