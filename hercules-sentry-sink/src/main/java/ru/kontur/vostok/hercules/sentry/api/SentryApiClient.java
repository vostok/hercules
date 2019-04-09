package ru.kontur.vostok.hercules.sentry.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.sentry.api.auth.BearerAuthHttpInterceptor;
import ru.kontur.vostok.hercules.sentry.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.api.model.OrganizationInfo;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Client of Sentry http-API
 *
 * @author Kirill Sulim
 */
public class SentryApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentryApiClient.class);

    private static final String API_URL = "/api/0/";
    private static final String PROJECTS_URL = API_URL + "projects/";
    private static final String ORGANIZATIONS_URL = API_URL + "organizations/";
    private static final String KEYS_URL_TEMPLATE = PROJECTS_URL + "%s/%s/keys/";

    private final ObjectMapper objectMapper;
    private final HttpHost sentryHost;
    private final CloseableHttpClient httpClient;

    public SentryApiClient(String sentryUrl, String token) {
        this.objectMapper = new ObjectMapper();
        this.sentryHost = HttpHost.create(sentryUrl);
        this.httpClient = HttpClients.custom()
                .addInterceptorFirst(new BearerAuthHttpInterceptor(token))
                .setRetryHandler(new StandardHttpRequestRetryHandler())
                .build();
    }

    public Result<Void, String> ping() {
        return request(new HttpHead(API_URL), new TypeReference<Void>() {});
    }

    /**
     * Get the projects which match the Sentry client
     *
     * @return the {@link Result} object  with a list of projects
     */
    public Result<List<ProjectInfo>, String> getProjects() {
        return pagedRequest(new HttpGet(PROJECTS_URL), new TypeReference<List<ProjectInfo>>() {});
    }

    /**
     * Get a list of public DSN which match the project
     *
     * @param project the project for which a list of public DSN is requested
     * @return the {@link Result} object with a list of public DSN
     */
    public Result<List<KeyInfo>, String> getPublicDsn(ProjectInfo project) {
        Optional<String> projectSlug = Optional.ofNullable(project.getSlug());
        Optional<String> organizationSlug = Optional.ofNullable(project.getOrganization()).map(OrganizationInfo::getSlug);

        if (!projectSlug.isPresent() || !organizationSlug.isPresent()) {
            return Result.error("Not enough info");
        }

        return pagedRequest(
                new HttpGet(String.format(KEYS_URL_TEMPLATE, organizationSlug.get(), projectSlug.get())),
                new TypeReference<List<KeyInfo>>() {}
        );
    }

    /**
     * Create new organisation in the Sentry
     *
     * @param name the name of organisation
     * @return the {@link Result} object with response entity or error
     */
    public Result<Void, String> createOrganization(String name) {
        HttpPost post = new HttpPost(ORGANIZATIONS_URL);
        post.getParams().setParameter("name", name);
        post.getParams().setParameter("slug", name);
        post.getParams().setParameter("agreeTerms", false);

        return request(post, new TypeReference<Void>() {});
    }

    private <T> Result<T, String> request(HttpUriRequest request, TypeReference<T> typeReference) {
        try (CloseableHttpResponse response = httpClient.execute(sentryHost, request)) {
            if (isErrorResponse(response)) {
                return Result.error(extractErrorMessage(response));
            }
            T value = null;
            Optional<HttpEntity> entity = Optional.ofNullable(response.getEntity());
            if (entity.isPresent()) {
                value = objectMapper.readValue(entity.get().getContent(), typeReference);
            }
            return Result.ok(value);
        } catch (Exception e) {
            LOGGER.error("Error on request", e);
            return Result.error(e.getMessage());
        }
    }

    private <T> Result<List<T>, String> pagedRequest(HttpUriRequest request, TypeReference<List<T>> listTypeReference) {
        List<T> resultList = new LinkedList<>();
        Optional<String> nextCursor = Optional.empty();
        try {
            do {
                // Add cursor info to support pagination
                nextCursor.ifPresent(cursorValue -> {
                    request.getParams().removeParameter("cursor");
                    request.getParams().setParameter("cursor", cursorValue);
                });
                try (CloseableHttpResponse response = httpClient.execute(sentryHost, request)) {
                    if (isErrorResponse(response)) {
                        return Result.error(extractErrorMessage(response));
                    }
                    resultList.addAll(objectMapper.readValue(response.getEntity().getContent(), listTypeReference));
                    nextCursor = getCursorValue(response.getFirstHeader("Link"));
                }
            } while (nextCursor.isPresent());
            return Result.ok(resultList);
        } catch (Exception e) {
            LOGGER.error("Error on paged request", e);
            return Result.error(e.getMessage());
        }
    }

    private static boolean isErrorResponse(CloseableHttpResponse response) {
        return 400 <= response.getStatusLine().getStatusCode();
    }

    private static String extractErrorMessage(CloseableHttpResponse response) {
        return response.getStatusLine().getReasonPhrase();
    }

    private static Optional<String> getCursorValue(Header linkHeader) {
        for (HeaderElement element : linkHeader.getElements()) {
            Map<String, String> params = Arrays.stream(element.getParameters())
                    .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

            if (!"next".equals(params.get("rel"))) {
                continue;
            }
            if ("false".equals(params.get("results"))) {
                return Optional.empty();
            }
            return Optional.ofNullable(params.get("cursor"));
        }
        return Optional.empty();
    }
}
