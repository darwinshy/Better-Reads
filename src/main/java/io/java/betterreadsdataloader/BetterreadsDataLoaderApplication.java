package io.java.betterreadsdataloader;

import io.java.betterreadsdataloader.author.Author;
import io.java.betterreadsdataloader.author.AuthorRepository;
import io.java.betterreadsdataloader.connection.DataStaxAstraProperties;
import java.nio.file.Path;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com")
@EnableConfigurationProperties(DataStaxAstraProperties.class)
public class BetterreadsDataLoaderApplication {

  @Autowired
  AuthorRepository authorRepository;

  public static void main(String[] args) {
    SpringApplication.run(BetterreadsDataLoaderApplication.class, args);
  }

  @PostConstruct
  public void start() {
    System.out.println("Application Started");

    Author author = new Author("id", "darwin", "Shashwat");
    authorRepository.save(author);
  }

  @Bean
  public CqlSessionBuilderCustomizer sessionBuilderCustomizer(
    DataStaxAstraProperties astraProperties
  ) {
    Path bundle = astraProperties.getSecureConnectBundle().toPath();
    return builder -> builder.withCloudSecureConnectBundle(bundle);
  }
}
