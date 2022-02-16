package com.geekbrains.cloud.jan.nio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class Buffers {

    public static void main(String[] args) throws IOException {

        // flip, rewind, clear, mark
        // get, put (with types)
        ByteBuffer buffer = ByteBuffer.allocate(15);

        ByteBuffer str = ByteBuffer.wrap("Hello world".getBytes(StandardCharsets.UTF_8));

        buffer.put("Hello world".getBytes(StandardCharsets.UTF_8));
        buffer.flip();

//        System.out.println((char) buffer.get());
//        System.out.println((char) buffer.get());
//        System.out.println((char) buffer.get());
//        buffer.mark();
//        System.out.println();
//
//        System.out.println((char) buffer.get());
//        System.out.println((char) buffer.get());
//
//        buffer.reset();
//        System.out.println();
//        System.out.println((char) buffer.get());
//        System.out.println((char) buffer.get());
//        buffer.rewind();
//
//        while (buffer.hasRemaining()) {
//            System.out.println((char) buffer.get());
//        }

        RandomAccessFile raf = new RandomAccessFile("data/file.txt", "rw");
        FileChannel channel = raf.getChannel();

        buffer.clear();

        while (true) {
            int readCount = channel.read(buffer);
            if (readCount == -1) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                System.out.print((char) buffer.get());
            }
            buffer.clear();
        }
        System.out.println();
    }
}
