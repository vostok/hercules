package ru.kontur.vostok.hercules.sentry.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.sentry.api.auth.BearerAuthHttpInterceptor;
import ru.kontur.vostok.hercules.sentry.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.api.model.OrganizationCreateModel;
import ru.kontur.vostok.hercules.sentry.api.model.OrganizationInfo;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectCreateModel;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.sentry.api.model.TeamCreateModel;
import ru.kontur.vostok.hercules.sentry.api.model.TeamInfo;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private static final String PROJECTS_URL = "projects/";
    private static final String TEAM_URL = "teams/";
    private static final String ORGANIZATIONS_URL = "organizations/";
    private static final String KEYS_URL = "keys/";

    private static final String DEFAULT_TEAM = "default_team";

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

    public Result<List<OrganizationInfo>, String> getOrganizations() {
        String uri = API_URL + ORGANIZATIONS_URL;
        return pagedRequest(new HttpGet(uri), new TypeReference<List<OrganizationInfo>>() {});
    }

    /**
     * Get the projects which match the organization
     *
     * @param organization the organization
     * @return the {@link Result} object  with a list of projects
     */
    public Result<List<ProjectInfo>, String> getProjects(String organization) {
        String uri = API_URL + ORGANIZATIONS_URL + organization + "/" + PROJECTS_URL;
        return pagedRequest(new HttpGet(uri), new TypeReference<List<ProjectInfo>>() {});
    }

    /**
     * Get a list of public DSN which match the project
     *
     * @param organization the project organization
     * @param project the project for which a list of public DSN is requested
     * @return the {@link Result} object with a list of public DSN
     */
    public Result<List<KeyInfo>, String> getPublicDsn(String organization, String project) {
        String uri = API_URL + PROJECTS_URL + organization + "/" +  project + "/" + KEYS_URL;
        return pagedRequest(new HttpGet(uri), new TypeReference<List<KeyInfo>>() {} );
    }

    /**
     * Create new organisation in the Sentry
     *
     * @param organization the name of organisation
     * @return the {@link Result} object with response entity or error
     */
    public Result<OrganizationInfo, String> createOrganization(String organization) {
        OrganizationCreateModel organizationModel = new OrganizationCreateModel(organization);
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body = {};
        try {
            body = objectMapper.writeValueAsBytes(organizationModel);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot create JSON from model for organization creation", e);
        }

        String uri = API_URL + ORGANIZATIONS_URL;
        HttpPost post = new HttpPost(uri);
        post.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_JSON));
        LOGGER.info(String.format("Creating of new organization '%s' in Sentry", organization));
        return request(post, new TypeReference<OrganizationInfo>() {});
    }

    public Result<TeamInfo, String> createTeam(String team, String organization) {
        TeamCreateModel teamModel = new TeamCreateModel(team);
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body = {};
        try {
            body = objectMapper.writeValueAsBytes(teamModel);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot create JSON from model for team creation", e);
        }
        String uri = API_URL + ORGANIZATIONS_URL + organization + "/" + TEAM_URL;
        HttpPost post = new HttpPost(uri);
        post.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_JSON));
        LOGGER.info(String.format("Creating of new team '%s' in Sentry", team));
        return request(post, new TypeReference<TeamInfo>() {});
    }

    public Result<TeamInfo, String> createTeam(String organization) {
        return createTeam(DEFAULT_TEAM, organization);
    }

    public Result<ProjectInfo, String> createProject(String organization, String project) {
        ProjectCreateModel projectModel = new ProjectCreateModel(project);
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body = {};
        try {
            body = objectMapper.writeValueAsBytes(projectModel);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot create JSON from model for project creation", e);
        }
        String uri = API_URL + TEAM_URL + organization + "/" + DEFAULT_TEAM + "/" + PROJECTS_URL;
        HttpPost post = new HttpPost(uri);
        post.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_JSON));
        LOGGER.info(String.format("Creating of new project '%s' in Sentry", project));
        return request(post, new TypeReference<ProjectInfo>() {});
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
