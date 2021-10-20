package ru.kontur.vostok.hercules.graphite.adapter.purgatory;

import ru.kontur.vostok.hercules.graphite.adapter.metric.Metric;
import ru.kontur.vostok.hercules.util.text.AsciiUtil;

/**
 * @author Petr Demenev
 */
public class MetricValidator {

    public boolean validate(Metric metric) {
        if (Double.isNaN(metric.value())) {
            return false;
        }

        byte[] metricName = metric.name();
        if (metricName.length == 0) {
            return false;
        }
        for (byte c : metricName) {
            if (!isMetricNameCharacter(c)) {
                return false;
            }
        }

        return true;
    }

    private boolean isMetricNameCharacter(byte c) {
        return AsciiUtil.isAlphaNumeric(c)
                || AsciiUtil.isDot(c)
                || AsciiUtil.isUnderscore(c)
                || AsciiUtil.isHyphen(c)
                || AsciiUtil.isColon(c);
    }
}
