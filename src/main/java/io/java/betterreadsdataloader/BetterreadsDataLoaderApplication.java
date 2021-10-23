package io.java.betterreadsdataloader;

import io.java.betterreadsdataloader.author.Author;
import io.java.betterreadsdataloader.author.AuthorRepository;
import io.java.betterreadsdataloader.connection.DataStaxAstraProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  // Get the paths to the file to load from yaml file
  @Value("${datadump.location.author}")
  private String authorsDumpLocation;

  @Value("${datadump.location.work}")
  private String worksDumpLocation;

  public static void main(String[] args) {
    SpringApplication.run(BetterreadsDataLoaderApplication.class, args);
  }

  @PostConstruct
  public void start() {
    initAuthors();
    initWorks();
  }

  private void initWorks() {
    Path worksDumpPath = Path.of(worksDumpLocation);
  }

  private void initAuthors() {
    Path authorsDumpPath = Path.of(authorsDumpLocation);

    try (Stream<String> lines = Files.lines(authorsDumpPath)) {
      lines.forEach(
        line -> {
          Author author = getAuthorFromLine(line);

          if (author != null) {
            System.out.println("--------------------------------------");
            System.out.println("Successfully Saved");
            authorRepository.save(author);
          }
        }
      );
    } catch (Exception e) {
      System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      System.out.println("Failed to save");
      e.printStackTrace();
    }
  }

  private Author getAuthorFromLine(String line) {
    try {
      String jsonString = line.substring(line.indexOf("{"));
      JSONObject json = new JSONObject(jsonString);

      String name = json.optString("name");
      String personalName = json.optString("personal_name");
      String id = json.optString("key").replace("/authors/", "");

      Author author = new Author(id, name, personalName);

      return author;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Bean
  public CqlSessionBuilderCustomizer sessionBuilderCustomizer(
    DataStaxAstraProperties astraProperties
  ) {
    Path bundle = astraProperties.getSecureConnectBundle().toPath();
    return builder -> builder.withCloudSecureConnectBundle(bundle);
  }
}
