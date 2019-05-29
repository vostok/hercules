package ru.kontur.vostok.hercules.gate.client.shedule;

import ru.kontur.vostok.hercules.gate.client.GreyListTopologyElement;
import ru.kontur.vostok.hercules.util.concurrent.Topology;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author tsypaev
 */
public class TopologyListsUpdateTaskScheduler {

    private static final int INITIAL_DELAY = 0;

    public static void executeQueuesUpdateTask(Topology<String> whiteQueue,
                                               BlockingQueue<GreyListTopologyElement> greyQueue,
                                               long timeForRevival,
                                               ScheduledExecutorService executorService) {
        TopologyListsUpdateTask task = new TopologyListsUpdateTask(whiteQueue, greyQueue, timeForRevival);
        executorService.scheduleWithFixedDelay(task, INITIAL_DELAY, timeForRevival, TimeUnit.MILLISECONDS);
    }
}
