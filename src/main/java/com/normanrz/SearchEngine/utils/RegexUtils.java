package com.normanrz.SearchEngine.utils;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by norman on 29.01.16.
 */
public class RegexUtils {

    private RegexUtils() {
    }

    public static Stream<MatchResult> matches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        boolean isNotEmpty = matcher.find();
        Iterator<MatchResult> iterator = new Iterator<MatchResult>() {
            boolean _hasNext = isNotEmpty;

            @Override
            public boolean hasNext() {
                return _hasNext;
            }

            @Override
            public MatchResult next() {
                if (!_hasNext) {
                    return null;
                }
                MatchResult result = matcher.toMatchResult();
                _hasNext = matcher.find();
                return result;
            }
        };

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }


}
