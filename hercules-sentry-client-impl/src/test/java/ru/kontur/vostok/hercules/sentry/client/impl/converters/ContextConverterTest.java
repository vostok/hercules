package ru.kontur.vostok.hercules.sentry.client.impl.converters;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import io.sentry.protocol.App;
import io.sentry.protocol.Browser;
import io.sentry.protocol.Device;
import io.sentry.protocol.Device.DeviceOrientation;
import io.sentry.protocol.Gpu;
import io.sentry.protocol.Gpu.JsonKeys;
import io.sentry.protocol.OperatingSystem;
import io.sentry.protocol.SentryRuntime;
import io.sentry.protocol.User;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

@SuppressWarnings("ConstantConditions")
public class ContextConverterTest {
    @Test
    public void shouldImplicitlyCastIntegerTypes() {
        Map<String, Variant> data = Map.of(
                Device.JsonKeys.MEMORY_SIZE, Variant.ofInteger(123)
        );
        ContextConverter<Device> converter = new ContextConverter<>(Device.class);

        Device context = converter.convert(data);

        Assert.assertEquals(Long.valueOf(123), context.getMemorySize());
    }

    @Test
    public void shouldPutUnknownFieldsInUnknownMap() {
        Map<String, Variant> data = Map.of(
                "some_unknown_field1", Variant.ofInteger(123),
                "some_unknown_field2", Variant.ofString("some_value"),
                "some_unknown_field3", Variant.ofFlag(true)
        );
        ContextConverter<Device> converter = new ContextConverter<>(Device.class);

        Device context = converter.convert(data);

        Assert.assertEquals(123, context.getUnknown().get("some_unknown_field1"));
        Assert.assertEquals("some_value", context.getUnknown().get("some_unknown_field2"));
        Assert.assertEquals(Boolean.TRUE, context.getUnknown().get("some_unknown_field3"));
    }


    @Test
    public void shouldPutKnownFieldsWithInconvertibleTypesInUnknownMap() {
        Map<String, Variant> data = Map.of(
                Device.JsonKeys.SIMULATOR, Variant.ofString("yes"),
                Device.JsonKeys.MEMORY_SIZE, Variant.ofString("huge")
        );
        ContextConverter<Device> converter = new ContextConverter<>(Device.class);

        Device context = converter.convert(data);

        Assert.assertEquals("yes", context.getUnknown().get(Device.JsonKeys.SIMULATOR));
        Assert.assertEquals("huge", context.getUnknown().get(Device.JsonKeys.MEMORY_SIZE));
    }

    @Test
    public void testUser() {
        Map<String, Variant> data = Map.of(
                User.JsonKeys.ID, Variant.ofString("user-id"),
                User.JsonKeys.EMAIL, Variant.ofString("some_user@example.com"),
                User.JsonKeys.USERNAME, Variant.ofString("pupkin"),
                User.JsonKeys.IP_ADDRESS, Variant.ofString("127.0.0.1")
        );
        ContextConverter<User> converter = new ContextConverter<>(User.class);

        User context = converter.convert(data);

        Assert.assertEquals("user-id", context.getId());
        Assert.assertEquals("some_user@example.com", context.getEmail());
        Assert.assertEquals("pupkin", context.getUsername());
        Assert.assertEquals("127.0.0.1", context.getIpAddress());
    }

    @Test
    public void testDevice() {
        Map<String, Variant> data = Map.ofEntries(
                Map.entry(Device.JsonKeys.NAME, Variant.ofString("name")),
                Map.entry(Device.JsonKeys.MANUFACTURER, Variant.ofString("manufacturer")),
                Map.entry(Device.JsonKeys.BRAND, Variant.ofString("brand")),
                Map.entry(Device.JsonKeys.FAMILY, Variant.ofString("family")),
                Map.entry(Device.JsonKeys.MODEL, Variant.ofString("model")),
                Map.entry(Device.JsonKeys.MODEL_ID, Variant.ofString("modelId")),
                Map.entry(Device.JsonKeys.ARCHS, Variant.ofVector(Vector.ofStrings("x86", "arm"))),
                Map.entry(Device.JsonKeys.BATTERY_LEVEL, Variant.ofFloat(85.f)),
                Map.entry(Device.JsonKeys.CHARGING, Variant.ofString("true")),
                Map.entry(Device.JsonKeys.ONLINE, Variant.ofString("false")),
                Map.entry(Device.JsonKeys.ORIENTATION, Variant.ofString(DeviceOrientation.LANDSCAPE.name())),
                Map.entry(Device.JsonKeys.SIMULATOR, Variant.ofString("true")),
                Map.entry(Device.JsonKeys.MEMORY_SIZE, Variant.ofLong(2048)),
                Map.entry(Device.JsonKeys.FREE_MEMORY, Variant.ofInteger(1024)),
                Map.entry(Device.JsonKeys.USABLE_MEMORY, Variant.ofShort((short) 50)),
                Map.entry(Device.JsonKeys.LOW_MEMORY, Variant.ofFlag(false)),
                Map.entry(Device.JsonKeys.STORAGE_SIZE, Variant.ofString("123")),
                Map.entry(Device.JsonKeys.FREE_STORAGE, Variant.ofLong(123L)),
                Map.entry(Device.JsonKeys.EXTERNAL_STORAGE_SIZE, Variant.ofInteger(123)),
                Map.entry(Device.JsonKeys.EXTERNAL_FREE_STORAGE, Variant.ofShort((short) 123)),
                Map.entry(Device.JsonKeys.SCREEN_WIDTH_PIXELS, Variant.ofString("")),
                Map.entry(Device.JsonKeys.SCREEN_HEIGHT_PIXELS, Variant.ofNull()),
                Map.entry(Device.JsonKeys.SCREEN_DENSITY, Variant.ofFloat(1.2f)),
                Map.entry(Device.JsonKeys.SCREEN_DPI, Variant.ofInteger(300)),
                Map.entry(Device.JsonKeys.BOOT_TIME, Variant.ofString("2000-01-23T01:23:45.678")),
                Map.entry(Device.JsonKeys.TIMEZONE, Variant.ofString("Asia/Yekaterinburg")),
                Map.entry(Device.JsonKeys.ID, Variant.ofString("some-id")),
                Map.entry(Device.JsonKeys.LANGUAGE, Variant.ofString("russian")),
                Map.entry(Device.JsonKeys.LOCALE, Variant.ofString("ru_RU")),
                Map.entry(Device.JsonKeys.CONNECTION_TYPE, Variant.ofString("wireless")),
                Map.entry(Device.JsonKeys.BATTERY_TEMPERATURE, Variant.ofString("50.23"))
        );
        ContextConverter<Device> converter = new ContextConverter<>(Device.class);

        Device context = converter.convert(data);

        Assert.assertEquals("name", context.getName());
        Assert.assertEquals("manufacturer", context.getManufacturer());
        Assert.assertEquals("brand", context.getBrand());
        Assert.assertEquals("family", context.getFamily());
        Assert.assertEquals("model", context.getModel());
        Assert.assertEquals("modelId", context.getModelId());
        Assert.assertArrayEquals(new String[]{"x86", "arm"}, context.getArchs());
        Assert.assertEquals(85.f, context.getBatteryLevel(), 0.001f);
        Assert.assertEquals(Boolean.TRUE, context.isCharging());
        Assert.assertEquals(Boolean.FALSE, context.isOnline());
        Assert.assertEquals(DeviceOrientation.LANDSCAPE, context.getOrientation());
        Assert.assertEquals(Boolean.TRUE, context.isSimulator());
        Assert.assertEquals(Long.valueOf(2048L), context.getMemorySize());
        Assert.assertEquals(Long.valueOf(1024L), context.getFreeMemory());
        Assert.assertEquals(Long.valueOf(50L), context.getUsableMemory());
        Assert.assertEquals(Boolean.FALSE, context.isLowMemory());
        Assert.assertEquals(Long.valueOf(123L), context.getStorageSize());
        Assert.assertEquals(Long.valueOf(123L), context.getFreeStorage());
        Assert.assertEquals(Long.valueOf(123L), context.getExternalStorageSize());
        Assert.assertEquals(Long.valueOf(123L), context.getExternalFreeStorage());
        Assert.assertNull(context.getScreenWidthPixels());
        Assert.assertNull(context.getScreenHeightPixels());
        Assert.assertEquals(Float.valueOf(1.2f), context.getScreenDensity());
        Assert.assertEquals(Integer.valueOf(300), context.getScreenDpi());
        Assert.assertEquals(Date.from(Instant.parse("2000-01-23T01:23:45.678Z")), context.getBootTime());
        Assert.assertEquals(TimeZone.getTimeZone("Asia/Yekaterinburg"), context.getTimezone());
        Assert.assertEquals("some-id", context.getId());
        Assert.assertEquals("russian", context.getLanguage());
        Assert.assertEquals("ru_RU", context.getLocale());
        Assert.assertEquals("wireless", context.getConnectionType());
        Assert.assertEquals(Float.valueOf(50.23f), context.getBatteryTemperature());
    }

    @Test
    public void testGpu() {
        Map<String, Variant> data = Map.of(
                JsonKeys.NAME, Variant.ofString("name"),
                JsonKeys.ID, Variant.ofInteger(1),
                JsonKeys.VENDOR_ID, Variant.ofInteger(2),
                JsonKeys.VENDOR_NAME, Variant.ofString("Vendor Inc."),
                JsonKeys.MEMORY_SIZE, Variant.ofInteger(1024),
                JsonKeys.API_TYPE, Variant.ofString("OpenGL"),
                JsonKeys.MULTI_THREADED_RENDERING, Variant.ofFlag(true),
                JsonKeys.VERSION, Variant.ofString("3.0.1234-gamma"),
                JsonKeys.NPOT_SUPPORT, Variant.ofString("yes")
        );
        ContextConverter<Gpu> converter = new ContextConverter<>(Gpu.class);

        Gpu context = converter.convert(data);

        Assert.assertEquals("name", context.getName());
        Assert.assertEquals(Integer.valueOf(1), context.getId());
        Assert.assertEquals(Integer.valueOf(2), context.getVendorId());
        Assert.assertEquals("Vendor Inc.", context.getVendorName());
        Assert.assertEquals(Integer.valueOf(1024), context.getMemorySize());
        Assert.assertEquals("OpenGL", context.getApiType());
        Assert.assertEquals(Boolean.TRUE, context.isMultiThreadedRendering());
        Assert.assertEquals("3.0.1234-gamma", context.getVersion());
        Assert.assertEquals("yes", context.getNpotSupport());
    }

    @Test
    public void testOperatingSystem() {
        Map<String, Variant> data = Map.of(
                OperatingSystem.JsonKeys.NAME, Variant.ofString("Android"),
                OperatingSystem.JsonKeys.VERSION, Variant.ofString("4.4 KitKat"),
                OperatingSystem.JsonKeys.RAW_DESCRIPTION, Variant.ofString("Vendor Android 4.4 KitKat. Build date: 12.12.2012"),
                OperatingSystem.JsonKeys.BUILD, Variant.ofString("274"),
                OperatingSystem.JsonKeys.KERNEL_VERSION, Variant.ofString("4.1"),
                OperatingSystem.JsonKeys.ROOTED, Variant.ofFlag(true)
        );
        ContextConverter<OperatingSystem> converter = new ContextConverter<>(OperatingSystem.class);

        OperatingSystem context = converter.convert(data);
        Assert.assertEquals("Android", context.getName());
        Assert.assertEquals("4.4 KitKat", context.getVersion());
        Assert.assertEquals("Vendor Android 4.4 KitKat. Build date: 12.12.2012", context.getRawDescription());
        Assert.assertEquals("274", context.getBuild());
        Assert.assertEquals("4.1", context.getKernelVersion());
        Assert.assertEquals(Boolean.TRUE, context.isRooted());
    }

    @Test
    public void testApp() {
        Map<String, Variant> data = Map.of(
                App.JsonKeys.APP_IDENTIFIER, Variant.ofString("my-supper-app-01"),
                App.JsonKeys.APP_START_TIME, Variant.ofString("2000-01-23T01:23:45.678+05:00"),
                App.JsonKeys.DEVICE_APP_HASH, Variant.ofString("n7fh10ao0r"),
                App.JsonKeys.BUILD_TYPE, Variant.ofString("SNAPSHOT"),
                App.JsonKeys.APP_NAME, Variant.ofString("My Super App"),
                App.JsonKeys.APP_VERSION, Variant.ofString("1.0-SNAPSHOT"),
                App.JsonKeys.APP_BUILD, Variant.ofString("4612"),
                App.JsonKeys.APP_PERMISSIONS, Variant.ofContainer(Container.builder()
                        .tag("some_key1", Variant.ofString("some_value1"))
                        .tag("some_key2", Variant.ofString("some_value2"))
                        .tag("some_key3", Variant.ofString("some_value3"))
                        .build())
        );
        ContextConverter<App> converter = new ContextConverter<>(App.class);

        App context = converter.convert(data);

        Assert.assertEquals("my-supper-app-01", context.getAppIdentifier());
        Assert.assertEquals(Date.from(ZonedDateTime.parse("2000-01-23T01:23:45.678+05:00").toInstant()), context.getAppStartTime());
        Assert.assertEquals("n7fh10ao0r", context.getDeviceAppHash());
        Assert.assertEquals("SNAPSHOT", context.getBuildType());
        Assert.assertEquals("My Super App", context.getAppName());
        Assert.assertEquals("1.0-SNAPSHOT", context.getAppVersion());
        Assert.assertEquals("4612", context.getAppBuild());
        Map<String, String> permissions = context.getPermissions();
        Assert.assertEquals("some_value1", permissions.get("some_key1"));
        Assert.assertEquals("some_value2", permissions.get("some_key2"));
        Assert.assertEquals("some_value3", permissions.get("some_key3"));
    }

    @Test
    public void testBrowser() {
        Map<String, Variant> data = Map.of(
                Browser.JsonKeys.NAME, Variant.ofString("My Super Browser"),
                Browser.JsonKeys.VERSION, Variant.ofString("100500.0.1")
        );
        ContextConverter<Browser> converter = new ContextConverter<>(Browser.class);

        Browser context = converter.convert(data);

        Assert.assertEquals("My Super Browser", context.getName());
        Assert.assertEquals("100500.0.1", context.getVersion());
    }

    @Test
    public void testSentryRuntime() {
        Map<String, Variant> data = Map.of(
                SentryRuntime.JsonKeys.NAME, Variant.ofString("JRE"),
                SentryRuntime.JsonKeys.VERSION, Variant.ofString("1.8"),
                SentryRuntime.JsonKeys.RAW_DESCRIPTION, Variant.ofString("LibericaJRE 1.8.1234. Build date: 20.01.2020")
        );
        ContextConverter<SentryRuntime> converter = new ContextConverter<>(SentryRuntime.class);

        SentryRuntime context = converter.convert(data);

        Assert.assertEquals("JRE", context.getName());
        Assert.assertEquals("1.8", context.getVersion());
        Assert.assertEquals("LibericaJRE 1.8.1234. Build date: 20.01.2020", context.getRawDescription());
    }
}