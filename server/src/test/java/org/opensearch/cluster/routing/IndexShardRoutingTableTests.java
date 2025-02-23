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

package org.opensearch.cluster.routing;

import org.opensearch.index.Index;
import org.opensearch.index.shard.ShardId;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.cluster.routing.IndexShardRoutingTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IndexShardRoutingTableTests extends OpenSearchTestCase {
    public void testEqualsAttributesKey() {
        List<String> attr1 = Arrays.asList("a");
        List<String> attr2 = Arrays.asList("b");
        IndexShardRoutingTable.AttributesKey attributesKey1 = new IndexShardRoutingTable.AttributesKey(attr1);
        IndexShardRoutingTable.AttributesKey attributesKey2 = new IndexShardRoutingTable.AttributesKey(attr1);
        IndexShardRoutingTable.AttributesKey attributesKey3 = new IndexShardRoutingTable.AttributesKey(attr2);
        String s = "Some random other object";
        assertEquals(attributesKey1, attributesKey1);
        assertEquals(attributesKey1, attributesKey2);
        assertNotEquals(attributesKey1, null);
        assertNotEquals(attributesKey1, s);
        assertNotEquals(attributesKey1, attributesKey3);
    }

    public void testEquals() {
        Index index = new Index("a", "b");
        ShardId shardId = new ShardId(index, 1);
        ShardId shardId2 = new ShardId(index, 2);
        IndexShardRoutingTable table1 = new IndexShardRoutingTable(shardId, new ArrayList<>());
        IndexShardRoutingTable table2 = new IndexShardRoutingTable(shardId, new ArrayList<>());
        IndexShardRoutingTable table3 = new IndexShardRoutingTable(shardId2, new ArrayList<>());
        String s = "Some other random object";
        assertEquals(table1, table1);
        assertEquals(table1, table2);
        assertNotEquals(table1, null);
        assertNotEquals(table1, s);
        assertNotEquals(table1, table3);
    }
}
