package ru.kontur.vostok.hercules.protocol.util;

import org.junit.Assert;
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
                Container.of("test", Variant.ofString("A")),
                description
        );

        TestEnum extractedB = ContainerUtil.extract(
                Container.of("test", Variant.ofString("b")),
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
                Container.of("test", Variant.ofString("C")),
                description
        );
    }

    @Test
    public void shouldExtractString() throws Exception {
        TagDescription<String> description = TagDescriptionBuilder.string("test").build();

        String stringVariantValue = ContainerUtil.extract(
                Container.of("test", Variant.ofString("abc")),
                description
        );

        assertThat(stringVariantValue, is("abc"));
    }

    @Test
    public void shouldExtractVectorOfStrings() {
        TagDescription<String[]> description = TagDescriptionBuilder.stringVector("test").build();

        String[] extractedStringVector = ContainerUtil.extract(
                Container.of("test", Variant.ofVector(Vector.ofStrings("string1", "string2"))),
                description
        );

        HerculesProtocolAssert.assertArrayEquals(new String[]{"string1", "string2"}, extractedStringVector, Assert::assertEquals);
    }

    @Test
    public void shouldAllowVectorOfContainersTag() throws Exception {
        TagDescription<Container[]> description = TagDescriptionBuilder.containerVector("test").build();

        Container[] expectedContainerVector = new Container[]{
                Container.of("a", Variant.ofInteger(1))
        };

        Container[] extractedContainerVector = ContainerUtil.extract(
                Container.of("test", Variant.ofVector(Vector.ofContainers(expectedContainerVector))),
                description
        );

        HerculesProtocolAssert.assertArrayEquals(expectedContainerVector, extractedContainerVector, HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldApplyConvert() throws Exception {
        TagDescription<String> description = TagDescriptionBuilder.parsable("test", s -> s + s).build();

        String extracted = ContainerUtil.extract(
                Container.of("test", Variant.ofString("value")),
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
                Container.empty(),
                description
        );

        assertThat(extracted, is("Default value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionInCaseOfAbsenceOfRequiredValue() throws Exception {
        TagDescription<String> description = TagDescriptionBuilder.string("test").build();

        ContainerUtil.extract(
                Container.empty(),
                description
        );
    }
}
