package ru.kontur.vostok.hercules.protocol.util;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.HerculesProtocolAssert;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TagDescriptionBuilderTest {

    private enum TestEnum {
        A, B
    }

    @Test
    public void shouldExtractEnum() throws Exception {
        TagDescription<TestEnum> description = TagDescriptionBuilder.enumValue("test", TestEnum.class)
                .build();

        TestEnum extractedA = ContainerUtil.extract(
                ContainerBuilder.create().tag("test", Variant.ofString("A")).build(),
                description
        );

        TestEnum extractedB = ContainerUtil.extract(
                ContainerBuilder.create().tag("test", Variant.ofString("b")).build(),
                description
        );

        assertThat(extractedA, is(TestEnum.A));
        assertThat(extractedB, is(TestEnum.B));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionOnWrongEnumValue() throws Exception {
        TagDescription<TestEnum> description = TagDescriptionBuilder.enumValue("test", TestEnum.class)
                .build();

        ContainerUtil.extract(
                ContainerBuilder.create().tag("test", Variant.ofString("C")).build(),
                description
        );
    }

    @Test
    public void shouldAllowStringAndTextVariantsInTextualTag() throws Exception {
        TagDescription<String> description = TagDescriptionBuilder.string("test").build();

        String stringVariantValue = ContainerUtil.extract(
                ContainerBuilder.create().tag("test", Variant.ofString("abc")).build(),
                description
        );
        String textVariantValue = ContainerUtil.extract(
                ContainerBuilder.create().tag("test", Variant.ofString("def")).build(),
                description
        );

        assertThat(stringVariantValue, is("abc"));
        assertThat(textVariantValue, is("def"));
    }

    @Test
    public void shouldAllowVectorOfContainersTag() throws Exception {
        TagDescription<Container[]> description = TagDescriptionBuilder.containerList("test").build();

        Container[] expectedContainerVector = new Container[]{
                ContainerBuilder.create().tag("a", Variant.ofInteger(1)).build()
        };

        Container[] extractedContainerVector = ContainerUtil.extract(
                ContainerBuilder.create()
                        .tag("test", Variant.ofVector(Vector.ofContainers(expectedContainerVector)))
                        .build(),
                description
        );

        HerculesProtocolAssert.assertArrayEquals(expectedContainerVector, extractedContainerVector, HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldApplyConvert() throws Exception {
        TagDescription<String> description = TagDescriptionBuilder.parsable("test", s -> s + s).build();

        String extracted = ContainerUtil.extract(
                ContainerBuilder.create().tag("test", Variant.ofString("value")).build(),
                description
        );

        assertThat(extracted, is("valuevalue"));
    }

    @Test
    public void shouldUseDefaultValueInCaseOfValueAbsence() throws Exception {
        TagDescription<String> description = TagDescriptionBuilder.string("test")
                .addDefault(() -> "Default value")
                .build();

        String extracted = ContainerUtil.extract(
                ContainerBuilder.create().build(),
                description
        );

        assertThat(extracted, is("Default value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionInCaseOfAbsenceOfRequiredValue() throws Exception {
        TagDescription<String> description = TagDescriptionBuilder.string("test").build();

        ContainerUtil.extract(
                ContainerBuilder.create().build(),
                description
        );
    }
}
