package ru.kontur.vostok.hercules.kafka.util.processing.bulk;

import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.kafka.util.processing.SinkStatusFsm;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.Objects;

/**
 * BulkSenderRunnable
 *
 * @author Kirill Sulim
 */
public class BulkSenderRunnable<Key, Value> implements Runnable {

    private final BulkSender<Value> sender;
    private final BulkQueue<Key, Value> queue;
    private final SinkStatusFsm status;

    public BulkSenderRunnable(
            BulkSender<Value> sender,
            BulkQueue<Key, Value> queue,
            SinkStatusFsm status
    ) {
        this.sender = sender;
        this.queue = queue;
        this.status = status;
    }

    @Override
    public void run() {
        while (status.isRunning()) {
            BulkQueue.RunUnit<Key, Value> take = queue.take();
            try {
                if (Objects.nonNull(take)) {
                    BulkSenderStat stat = sender.process(take.getStorage().getRecords());
                    take.getFuture().complete(Result.ok(new BulkQueue.RunResult<>(take.getStorage(), stat)));
                }
            } catch (BackendServiceFailedException e) {
                take.getFuture().complete(Result.error(e));
            }
        }
    }
}
