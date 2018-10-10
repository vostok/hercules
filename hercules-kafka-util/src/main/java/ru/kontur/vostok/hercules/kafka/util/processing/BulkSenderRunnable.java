package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.util.functional.Result;

/**
 * BulkSenderRunnable
 *
 * @author Kirill Sulim
 */
public class BulkSenderRunnable<Key, Value> implements Runnable {

    private final BulkSender<Value> sender;
    private final BulkQueue<Key, Value> queue;
    private final CommonBulkSinkStatusFsm status;

    public BulkSenderRunnable(
            BulkSender<Value> sender,
            BulkQueue<Key, Value> queue,
            CommonBulkSinkStatusFsm status
    ) {
        this.sender = sender;
        this.queue = queue;
        this.status = status;
    }

    @Override
    public void run() {
        while (status.isRunning()) {
            try {
                BulkQueue.RunUnit<Key, Value> take = queue.take();
                try {
                    if (!take.getFuture().isCancelled()) {
                        BulkSenderStat stat = sender.process(take.getStorage().getRecords());
                        take.getFuture().complete(Result.ok(new BulkQueue.RunResult<>(take.getStorage(), stat)));
                    }
                } catch (BackendServiceFailedException e) {
                    take.getFuture().complete(Result.error(e));
                }
            } catch (InterruptedException e) {
                /*
                 * InterruptedException thrown from queue.take marks that processing stopped
                 */
                break;
            }
        }
    }
}
