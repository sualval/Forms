package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public static final String GET = "GET";
    public static final String POST = "POST";
    final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> hashMap = new ConcurrentHashMap<>();
    final List<String> allowedMethods = List.of(GET, POST);

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void addHandler(String method, String path, Handler handler) {
        hashMap.putIfAbsent(method, new ConcurrentHashMap<>());
        hashMap.get(method).put(path, handler);
    }

    public void start(int countThreadsPool, int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            final ExecutorService es = Executors.newFixedThreadPool(countThreadsPool);

            while (true) {
                try {
                    var socket = serverSocket.accept();
                    es.submit(() -> run(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run(Socket socket) {

        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {

            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                return;
            }
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                badRequest(out);
                return;
            }


            final var method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                return;
            }

            final var path = requestLine[1];
            if (!path.startsWith("/")) {
                badRequest(out);
                return;
            }
            System.out.println(path);

            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                return;
            }
            in.reset();
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));


            Request request = null;
            if (!method.equals(GET)) {
                in.skip(headersDelimiter.length);
                final var contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);
                    final var body = new String(bodyBytes);
                    request = new Request(method, path, body, headers);
                }
            } else {
                request = new Request(method, path, "", headers);
            }

            System.out.println(method + " " + Thread.currentThread().getName());

            if (!hashMap.containsKey(method)) {
                notFound(out);
                return;
            }

            var methodHandlers = hashMap.get(method);
            assert request != null;
            if (!methodHandlers.containsKey(request.getPath())) {
                notFound(out);
                return;
            }
            methodHandlers.get(request.getPath()).handle(request, out);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notFound(BufferedOutputStream out) throws IOException {
        out.write(("HTTP/1.1 404 Not Found\r\n"
                + "Content-Length: 0\r\n"
                + "Connection: close\r\n"
                + "\r\n").getBytes());
        out.flush();
    }

}