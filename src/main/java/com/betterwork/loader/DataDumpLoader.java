package com.betterwork.loader;

import com.betterwork.author.Author;
import com.betterwork.author.AuthorRepository;
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
import java.util.stream.Stream;

@Component
public class DataDumpLoader {

    @Value("${datadump.location.authors}")
    private String authorsDumpLocation;

    @Value("${datadump.location.works}")
    private String worksDumpLocation;

    @Autowired
    private AuthorRepository authorRepository;

    @PostConstruct
    public void start() {
        System.out.println("Data Load Started...");
        initAuthors();
        System.out.println("Data Load Completed.");
    }

    private void initAuthors() {
        Path path = Paths.get(authorsDumpLocation);
        try(Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> {
                String jsonString = line.substring(line.indexOf("{"));
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(jsonString);
                    authorRepository.save(getAuthor(jsonObject));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Author getAuthor(JSONObject jsonObject) {
        return Author.builder()
                .id(jsonObject.optString("key").replace("/authors/", ""))
                .name(jsonObject.optString("name"))
                .build();
    }

}
