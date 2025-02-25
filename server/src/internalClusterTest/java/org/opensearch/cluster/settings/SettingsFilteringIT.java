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

package org.opensearch.cluster.settings;

import org.opensearch.action.admin.cluster.node.info.NodeInfo;
import org.opensearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.Plugin;
import org.opensearch.test.OpenSearchIntegTestCase;
import org.opensearch.test.OpenSearchIntegTestCase.ClusterScope;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.opensearch.action.admin.cluster.node.info.NodesInfoRequest.Metric.SETTINGS;

import static org.opensearch.test.OpenSearchIntegTestCase.Scope.SUITE;
import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@ClusterScope(scope = SUITE, supportsDedicatedMasters = false, numDataNodes = 1)
public class SettingsFilteringIT extends OpenSearchIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(SettingsFilteringPlugin.class);
    }

    public static class SettingsFilteringPlugin extends Plugin {
        public static final Setting<Boolean> SOME_NODE_SETTING =
            Setting.boolSetting("some.node.setting", false, Property.NodeScope, Property.Filtered);
        public static final Setting<Boolean> SOME_OTHER_NODE_SETTING =
            Setting.boolSetting("some.other.node.setting", false, Property.NodeScope);

        @Override
        public Settings additionalSettings() {
            return Settings.builder().put("some.node.setting", true).put("some.other.node.setting", true).build();
        }

        @Override
        public List<Setting<?>> getSettings() {
            return Arrays.asList(SOME_NODE_SETTING,
            SOME_OTHER_NODE_SETTING,
            Setting.groupSetting("index.filter_test.", Property.IndexScope));
        }

        @Override
        public List<String> getSettingsFilter() {
            return Arrays.asList("index.filter_test.foo", "index.filter_test.bar*");
        }
    }

    public void testSettingsFiltering() {
        assertAcked(client().admin().indices().prepareCreate("test-idx").setSettings(Settings.builder()
                .put("filter_test.foo", "test")
                .put("filter_test.bar1", "test")
                .put("filter_test.bar2", "test")
                .put("filter_test.notbar", "test")
                .put("filter_test.notfoo", "test")
                .build()).get());
        GetSettingsResponse response = client().admin().indices().prepareGetSettings("test-idx").get();
        Settings settings = response.getIndexToSettings().get("test-idx");

        assertThat(settings.get("index.filter_test.foo"), nullValue());
        assertThat(settings.get("index.filter_test.bar1"), nullValue());
        assertThat(settings.get("index.filter_test.bar2"), nullValue());
        assertThat(settings.get("index.filter_test.notbar"), equalTo("test"));
        assertThat(settings.get("index.filter_test.notfoo"), equalTo("test"));
    }

    public void testNodeInfoIsFiltered() {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().clear().addMetric(SETTINGS.metricName()).get();
        for(NodeInfo info : nodeInfos.getNodes()) {
            Settings settings = info.getSettings();
            assertNotNull(settings);
            assertNull(settings.get(SettingsFilteringPlugin.SOME_NODE_SETTING.getKey()));
            assertTrue(settings.getAsBoolean(SettingsFilteringPlugin.SOME_OTHER_NODE_SETTING.getKey(), false));
            assertEquals(settings.get("node.name"), info.getNode().getName());
        }
    }
}
