package io.java.betterreadsdataloader;

import connection.DataStaxAstraProperties;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraProperties.class)
public class BetterreadsDataLoaderApplication {

  public static void main(String[] args) {
    SpringApplication.run(BetterreadsDataLoaderApplication.class, args);
  }

  @Bean
  public CqlSessionBuilderCustomizer sessionBuilderCustomizer(
    DataStaxAstraProperties properties
  ) {
    Path bundle = properties.getSecureConnectBundle().toPath();
    return builder -> builder.withCloudSecureConnectBundle(bundle);
  }
}
