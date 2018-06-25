package ru.kontur.vostok.hercules.meta.timeline;

public class TimelineUtil {

    public static long calculateTimetrapOffset(long timestamp, long timetrapSize) {
        return timestamp - (timestamp % timetrapSize);
    }

    public static long calculateNextTimetrapOffset(long timestamp, long timetrapSize) {
        return calculateTimetrapOffset(timestamp, timetrapSize) + timetrapSize;
    }
}
