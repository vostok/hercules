package ru.kontur.vostok.hercules.management.task;

/**
 * @author Gregory Koshelev
 */
public final class TaskConstants {
    public static final String kafkaTaskTopic = "hercules_management_task_kafka".intern();
    public static final String cassandraTaskTopic = "hercules_management_task_cassandra".intern();

    private TaskConstants() {
    }
}
