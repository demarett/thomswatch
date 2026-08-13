package fr.overwatchtracker.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class AppConfigTest {
  @Test
  void appliesTheConfiguredConnectTimeoutToTheJdkClient() throws Exception {
    var connectTimeout = Duration.ofSeconds(2);
    var builder = RestClient.builder();
    new AppConfig().timeouts(connectTimeout, Duration.ofSeconds(10)).customize(builder);

    try (var endpoint = UnresponsiveEndpoint.create()) {
      var client = builder.baseUrl(endpoint.url()).build();
      var startedAt = System.nanoTime();

      assertTimeoutPreemptively(connectTimeout.multipliedBy(2), () ->
          assertThrows(RestClientException.class, () ->
              client.get().retrieve().toBodilessEntity()));
      var elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

      assertTrue(elapsed.compareTo(connectTimeout) >= 0,
          () -> "Connection failed before the configured timeout: " + elapsed);
      assertTrue(elapsed.compareTo(connectTimeout.multipliedBy(2)) < 0,
          () -> "Connection exceeded twice the configured timeout: " + elapsed);
    }
  }

  private record UnresponsiveEndpoint(ServerSocket server, List<Socket> connections)
      implements AutoCloseable {
    static UnresponsiveEndpoint create() throws IOException {
      var loopback = InetAddress.getByName("127.0.0.1");
      var server = new ServerSocket();
      server.bind(new InetSocketAddress(loopback, 0), 1);
      var address = new InetSocketAddress(loopback, server.getLocalPort());
      var connections = new ArrayList<Socket>();
      while (true) {
        var connection = new Socket();
        try {
          connection.connect(address, 100);
          connections.add(connection);
        } catch (SocketTimeoutException expected) {
          connection.close();
          return new UnresponsiveEndpoint(server, connections);
        }
      }
    }

    String url() {
      return "http://" + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort();
    }

    @Override
    public void close() throws IOException {
      for (var connection : connections) {
        connection.close();
      }
      server.close();
    }
  }
}
