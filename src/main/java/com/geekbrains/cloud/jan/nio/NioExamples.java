package com.geekbrains.cloud.jan.nio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

public class NioExamples {
    public static void main(String[] args) throws IOException {

        Path path = Paths.get("data") ;
        System.out.println(Files.exists(path));

        Path file = path.resolve("file.txt");
        System.out.println(file.toAbsolutePath());

        Path root = path.resolve("..").normalize();

        Files.list(root)
                .map(Path::toAbsolutePath)
                .forEach(System.out::println);

        Files.write(
                file,
                "Hello world!".getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND
        );

//        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                System.out.println(file);
//                return super.visitFile(file, attrs);
//            }
//        });

        Files.walk(root, 3)
                .forEach(System.out::println);

        List<String> filesTree = Files.walk(root, 3)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());

        System.out.println(filesTree);

        new Watcher(path);
    }
}
