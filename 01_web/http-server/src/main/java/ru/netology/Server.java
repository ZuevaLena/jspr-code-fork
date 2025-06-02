package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private static final List<String> VALID_PATHS = List.of(
            "/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html",
            "/classic.html", "/events.html", "/events.js");
    private static final int THREAD_POOL_SIZE = 64;
    private final int port;
    private ExecutorService threadPool;

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    threadPool.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            final var requestLine = in.readLine();
            if (requestLine == null) return;

            final var parts = requestLine.split(" ");
            if (parts.length != 3) {
                return;
            }

            final var pathWithQuery = parts[1];
            String path;
            Map<String, String> queryParams = new HashMap<>();
            int queryIndex = pathWithQuery.indexOf('?');
            if (queryIndex != -1) {
                path = pathWithQuery.substring(0, queryIndex);
                String queryString = pathWithQuery.substring(queryIndex + 1);
                parseQuery(queryString, queryParams);
            } else {
                path = pathWithQuery;
            }

            if (!VALID_PATHS.contains(path)) {
                sendResponse(out, "HTTP/1.1 404 Not Found", "text/plain", 0);
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            Request request = new Request(path, queryParams);

            if (path.equals("/classic.html")) {
                handleClassicHtml(out, filePath, mimeType, request);
            } else {
                sendFile(out, filePath, mimeType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseQuery(String queryString, Map<String, String> queryParams) {
        if (queryString == null || queryString.isEmpty()) {
            return;
        }

        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                queryParams.put(keyValue[0], keyValue[1]);
            } else if (keyValue.length == 1) {
                queryParams.put(keyValue[0], "");
            }
        }
    }

    private void handleClassicHtml(BufferedOutputStream out, Path filePath, String mimeType, Request request) throws IOException {
        final var template = Files.readString(filePath);
        final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
        sendResponse(out, "HTTP/1.1 200 OK", mimeType, content.length);
        out.write(content);
        out.flush();
    }

    private void sendFile(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        final var length = Files.size(filePath);
        sendResponse(out, "HTTP/1.1 200 OK", mimeType, length);
        Files.copy(filePath, out);
        out.flush();
    }

    private void sendResponse(BufferedOutputStream out, String status, String contentType, long contentLength) throws IOException {
        out.write((status + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "Connection: close\r\n" +
                "\r\n").getBytes());
    }
}