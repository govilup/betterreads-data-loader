package com.betterwork.loader;

import com.betterwork.entity.Author;
import com.betterwork.entity.Book;
import com.betterwork.repository.AuthorRepository;
import com.betterwork.repository.BookRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DataDumpLoader {

    @Value("${datadump.location.authors}")
    private String authorsDumpLocation;

    @Value("${datadump.location.works}")
    private String worksDumpLocation;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @PostConstruct
    public void start() {
        //initAuthors();
        initWorks();
    }

    private void initAuthors() {
        System.out.println("*********************** Loading Authors to Astra DB **********************");
        Path path = Paths.get(authorsDumpLocation);
        try(Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> {
                String jsonString = line.substring(line.indexOf("{"));
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(jsonString);
                    Author author = author(jsonObject);
                    System.out.println("Saving author "+ author.getName() +"...");
                    authorRepository.save(author);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initWorks() {
        System.out.println("*********************** Loading Books to Astra DB **********************");
        Path path = Paths.get(worksDumpLocation);
        try(Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> {
                String jsonString = line.substring(line.indexOf("{"));
                JSONObject jsonObject = null;
                try {
                    Book book = getBook(new JSONObject(jsonString));
                    System.out.println("Saving book "+ book.getName() +"...");
                    bookRepository.save(book);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Book getBook(JSONObject jsonObject) throws JSONException {
        JSONObject descriptionObj = jsonObject.optJSONObject("description");
        String description = null;
        if(descriptionObj != null) {
            description = descriptionObj.optString("value");
        }
        JSONObject publishedDateObj = jsonObject.optJSONObject("created");
        String dateStr = null;
        if(publishedDateObj != null) {
            dateStr = publishedDateObj.optString("value");
        }

        JSONArray coversJsonArray = jsonObject.optJSONArray("covers");
        List<String> coverIds = new ArrayList<>();
        if(coversJsonArray != null) {
            for (int i = 0; i < coversJsonArray.length(); i++) {
                coverIds.add(coversJsonArray.getString(i));
            }
        }

        JSONArray authorsArray = jsonObject.optJSONArray("authors");
        List<String> authorIds = extractAuthorIds(authorsArray);

        return Book.builder()
                .id(jsonObject.getString("key").replace("/works/", ""))
                .name(jsonObject.optString("title"))
                .description(description)
                .authorNames(getAuthorNames(authorIds))
                .authorIds(authorIds)
                .coverIds(coverIds)
                .publishedDate(LocalDate.parse(dateStr, getFormat()))
                .build();
    }

    private DateTimeFormatter getFormat() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    }

    private List<String> extractAuthorIds(JSONArray authorsArray) throws JSONException {
        List<String> authorIds = new ArrayList<>();
        if(authorsArray != null) {
            for (int i = 0; i < authorsArray.length(); i++) {
                String authorId = authorsArray.getJSONObject(i)
                        .getJSONObject("author")
                        .getString("key").replace("/authors/", "");
                authorIds.add(authorId);
            }
        }
        return authorIds;
    }

    private List<String> getAuthorNames(List<String> authorIds) {
        return authorIds.stream().map(id -> authorRepository.findById(id))
                .map(optionalAuthor -> {
                    if (optionalAuthor.isEmpty()) return "Unknown Author";
                    return optionalAuthor.get().getName();
                }).collect(Collectors.toList());
    }

    private Author author(JSONObject jsonObject) {
        return Author.builder()
                .id(jsonObject.optString("key").replace("/authors/", ""))
                .name(jsonObject.optString("name"))
                .personalName(jsonObject.optString("personal_name"))
                .build();
    }

}
