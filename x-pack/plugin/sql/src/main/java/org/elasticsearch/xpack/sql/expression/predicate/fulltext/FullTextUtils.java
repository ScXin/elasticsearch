/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.sql.expression.predicate.fulltext;

import org.elasticsearch.common.Strings;
import org.elasticsearch.xpack.sql.expression.predicate.fulltext.FullTextPredicate.Operator;
import org.elasticsearch.xpack.sql.parser.ParsingException;
import org.elasticsearch.xpack.sql.tree.Location;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;

abstract class FullTextUtils {

    private static final String DELIMITER = ";";

    static Map<String, String> parseSettings(String options, Location location) {
        if (!Strings.hasText(options)) {
            return emptyMap();
        }
        String[] list = Strings.delimitedListToStringArray(options, DELIMITER);
        Map<String, String> op = new LinkedHashMap<>(list.length);

        for (String entry : list) {
            String[] split = splitInTwo(entry, "=");
            if (split == null) {
                throw new ParsingException(location, "Cannot parse entry {} in options {}", entry, options);
            }

            String previous = op.put(split[0], split[1]);
            if (previous != null) {
                throw new ParsingException(location, "Duplicate option {} detected in options {}", entry, options);
            }

        }
        return op;
    }

    static Map<String, Float> parseFields(Map<String, String> options, Location location) {
        return parseFields(options.get("fields"), location);
    }

    static Map<String, Float> parseFields(String fieldString, Location location) {
        if (!Strings.hasText(fieldString)) {
            return emptyMap();
        }
        Set<String> fieldNames = Strings.commaDelimitedListToSet(fieldString);
        
        Float defaultBoost = Float.valueOf(1.0f);
        Map<String, Float> fields = new LinkedHashMap<>();
        
        for (String fieldName : fieldNames) {
            if (fieldName.contains("^")) {
                String[] split = splitInTwo(fieldName, "^");
                if (split == null) {
                    fields.put(fieldName, defaultBoost);
                }
                else {
                    try {
                        fields.put(split[0], Float.parseFloat(split[1]));
                    } catch (NumberFormatException nfe) {
                        throw new ParsingException(location, "Cannot parse boosting for {}", fieldName);
                    }
                }
            }
            else {
                fields.put(fieldName, defaultBoost);
            }
        }

        return fields;
    }
    
    private static String[] splitInTwo(String string, String delimiter) {
        String[] split = Strings.split(string, delimiter);
        if (split == null || split.length != 2) {
            return null;
        }
        return split;
    }

    static FullTextPredicate.Operator operator(Map<String, String> options, String key) {
        String value = options.get(key);
        return value != null ? Operator.valueOf(value.toUpperCase(Locale.ROOT)) : null;
    }
}
