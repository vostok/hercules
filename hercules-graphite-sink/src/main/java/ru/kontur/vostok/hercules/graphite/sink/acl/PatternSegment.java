package ru.kontur.vostok.hercules.graphite.sink.acl;

/**
 * Class for preprocessing a pattern segment
 * <p>
 * segment - pattern segment;<br>
 * starPosition - position in the pattern segment where the {@code *} character is located,
 * if {@code *} is missing, then value will be -1;<br>
 * prefix - substring in the pattern segment before the {@code *},
 * if {@code *} is missing or is the first character in a segment, then value will be "";<br>
 * suffix - substring in the pattern segment after the {@code *},
 * if {@code *} is missing or is the last character in a segment, then value will be "".
 *
 * @author Vladimir Tsypaev
 */
public class PatternSegment {
    private static final String STAR = "*";

    private final String segment;
    private final int starPosition;
    private final String prefix;
    private final String suffix;

    public PatternSegment(String segment) {
        this.segment = segment;
        this.starPosition = segment.indexOf(STAR);
        this.prefix = starPosition != -1 ? segment.substring(0, starPosition) : "";
        this.suffix = starPosition != -1 ? segment.substring(starPosition + 1) : "";
    }

    public String getSegment() {
        return segment;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean isStar() {
        return segment.equals(STAR);
    }

    public boolean containsStar() {
        return starPosition != -1;
    }

    public boolean startsWithStar() {
        return starPosition == 0;
    }

    public boolean endsWithStar() {
        return segment.length() > 0 && starPosition == segment.length() - 1;
    }
}
