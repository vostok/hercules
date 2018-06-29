package ru.kontur.vostok.hercules.timeline.sink;

import ru.kontur.vostok.hercules.meta.timeline.Timeline;

/**
 * @author Gregory Koshelev
 */
public class TimelineUtil {
    public static String timelineToApplicationId(Timeline timeline) {
        return "hercules.sink.timeline." + timeline.getName();
    }

    public static String timelineToTableName(Timeline timeline) {
        return timeline.getName();
    }
}
