package ru.netology;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {

        final var server = new Server();

        server.addHandler("POST", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
                String text = "Hello";
                responseStream.write(("HTTP/1.1 200 OK\r\n" + "Content-Type: " + "text/plain" + "\r\n" + "Content-Length: " + text.length() + "\r\n" + "Connection: close\r\n" + "\r\n" + text).getBytes());
            }
        });
        server.addHandler("GET", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
                final var filePath = Path.of(".", "public", "/spring.svg");
                final var mimeType = Files.probeContentType(filePath);
                final var length = Files.size(filePath);
                responseStream.write(("HTTP/1.1 200 OK\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: " + length + "\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
                Files.copy(filePath, responseStream);
                responseStream.flush();
            }
        });
        server.start(64, 9999);
    }
}






