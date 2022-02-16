package com.geekbrains.cloud.jan;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Sender {

    private Sender() {}

    static void getFile(DataInputStream is, Path clientDir, int size2, byte[] buf) throws IOException {
        String fileName = is.readUTF();
        System.out.println("received: " + fileName);
        long size = is.readLong();
        try(OutputStream fos = new FileOutputStream(clientDir.resolve(fileName).toFile())) {
            for (int i = 0; i < (size + size2 - 1) / size2; i++) {
                int readBytes = is.read(buf);
                fos.write(buf, 0 , readBytes);
            }
        }
    }


    static void sendFile(String fileName, DataOutputStream os, Path clientDir) throws IOException {
        os.writeUTF("#file#");
        os.writeUTF(fileName);
        Path file = clientDir.resolve(fileName);
        long size = Files.size(file);
        byte[] bytes = Files.readAllBytes(file);
        os.writeLong(size);
        os.write(bytes);
        os.flush();
    }

}
