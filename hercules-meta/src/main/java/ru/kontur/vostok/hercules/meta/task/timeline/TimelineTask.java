package ru.kontur.vostok.hercules.meta.task.timeline;

import ru.kontur.vostok.hercules.meta.timeline.Timeline;

/**
 * @author Gregory Koshelev
 */
public class TimelineTask {
    private Timeline timeline;
    private TimelineTaskType type;

    public TimelineTask(Timeline timeline, TimelineTaskType type) {
        this.timeline = timeline;
        this.type = type;
    }

    public TimelineTask() {
    }

    public Timeline getTimeline() {
        return timeline;
    }
    public void setTimeline(Timeline timeline) {
        this.timeline = timeline;
    }

    public TimelineTaskType getType() {
        return type;
    }
    public void setType(TimelineTaskType type) {
        this.type = type;
    }
}
