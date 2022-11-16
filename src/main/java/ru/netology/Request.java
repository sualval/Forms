package ru.netology;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Request {


    private final String method;
    private final String path;
    private final String body;
    private final List<String> headers;
    private final List<NameValuePair> queryParams;

    public Request(String method, String path, String body, List<String> headers) {


        URI uri;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        this.headers = headers;
        this.method = method;
        this.path = uri.getPath();
        this.body = body;
        queryParams = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);

    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getPostParam(String name) {
        String[] split = body.split("-");
        return Arrays.stream(split).filter(s -> s.equals(name)).toList();
    }

    public String getPostParams() {
        return body;
    }

    public List<NameValuePair> getQueryParam(String name) {

        return getQueryParams().stream().filter(nameValuePair -> nameValuePair.getName().equals(name)).toList();
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }
}
