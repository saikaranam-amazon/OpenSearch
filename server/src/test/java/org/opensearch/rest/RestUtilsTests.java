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
 *    http://www.apache.org/licenses/LICENSE-2.0
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

package org.opensearch.rest;

import org.opensearch.test.OpenSearchTestCase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class RestUtilsTests extends OpenSearchTestCase {

    static char randomDelimiter() {
        return randomBoolean() ? '&' : ';';
    }

    public void testDecodeQueryString() {
        Map<String, String> params = new HashMap<>();

        String uri = "something?test=value";
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(1));
        assertThat(params.get("test"), equalTo("value"));

        params.clear();
        uri = String.format(Locale.ROOT, "something?test=value%ctest1=value1", randomDelimiter());
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(2));
        assertThat(params.get("test"), equalTo("value"));
        assertThat(params.get("test1"), equalTo("value1"));

        params.clear();
        uri = "something";
        RestUtils.decodeQueryString(uri, uri.length(), params);
        assertThat(params.size(), equalTo(0));

        params.clear();
        uri = "something";
        RestUtils.decodeQueryString(uri, -1, params);
        assertThat(params.size(), equalTo(0));
    }

    public void testDecodeQueryStringEdgeCases() {
        Map<String, String> params = new HashMap<>();

        String uri = "something?";
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(0));

        params.clear();
        uri = String.format(Locale.ROOT, "something?%c", randomDelimiter());
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(0));

        params.clear();
        uri = String.format(Locale.ROOT, "something?p=v%c%cp1=v1", randomDelimiter(), randomDelimiter());
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(2));
        assertThat(params.get("p"), equalTo("v"));
        assertThat(params.get("p1"), equalTo("v1"));

        params.clear();
        uri = "something?=";
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(0));

        params.clear();
        uri = String.format(Locale.ROOT, "something?%c=", randomDelimiter());
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(0));

        params.clear();
        uri = "something?a";
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(1));
        assertThat(params.get("a"), equalTo(""));

        params.clear();
        uri = String.format(Locale.ROOT, "something?p=v%ca", randomDelimiter());
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(2));
        assertThat(params.get("a"), equalTo(""));
        assertThat(params.get("p"), equalTo("v"));

        params.clear();
        uri = String.format(Locale.ROOT, "something?p=v%ca%cp1=v1", randomDelimiter(), randomDelimiter());
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(3));
        assertThat(params.get("a"), equalTo(""));
        assertThat(params.get("p"), equalTo("v"));
        assertThat(params.get("p1"), equalTo("v1"));

        params.clear();
        uri = String.format(Locale.ROOT, "something?p=v%ca%cb%cp1=v1", randomDelimiter(), randomDelimiter(), randomDelimiter());
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.size(), equalTo(4));
        assertThat(params.get("a"), equalTo(""));
        assertThat(params.get("b"), equalTo(""));
        assertThat(params.get("p"), equalTo("v"));
        assertThat(params.get("p1"), equalTo("v1"));
    }

    public void testCorsSettingIsARegex() {
        assertCorsSettingRegex("/foo/", Pattern.compile("foo"));
        assertCorsSettingRegex("/.*/", Pattern.compile(".*"));
        assertCorsSettingRegex("/https?:\\/\\/localhost(:[0-9]+)?/", Pattern.compile("https?:\\/\\/localhost(:[0-9]+)?"));
        assertCorsSettingRegexMatches("/https?:\\/\\/localhost(:[0-9]+)?/", true, "http://localhost:9200", "http://localhost:9215",
                "https://localhost:9200", "https://localhost");
        assertCorsSettingRegexMatches("/https?:\\/\\/localhost(:[0-9]+)?/", false, "htt://localhost:9200", "http://localhost:9215/foo",
                "localhost:9215");
        assertCorsSettingRegexIsNull("//");
        assertCorsSettingRegexIsNull("/");
        assertCorsSettingRegexIsNull("/foo");
        assertCorsSettingRegexIsNull("foo");
        assertCorsSettingRegexIsNull("");
    }

    public void testCrazyURL() {
        String host = "example.com";
        Map<String, String> params = new HashMap<>();

        // This is a valid URL
        String uri = String.format(
                Locale.ROOT,
                host + "/:@-._~!$%c'()*+,=;:@-._~!$%c'()*+,=:@-._~!$%c'()*+,==?/?:@-._~!$'()*+,=/?:@-._~!$'()*+,==#/?:@-._~!$%c'()*+,;=",
                randomDelimiter(),
                randomDelimiter(),
                randomDelimiter(),
                randomDelimiter());
        RestUtils.decodeQueryString(uri, uri.indexOf('?') + 1, params);
        assertThat(params.get("/?:@-._~!$'()* ,"), equalTo("/?:@-._~!$'()* ,=="));
        assertThat(params.size(), equalTo(1));
    }

    private void assertCorsSettingRegexIsNull(String settingsValue) {
        assertThat(RestUtils.checkCorsSettingForRegex(settingsValue), is(nullValue()));
    }

    private void assertCorsSettingRegex(String settingsValue, Pattern pattern) {
        assertThat(RestUtils.checkCorsSettingForRegex(settingsValue).toString(), is(pattern.toString()));
    }

    private void assertCorsSettingRegexMatches(String settingsValue, boolean expectMatch, String ... candidates) {
        Pattern pattern = RestUtils.checkCorsSettingForRegex(settingsValue);
        for (String candidate : candidates) {
            assertThat(String.format(Locale.ROOT, "Expected pattern %s to match against %s: %s", settingsValue, candidate, expectMatch),
                    pattern.matcher(candidate).matches(), is(expectMatch));
        }
    }
}
