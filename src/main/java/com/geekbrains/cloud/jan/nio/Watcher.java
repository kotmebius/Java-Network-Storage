package com.geekbrains.cloud.jan.nio;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class Watcher {

    private final WatchService service;

    public Watcher(Path dir) throws IOException {
        service = FileSystems.getDefault()
                .newWatchService();
        new Thread(this::run).start();
        dir.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    }

    public WatchService getService() {
        return service;
    }

    private void run() {
        try {
            while (true) {
                WatchKey watchKey = service.take();
                System.out.println(watchKey);
                List<WatchEvent<?>> events = watchKey.pollEvents();
                for (WatchEvent<?> event : events) {
                    System.out.println(event.kind() + " - " + event.context());
                }
                watchKey.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
