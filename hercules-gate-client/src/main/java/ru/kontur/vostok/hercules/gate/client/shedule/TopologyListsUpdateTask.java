package ru.kontur.vostok.hercules.gate.client.shedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.gate.client.GreyListTopologyElement;
import ru.kontur.vostok.hercules.util.concurrent.Topology;

import java.util.concurrent.BlockingQueue;

/**
 * @author tsypaev
 */
public class TopologyListsUpdateTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyListsUpdateTask.class);

    private Topology<String> whiteQueue;
    private BlockingQueue<GreyListTopologyElement> greyQueue;
    private long timeForRevival;

    TopologyListsUpdateTask(Topology<String> whiteQueue,
                            BlockingQueue<GreyListTopologyElement> greyQueue,
                            long timeForRevival) {
        this.whiteQueue = whiteQueue;
        this.greyQueue = greyQueue;
        this.timeForRevival = timeForRevival;
    }

    @Override
    public void run() {
        if (greyQueue.isEmpty()) {
            return;
        }

        for (int i = 0; i < greyQueue.size(); i++) {
            GreyListTopologyElement element = greyQueue.peek();
            if (System.currentTimeMillis() - element.getEntryTime() >= timeForRevival) {
                try {
                    GreyListTopologyElement pollElement = greyQueue.take();
                    whiteQueue.add(pollElement.getUrl());
                } catch (InterruptedException e) {
                    LOGGER.warn("Error when take {} from grey list." , element);
                }
            } else {
                return;
            }
        }
    }
}
