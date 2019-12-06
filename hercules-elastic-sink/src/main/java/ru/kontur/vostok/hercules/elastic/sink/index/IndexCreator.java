package ru.kontur.vostok.hercules.elastic.sink.index;

import org.elasticsearch.client.RestClient;

/**
 * @author Gregory Koshelev
 */
public interface IndexCreator {

    boolean create(String index);

    /**
     * In case of {@link IndexPolicy#DAILY}, an index name should end with date in following format:<br>
     * <pre>
     * YYYY.MM.DD, where
     *     YYYY - year
     *     MM   - month from 1 (January) to 12 (December)
     *     DD   - day of month started with 1
     * </pre>
     *
     * @param policy the index policy
     * @param restClient REST client
     * @return index creator instance
     */
    static IndexCreator forPolicy(IndexPolicy policy, RestClient restClient) {
        switch (policy) {
            case DAILY:
                return new SimpleIndexCreator(restClient);
            case ILM:
                return new IlmIndexCreator(restClient);
                default:
                    throw new IllegalArgumentException("Unknown index policy " + policy);
        }
    }
}
