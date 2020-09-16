package ru.kontur.vostok.hercules.cassandra.sink;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;

/**
 * Builder for batch statements with size in bytes control.
 *
 * Not Thread safe.
 * @author Gregory Koshelev
 */
public class BatchBuilder {
    private final BatchStatementBuilder batchStatementBuilder;
    private int batchSizeBytes;

    public BatchBuilder(CassandraConnector connector) {
        this.batchStatementBuilder = BatchStatement.builder(DefaultBatchType.UNLOGGED);
        this.batchSizeBytes = connector.batchSizeBytesMinimum();
    }

    public int getStatementsCount() {
        return batchStatementBuilder.getStatementsCount();
    }

    public void addStatement(BatchableStatement<?> statement, int statementSizeBytes) {
        batchStatementBuilder.addStatement(statement);
        batchSizeBytes += statementSizeBytes;
    }

    public int getBatchSizeBytes() {
        return batchSizeBytes;
    }

    public BatchStatement build() {
        return batchStatementBuilder.build();
    }
}
