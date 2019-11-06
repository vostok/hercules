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
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.sentry.api.auth.BearerAuthHttpInterceptor;
import ru.kontur.vostok.hercules.sentry.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.api.model.Organization;
import ru.kontur.vostok.hercules.sentry.api.model.OrganizationInfo;
import ru.kontur.vostok.hercules.sentry.api.model.Project;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.sentry.api.model.ResponseMessage;
import ru.kontur.vostok.hercules.sentry.api.model.Team;
import ru.kontur.vostok.hercules.sentry.api.model.TeamInfo;
import ru.kontur.vostok.hercules.sentry.sink.ErrorInfo;
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

    private static final String ORGANIZATIONS_URL = API_URL + "organizations/";
    private static final String PROJECTS_URL = API_URL + "projects/";
    private static final String TEAMS_URL = API_URL + "teams/";

    private static final String GET_PROJECTS_URL = ORGANIZATIONS_URL + "%s/projects/";
    private static final String GET_PUBLIC_DSN_URL = PROJECTS_URL + "%s/%s/keys/";
    private static final String GET_TEAMS_URL = ORGANIZATIONS_URL + "%s/teams/";
    private static final String CREATE_TEAM_URL = ORGANIZATIONS_URL + "%s/teams/";
    private static final String CREATE_PROJECT_URL = TEAMS_URL + "%s/%s/projects/";

    private static final String CLIENT_API_ERROR = "ClientApiError";
    private static final String CANNOT_CREATE_JSON = "CannotCreateJSON";

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

    /**
     * Ping of Sentry
     *
     * @return the {@link Result} object with error information in case of error
     */
    public Result<Void, ErrorInfo> ping() {
        return request(
                new HttpHead(API_URL),
                new TypeReference<Void>() {});
    }

    /**
     * Get the organizations for this Sentry API client
     *
     * @return the {@link Result} object with a list of organizations
     */
    public Result<List<OrganizationInfo>, ErrorInfo> getOrganizations() {
        return pagedRequest(
                new HttpGet(ORGANIZATIONS_URL),
                new TypeReference<List<OrganizationInfo>>() {});
    }

    /**
     * Get the projects which match the organization
     *
     * @param organization the organization
     * @return the {@link Result} object with a list of projects
     */
    public Result<List<ProjectInfo>, ErrorInfo> getProjects(String organization) {
        return pagedRequest(
                new HttpGet(String.format(GET_PROJECTS_URL, organization)),
                new TypeReference<List<ProjectInfo>>() {});
    }

    /**
     * Get a list of public DSN which match the project
     *
     * @param organization the organization of the project
     * @param project the project for which a list of public DSN is requested
     * @return the {@link Result} object with a list of public DSN
     */
    public Result<List<KeyInfo>, ErrorInfo> getPublicDsn(String organization, String project) {
        return pagedRequest(
                new HttpGet(String.format(GET_PUBLIC_DSN_URL, organization, project)),
                new TypeReference<List<KeyInfo>>() {} );
    }

    /**
     * Get the teams which match the organization
     *
     * @param organization the organization
     * @return the {@link Result} object with a list of teams
     */
    public Result<List<TeamInfo>, ErrorInfo> getTeams(String organization) {
        return pagedRequest(
                new HttpGet(String.format(GET_TEAMS_URL, organization)),
                new TypeReference<List<TeamInfo>>() {} );
    }

    /**
     * Create new organization in the Sentry
     *
     * @param organization the name of an organization
     * @return the {@link Result} object with created organization or error
     */
    public Result<OrganizationInfo, ErrorInfo> createOrganization(String organization) {
        Organization organizationModel = new Organization(organization);
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(organizationModel);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Cannot create JSON from model for organization creation: %s", e.getMessage()));
            return Result.error(new ErrorInfo(CANNOT_CREATE_JSON, false));
        }
        HttpPost post = new HttpPost(ORGANIZATIONS_URL);
        post.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_JSON));
        LOGGER.info(String.format("Creating of new organization '%s' in Sentry", organization));
        return request(post, new TypeReference<OrganizationInfo>() {});
    }

    /**
     * Create new team in the Sentry
     *
     * @param organization the organization where need to create a team
     * @param team the team name
     * @return the {@link Result} object with created team or error
     */
    public Result<TeamInfo, ErrorInfo> createTeam(String organization, String team) {
        Team teamModel = new Team(team);
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(teamModel);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Cannot create JSON from model for team creation: %s", e.getMessage()));
            return Result.error(new ErrorInfo(CANNOT_CREATE_JSON, false));
        }
        HttpPost post = new HttpPost(String.format(CREATE_TEAM_URL, organization));
        post.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_JSON));
        LOGGER.info(String.format("Creating of new team '%s' in Sentry", team));
        return request(post, new TypeReference<TeamInfo>() {});
    }

    /**
     * Create new project in the Sentry
     *
     * @param organization the organization where need to create a project
     * @param team the team where need to create a project
     * @param project the project name
     * @return the {@link Result} object with created project or error
     */
    public Result<ProjectInfo, ErrorInfo> createProject(String organization, String team, String project) {
        Project projectModel = new Project(project);
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(projectModel);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Cannot create JSON from model for project creation: %s", e.getMessage()));
            return Result.error(new ErrorInfo(CANNOT_CREATE_JSON, false));
        }
        HttpPost post = new HttpPost(String.format(CREATE_PROJECT_URL, organization, team));
        post.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_JSON));
        LOGGER.info(String.format("Creating of new project '%s' in Sentry", project));
        return request(post, new TypeReference<ProjectInfo>() {});
    }

    private <T> Result<T, ErrorInfo> request(HttpUriRequest request, TypeReference<T> typeReference) {
        try (CloseableHttpResponse response = httpClient.execute(sentryHost, request)) {
            Optional<HttpEntity> entity = Optional.ofNullable(response.getEntity());
            if (isErrorResponse(response)) {
                String message = null;
                if (entity.isPresent()) {
                    ResponseMessage responseMessage = objectMapper
                            .readValue(entity.get().getContent(), new TypeReference<ResponseMessage>() {});
                    message = responseMessage.getDetail();
                }
                return Result.error(new ErrorInfo(CLIENT_API_ERROR, extractStatusCode(response), message));
            }
            T value = null;
            if (entity.isPresent()) {
                value = objectMapper.readValue(entity.get().getContent(), typeReference);
            }
            return Result.ok(value);
        } catch (Exception e) {
            return Result.error(new ErrorInfo(CLIENT_API_ERROR, e.getMessage()));
        }
    }

    private <T> Result<List<T>, ErrorInfo> pagedRequest(HttpUriRequest request, TypeReference<List<T>> listTypeReference) {
        List<T> resultList = new LinkedList<>();
        Optional<String> nextCursor = Optional.empty();
        try {
            do {
                // Add cursor info to support pagination
                if(nextCursor.isPresent()) {
                    RequestBuilder requestBuilder = RequestBuilder.copy(request);
                    requestBuilder.addParameter("cursor", nextCursor.get());
                    request = requestBuilder.build();
                }
                try (CloseableHttpResponse response = httpClient.execute(sentryHost, request)) {
                    Optional<HttpEntity> entity = Optional.ofNullable(response.getEntity());
                    if (isErrorResponse(response)) {
                        String message = null;
                        if (entity.isPresent()) {
                            ResponseMessage responseMessage = objectMapper
                                    .readValue(entity.get().getContent(), new TypeReference<ResponseMessage>() {});
                            message = responseMessage.getDetail();
                        }
                        return Result.error(new ErrorInfo(CLIENT_API_ERROR, extractStatusCode(response), message));
                    }
                    if (entity.isPresent()) {
                        resultList.addAll(objectMapper.readValue(entity.get().getContent(), listTypeReference));
                    }
                    nextCursor = getCursorValue(response.getFirstHeader("Link"));
                }
            } while (nextCursor.isPresent());
            return Result.ok(resultList);
        } catch (Exception e) {
            return Result.error(new ErrorInfo(CLIENT_API_ERROR, e.getMessage()));
        }
    }

    private static boolean isErrorResponse(CloseableHttpResponse response) {
        return 400 <= extractStatusCode(response);
    }

    private static int extractStatusCode(CloseableHttpResponse response) {
        return response.getStatusLine().getStatusCode();
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
