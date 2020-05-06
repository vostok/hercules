package ru.kontur.vostok.hercules.elastic.adapter.bulk;

import ru.kontur.vostok.hercules.elastic.adapter.bulk.action.IndexAction;
import ru.kontur.vostok.hercules.elastic.adapter.bulk.action.IndexActionReader;
import ru.kontur.vostok.hercules.elastic.adapter.document.DocumentReader;
import ru.kontur.vostok.hercules.util.bytes.ByteUtil;

import java.util.Iterator;
import java.util.Map;

/**
 * Read index requests from bulk request.
 *
 * @author Gregory Koshelev
 */
public final class BulkReader {
    private static final byte NEW_LINE = '\n';

    /**
     * Read index requests from input bulk data.
     * <p>
     * Index requests are connected through new line. Each index request consists of index action {@link IndexAction} and document as follows:
     * <pre>
     *     {"index": {"_index": "<index>", "_type": "<type>"}}\n  // first index action
     *     {"field1: "value1", ...}\n                             // first document
     *     {"index": {"_index": "<index>", "_type": "<type>"}}\n  // second index action
     *     {"field2: "value2", ...}\n                             // second document
     *     ...                                                    // and so on
     * </pre>
     * If no index is specified in index action, then use default index.
     * Also, use default document type if no document type is provided.
     *
     * @param data         input data
     * @param defaultIndex default index name
     * @param defaultType  default document type
     * @return iterator
     */
    public static Iterator<IndexRequest> read(byte[] data, String defaultIndex, String defaultType) {
        return new Itr(data, defaultIndex, defaultType);
    }

    private static class Itr implements Iterator<IndexRequest> {
        private final byte[] data;
        private final String defaultIndex;
        private final String defaultType;

        private IndexRequest next;
        private int nextPosition = 0;

        Itr(byte[] data, String defaultIndex, String defaultType) {
            this.data = data;
            this.defaultIndex = defaultIndex;
            this.defaultType = defaultType;

            prepareNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public IndexRequest next() {
            IndexRequest result = next;

            prepareNext();

            return result;
        }

        private void prepareNext() {
            IndexRequest result = null;

            while (result == null && nextPosition < data.length) {
                IndexAction action = findIndexAction();

                if (action == null) {
                    continue;
                }

                Map<String, Object> document = findDocument();

                if (document == null) {
                    continue;
                }

                result = new IndexRequest(updateDefaults(action), document);
            }

            next = result;
        }

        private IndexAction findIndexAction() {
            IndexAction action = null;

            while (action == null && nextPosition < data.length) {
                int length = lineLength(data, nextPosition);

                action = IndexActionReader.read(data, nextPosition, length);

                nextPosition += length + 1 /* skip '\n' */;
            }

            return action;
        }

        private Map<String, Object> findDocument() {
            Map<String, Object> document = null;

            if (nextPosition < data.length) {
                int length = lineLength(data, nextPosition);

                document = DocumentReader.read(data, nextPosition, length);

                nextPosition += length + 1 /* skip '\n' */;
            }

            return document;
        }

        private IndexAction updateDefaults(IndexAction action) {
            if (action.getIndex() == null) {
                action.setIndex(defaultIndex);
            }
            if (action.getType() == null) {
                action.setType(defaultType);
            }
            return action;
        }
    }

    /**
     * Get line length from the source byte array starting from the offset position.
     * Line end determines by new line symbol {@code '\n'} or end of array.
     * <p>
     * Line length does not include new line symbol.
     *
     * @param bytes  the byte array
     * @param offset the starting offset
     * @return line length
     */
    private static int lineLength(byte[] bytes, int offset) {
        int newlinePosition = ByteUtil.find(bytes, NEW_LINE, offset);
        return ((newlinePosition != -1) ? newlinePosition : bytes.length) - offset;
    }

    private BulkReader() {
        /* static class */
    }
}
