package io.java.betterreadsdataloader;

import io.java.betterreadsdataloader.author.Author;
import io.java.betterreadsdataloader.author.AuthorRepository;
import io.java.betterreadsdataloader.book.Book;
import io.java.betterreadsdataloader.book.BookRepository;
import io.java.betterreadsdataloader.connection.DataStaxAstraProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.json.JSONArray;
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

  @Autowired
  BookRepository bookRepository;

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

    try (Stream<String> lines = Files.lines(worksDumpPath)) {
      lines.forEach(line -> {
        Book book = getBookFromLine(line);
        if (book != null) {
          System.out.println("--------------------------------------");
          System.out.println("Successfully Saved");
          bookRepository.save(book);
        }
      });
    } catch (Exception e) {
      System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      System.out.println("Failed to save");
      e.printStackTrace();
    }
  }

  private void initAuthors() {
    Path authorsDumpPath = Path.of(authorsDumpLocation);

    try (Stream<String> lines = Files.lines(authorsDumpPath)) {
      lines.forEach(line -> {
        Author author = getAuthorFromLine(line);

        if (author != null) {
          System.out.println("--------------------------------------");
          System.out.println("Successfully Saved");
          authorRepository.save(author);
        }
      });
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

  private Book getBookFromLine(String line) {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    try {
      String jsonString = line.substring(line.indexOf("{"));
      JSONObject json = new JSONObject(jsonString);

      String id = json.optString("key").replace("/authors/", "");

      String name = json.optString("title");

      JSONObject descriptionObj = json.optJSONObject("description");
      String description;
      if (descriptionObj != null)
        description = descriptionObj.optString("value");
      else
        description = null;

      JSONObject publishedDateObj = json.optJSONObject("created");
      String publishedDateString = publishedDateObj.optString("value");
      LocalDate publishedDate = LocalDate.parse(publishedDateString, formatter);

      JSONArray coversObj = json.optJSONArray("covers");
      List<String> covers = new ArrayList<>();

      if (coversObj != null)
        for (int i = 0; i < coversObj.length(); i++) {
          covers.add(coversObj.getString(i));
        }

      JSONArray authorsObj = json.optJSONArray("authors");
      List<String> authorIds = new ArrayList<>();
      List<String> authorNames = new ArrayList<>();

      if (authorsObj != null) {
        for (int i = 0; i < authorsObj.length(); i++) {
          JSONObject authorObj = authorsObj.getJSONObject(i).getJSONObject("author");
          authorIds.add(authorObj.getString("key").replace("/authors/", ""));
        }

        authorNames = authorIds.stream().map(aid -> authorRepository.findById(aid)).map(optionalAuthor -> {
          if (!optionalAuthor.isPresent())
            return "Unknown Author";
          return optionalAuthor.get().getName();
        }).collect(Collectors.toList());
      }

      Book book = new Book(id, name, description, publishedDate, covers, authorIds, authorNames);

      return book;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Bean
  public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraProperties astraProperties) {
    Path bundle = astraProperties.getSecureConnectBundle().toPath();
    return builder -> builder.withCloudSecureConnectBundle(bundle);
  }
}
