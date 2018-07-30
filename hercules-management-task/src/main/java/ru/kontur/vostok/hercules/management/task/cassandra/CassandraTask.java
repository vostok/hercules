package ru.kontur.vostok.hercules.management.task.cassandra;

/**
 * @author Gregory Koshelev
 */
public class CassandraTask {
    private String table;
    private CassandraTaskType type;

    public CassandraTask(String table, CassandraTaskType type) {
        this.table = table;
        this.type = type;
    }

    public String getTable() {
        return table;
    }
    public void setTable(String table) {
        this.table = table;
    }

    public CassandraTaskType getType() {
        return type;
    }
    public void setType(CassandraTaskType type) {
        this.type = type;
    }
}
