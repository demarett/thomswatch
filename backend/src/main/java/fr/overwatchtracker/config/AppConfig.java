package fr.overwatchtracker.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class AppConfig {
  @Bean RestClientCustomizer timeouts(@Value("${overfast.connect-timeout}") Duration connect,
      @Value("${overfast.read-timeout}") Duration read) {
    return builder -> {
      var factory = new JdkClientHttpRequestFactory();
      factory.setReadTimeout(read);
      builder.requestFactory(factory);
    };
  }
  @Bean CacheManager cacheManager(@Value("${overfast.cache-minutes}") long minutes) {
    var manager = new CaffeineCacheManager("overfastPlayers","heroCatalog");
    manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(minutes, TimeUnit.MINUTES).maximumSize(500));
    return manager;
  }
}
