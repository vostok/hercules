package ru.kontur.vostok.hercules.management.api.routing.sentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperReadRepository;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineConfig;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineRoute;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;
import ru.kontur.vostok.hercules.routing.sentry.SentryRouting;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * Unit tests for {@link SentryRouteValidator}.
 *
 * @author Aleksandr Yuferov
 */
@RunWith(Parameterized.class)
public class SentryRouteValidatorTest {
    private SentryRouteValidator validator;

    @Before
    public void prepare() {
        ZookeeperReadRepository repository = Mockito.mock(ZookeeperReadRepository.class);
        validator = new SentryRouteValidator(repository, SentryRouting.DEFAULT_CONFIG);

        DecisionTreeEngineConfig config = DecisionTreeEngineConfig.builder()
                .addAllowedTag("properties/project")
                .addAllowedTag("properties/subproject")
                .addAllowedTag("properties/application")
                .addAllowedTag("properties/environment")
                .build();
        doReturn(config).when(repository).fetchEngineConfig(Mockito.any());

        doReturn(List.of("e610b9c3-4ccd-415a-b158-de98b27db57a.json"))
                .when(repository).fetchAllRoutesFilesNames(any());

        doReturn(DecisionTreeEngineRoute.<SentryDestination>builder()
                .setId(UUID.fromString("e610b9c3-4ccd-415a-b158-de98b27db57a"))
                .setConditions(Map.of(
                        "properties/project", "my-project",
                        "properties/subproject", "my-project-subproject"
                ))
                .setDestination(SentryDestination.of("my-organization", "my-project"))
                .build()
        ).when(repository).fetchRouteByRelativePath(eq("e610b9c3-4ccd-415a-b158-de98b27db57a.json"), any());
    }

    @Parameterized.Parameter
    public DecisionTreeEngineRoute<SentryDestination> route;

    @Parameterized.Parameter(1)
    public ValidationResult expectedResult;

    @Test
    public void test() {
        ValidationResult actualResult = validator.validate(route);

        Assert.assertEquals(expectedResult.isOk(), actualResult.isOk());
        if (expectedResult.isError()) {
            Assert.assertEquals(expectedResult.error(), actualResult.error());
        }
    }

    @Parameterized.Parameters
    public static Object[][] parameters() {
        return new Object[][]{
                {
                        DecisionTreeEngineRoute.<SentryDestination>builder()
                                .setConditions(Map.of(
                                        "properties/project", "my-project",
                                        "properties/subproject", "my-project-subproject"
                                ))
                                .setDestination(SentryDestination.of("my-organization", "my-project"))
                                .build(),
                        ValidationResult.error("a route e610b9c3-4ccd-415a-b158-de98b27db57a exists with " +
                                "identical conditions")
                },
                {
                        DecisionTreeEngineRoute.<SentryDestination>builder()
                                .setConditions(Map.of(
                                        "properties/project", "other-project",
                                        "properties/subproject", "my-project-subproject"
                                ))
                                .setDestination(null)
                                .build(),
                        ValidationResult.error("destination cannot be null (if you want to create 'nowhere' destination use "
                                + "'{ \"organization\": null, \"project\": \"null\" }' destination)")
                },
                {
                        DecisionTreeEngineRoute.<SentryDestination>builder()
                                .setConditions(Map.of(
                                        "properties/project", "other-project",
                                        "properties/subproject", "my-project-subproject"
                                ))
                                .setDestination(SentryDestination.toNowhere())
                                .build(),
                        ValidationResult.ok()
                },
                {
                        DecisionTreeEngineRoute.<SentryDestination>builder()
                                .setConditions(Map.of(
                                        "properties/project", "other-project",
                                        "properties/subproject", "my-project-subproject"
                                ))
                                .setDestination(SentryDestination.of("{tag:properties/project}", "my-project"))
                                .build(),
                        ValidationResult.error("constant interpolations found in 'organization' field:\n" +
                                "* '{tag:properties/project}' can be replaced with 'other-project'")
                },
                {
                        DecisionTreeEngineRoute.<SentryDestination>builder()
                                .setConditions(Map.of(
                                        "properties/project", "other-project",
                                        "properties/subproject", "my-project-subproject"
                                ))
                                .setDestination(SentryDestination.of("other-project", "{tag:properties/project}"))
                                .build(),
                        ValidationResult.error("constant interpolations found in 'project' field:\n" +
                                "* '{tag:properties/project}' can be replaced with 'other-project'")
                },
                {
                        DecisionTreeEngineRoute.<SentryDestination>builder()
                                .setConditions(Map.of(
                                        "some-tag", "other-project"
                                ))
                                .setDestination(SentryDestination.of("other-project", "my-project"))
                                .build(),
                        ValidationResult.error("tag paths found in conditions that are not allowed: some-tag")
                },
                {
                        DecisionTreeEngineRoute.<SentryDestination>builder()
                                .setConditions(Map.of(
                                        "properties/project", "other-project",
                                        "properties/subproject", "my-project-other-subproject"
                                ))
                                .setDestination(SentryDestination.of("my-org", "my-proj"))
                                .build(),
                        ValidationResult.ok()
                }
        };
    }
}