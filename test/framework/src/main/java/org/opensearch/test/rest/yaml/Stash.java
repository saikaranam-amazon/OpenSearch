/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.test.rest.yaml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.Strings;
import org.opensearch.common.xcontent.ToXContentFragment;
import org.opensearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows to cache the last obtained test response and or part of it within variables
 * that can be used as input values in following requests and assertions.
 */
public class Stash implements ToXContentFragment {
    private static final Pattern EXTENDED_KEY = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Pattern PATH = Pattern.compile("\\$_path");

    private static final Logger logger = LogManager.getLogger(Stash.class);

    public static final Stash EMPTY = new Stash();

    private final Map<String, Object> stash = new HashMap<>();
    private final ObjectPath stashObjectPath = new ObjectPath(stash);

    /**
     * Allows to saved a specific field in the stash as key-value pair
     */
    public void stashValue(String key, Object value) {
        logger.trace("stashing [{}]=[{}]", key, value);
        Object old = stash.put(key, value);
        if (old != null && old != value) {
            logger.trace("replaced stashed value [{}] with same key [{}]", old, key);
        }
    }

    /**
     * Clears the previously stashed values
     */
    public void clear() {
        stash.clear();
    }

    /**
     * Tells whether a particular key needs to be looked up in the stash based on its name.
     * Returns true if the string representation of the key starts with "$", false otherwise
     * The stash contains fields eventually extracted from previous responses that can be reused
     * as arguments for following requests (e.g. scroll_id)
     */
    public boolean containsStashedValue(Object key) {
        if (key == null || false == key instanceof CharSequence) {
            return false;
        }
        String stashKey = key.toString();
        if (false == Strings.hasLength(stashKey)) {
            return false;
        }
        if (stashKey.startsWith("$")) {
            return true;
        }
        return EXTENDED_KEY.matcher(stashKey).find();
    }

    /**
     * Retrieves a value from the current stash.
     * The stash contains fields eventually extracted from previous responses that can be reused
     * as arguments for following requests (e.g. scroll_id)
     */
    public Object getValue(String key) throws IOException {
        if (key.charAt(0) == '$' && key.charAt(1) != '{') {
            return unstash(key.substring(1));
        }
        Matcher matcher = EXTENDED_KEY.matcher(key);
        /*
         * String*Buffer* because that is what the Matcher API takes. In modern versions of java the uncontended synchronization is very,
         * very cheap so that should not be a problem.
         */
        StringBuffer result = new StringBuffer(key.length());
        if (false == matcher.find()) {
            throw new IllegalArgumentException("Doesn't contain any stash keys [" + key + "]");
        }
        do {
            matcher.appendReplacement(result, Matcher.quoteReplacement(unstash(matcher.group(1)).toString()));
        } while (matcher.find());
        matcher.appendTail(result);
        return result.toString();
    }

    private Object unstash(String key) throws IOException {
        Object stashedValue = stashObjectPath.evaluate(key);
        if (stashedValue == null) {
            throw new IllegalArgumentException("stashed value not found for key [" + key + "]");
        }
        return stashedValue;
    }

    /**
     * Goes recursively against each map entry and replaces any string value starting with "$" with its
     * corresponding value retrieved from the stash
     */
    @SuppressWarnings("unchecked") // Safe because we check that all the map keys are string in unstashObject
    public Map<String, Object> replaceStashedValues(Map<String, Object> map) throws IOException {
        return (Map<String, Object>) unstashObject(new ArrayList<>(), map);
    }

    private Object unstashObject(List<Object> path, Object obj) throws IOException {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            List<Object> result = new ArrayList<>();
            int index = 0;
            for (Object o : list) {
                path.add(index++);
                if (containsStashedValue(o)) {
                    result.add(getValue(path, o.toString()));
                } else {
                    result.add(unstashObject(path, o));
                }
                path.remove(path.size() - 1);
            }
            return result;
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = (String) entry.getKey();
                Object value = entry.getValue();
                if (containsStashedValue(key)) {
                    key = getValue(key).toString();
                }
                path.add(key);
                if (containsStashedValue(value)) {
                    value = getValue(path, value.toString());
                } else {
                    value = unstashObject(path, value);
                }
                path.remove(path.size() - 1);
                if (null != result.putIfAbsent(key, value)) {
                    throw new IllegalArgumentException("Unstashing has caused a key conflict! The map is [" + result + "] and the key is ["
                            + entry.getKey() + "] which unstashes to [" + key + "]");
                }
            }
            return result;
        }
        return obj;
    }

    /**
     * Lookup a value from the stash adding support for a special key ({@code $_path}) which
     * returns a string that is the location in the path of the of the object currently being
     * unstashed. This is useful during documentation testing.
     */
    private Object getValue(List<Object> path, String key) throws IOException {
        Matcher matcher = PATH.matcher(key);
        if (false == matcher.find()) {
            return getValue(key);
        }
        StringBuilder pathBuilder = new StringBuilder();
        Iterator<Object> element = path.iterator();
        if (element.hasNext()) {
            pathBuilder.append(element.next().toString().replace(".", "\\."));
            while (element.hasNext()) {
                pathBuilder.append('.');
                pathBuilder.append(element.next().toString().replace(".", "\\."));
            }
        }
        String builtPath = Matcher.quoteReplacement(pathBuilder.toString());
        StringBuffer newKey = new StringBuffer(key.length());
        do {
            matcher.appendReplacement(newKey, builtPath);
        } while (matcher.find());
        matcher.appendTail(newKey);
        return getValue(newKey.toString());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field("stash", stash);
        return builder;
    }
}
