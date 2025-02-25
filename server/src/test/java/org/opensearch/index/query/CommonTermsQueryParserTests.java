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

package org.opensearch.index.query;

import org.opensearch.action.search.SearchResponse;
import org.opensearch.test.OpenSearchSingleNodeTestCase;

public class CommonTermsQueryParserTests extends OpenSearchSingleNodeTestCase {
    public void testWhenParsedQueryIsNullNoNullPointerExceptionIsThrown() {
        final String index = "test-index";
        final String type = "test-type";
        client()
                .admin()
                .indices()
                .prepareCreate(index)
                .addMapping(type, "name", "type=text,analyzer=stop")
                .execute()
                .actionGet();
        ensureGreen();

        CommonTermsQueryBuilder commonTermsQueryBuilder =
                new CommonTermsQueryBuilder("name", "the").queryName("query-name");

        // the named query parses to null; we are testing this does not cause a NullPointerException
        SearchResponse response =
                client().prepareSearch(index).setTypes(type).setQuery(commonTermsQueryBuilder).execute().actionGet();

        assertNotNull(response);
        assertEquals(response.getHits().getHits().length, 0);
    }
}
