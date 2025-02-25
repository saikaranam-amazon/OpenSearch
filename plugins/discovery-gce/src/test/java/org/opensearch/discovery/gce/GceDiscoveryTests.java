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

package org.opensearch.discovery.gce;

import org.opensearch.Version;
import org.opensearch.cloud.gce.GceInstancesServiceImpl;
import org.opensearch.cloud.gce.GceMetadataService;
import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.test.transport.MockTransportService;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

/**
 * This test class uses a GCE HTTP Mock system which allows to simulate JSON Responses.
 *
 * To implement a new test you'll need to create an `instances.json` file which contains expected response
 * for a given project-id and zone under the src/test/resources/org/opensearch/discovery/gce with dir name:
 *
 * compute/v1/projects/[project-id]/zones/[zone]
 *
 * By default, project-id is the test method name, lowercase and missing the "test" prefix.
 *
 * For example, if you create a test `myNewAwesomeTest` with following settings:
 *
 * Settings nodeSettings = Settings.builder()
 *  .put(GceComputeService.PROJECT, projectName)
 *  .put(GceComputeService.ZONE, "europe-west1-b")
 *  .build();
 *
 *  You need to create a file under `src/test/resources/org/opensearch/discovery/gce/` named:
 *
 *  compute/v1/projects/mynewawesometest/zones/europe-west1-b/instances.json
 *
 */
public class GceDiscoveryTests extends OpenSearchTestCase {

    protected static ThreadPool threadPool;
    protected MockTransportService transportService;
    protected GceInstancesServiceMock mock;
    protected String projectName;

    @BeforeClass
    public static void createThreadPool() {
        threadPool = new TestThreadPool(GceDiscoveryTests.class.getName());
    }

    @AfterClass
    public static void stopThreadPool() {
        if (threadPool != null) {
            threadPool.shutdownNow();
            threadPool = null;
        }
    }

    @Before
    public void setProjectName() {
        projectName = getTestName().toLowerCase(Locale.ROOT);
        // Slice off the "test" part of the method names so the project names
        if (projectName.startsWith("test")) {
            projectName = projectName.substring("test".length());
        }
    }

    @Before
    public void createTransportService() {
        transportService = MockTransportService.createNewService(Settings.EMPTY, Version.CURRENT, threadPool, null);
    }

    @After
    public void stopGceComputeService() throws IOException {
        if (mock != null) {
            mock.close();
        }
    }

    protected List<TransportAddress> buildDynamicNodes(GceInstancesServiceImpl gceInstancesService, Settings nodeSettings) {
        GceSeedHostsProvider provider = new GceSeedHostsProvider(nodeSettings, gceInstancesService,
            transportService, new NetworkService(Collections.emptyList()));

        List<TransportAddress> dynamicHosts = provider.getSeedAddresses(null);
        logger.info("--> addresses found: {}", dynamicHosts);
        return dynamicHosts;
    }

    public void testNodesWithDifferentTagsAndNoTagSet() {
        Settings nodeSettings = Settings.builder()
                .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
                .put(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "europe-west1-b")
                .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(2));
    }

    public void testNodesWithDifferentTagsAndOneTagSet() {
        Settings nodeSettings = Settings.builder()
                .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
                .put(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "europe-west1-b")
                .putList(GceSeedHostsProvider.TAGS_SETTING.getKey(), "opensearch")
                .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(1));
    }

    public void testNodesWithDifferentTagsAndTwoTagSet() {
        Settings nodeSettings = Settings.builder()
                .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
                .put(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "europe-west1-b")
                .putList(GceSeedHostsProvider.TAGS_SETTING.getKey(), "opensearch", "dev")
                .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(1));
    }

    public void testNodesWithSameTagsAndNoTagSet() {
        Settings nodeSettings = Settings.builder()
                .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
                .put(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "europe-west1-b")
                .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(2));
    }

    public void testNodesWithSameTagsAndOneTagSet() {
        Settings nodeSettings = Settings.builder()
                .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
                .put(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "europe-west1-b")
                .putList(GceSeedHostsProvider.TAGS_SETTING.getKey(), "opensearch")
                .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(2));
    }

    public void testNodesWithSameTagsAndTwoTagsSet() {
        Settings nodeSettings = Settings.builder()
                .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
                .put(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "europe-west1-b")
                .putList(GceSeedHostsProvider.TAGS_SETTING.getKey(), "opensearch", "dev")
                .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(2));
    }

    public void testMultipleZonesAndTwoNodesInSameZone() {
        Settings nodeSettings = Settings.builder()
                .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
                .putList(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "us-central1-a", "europe-west1-b")
                .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(2));
    }

    public void testMultipleZonesAndTwoNodesInDifferentZones() {
        Settings nodeSettings = Settings.builder()
                .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
                .putList(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "us-central1-a", "europe-west1-b")
                .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(2));
    }

    /**
     * For issue https://github.com/elastic/elasticsearch-cloud-gce/issues/43
     */
    public void testZeroNode43() {
        Settings nodeSettings = Settings.builder()
                .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
                .putList(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "us-central1-a", "us-central1-b")
                .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(0));
    }

    public void testIllegalSettingsMissingAllRequired() {
        Settings nodeSettings = Settings.builder()
            // to prevent being resolved using default GCE host
            .put(GceMetadataService.GCE_HOST.getKey(), "http://internal")
            .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        try {
            buildDynamicNodes(mock, nodeSettings);
            fail("We expect an IllegalArgumentException for incomplete settings");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("one or more gce discovery settings are missing."));
        }
    }

    public void testIllegalSettingsMissingProject() {
        Settings nodeSettings = Settings.builder()
            // to prevent being resolved using default GCE host
            .put(GceMetadataService.GCE_HOST.getKey(), "http://internal")
            .putList(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "us-central1-a", "us-central1-b")
            .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        try {
            buildDynamicNodes(mock, nodeSettings);
            fail("We expect an IllegalArgumentException for incomplete settings");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("one or more gce discovery settings are missing."));
        }
    }

    public void testIllegalSettingsMissingZone() {
        Settings nodeSettings = Settings.builder()
            // to prevent being resolved using default GCE host
            .put(GceMetadataService.GCE_HOST.getKey(), "http://internal")
            .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
            .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        try {
            buildDynamicNodes(mock, nodeSettings);
            fail("We expect an IllegalArgumentException for incomplete settings");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("one or more gce discovery settings are missing."));
        }
    }

    /**
     * For issue https://github.com/elastic/elasticsearch/issues/16967:
     * When using multiple regions and one of them has no instance at all, this
     * was producing a NPE as a result.
     */
    public void testNoRegionReturnsEmptyList() {
        Settings nodeSettings = Settings.builder()
            .put(GceInstancesServiceImpl.PROJECT_SETTING.getKey(), projectName)
            .putList(GceInstancesServiceImpl.ZONE_SETTING.getKey(), "europe-west1-b", "us-central1-a")
            .build();
        mock = new GceInstancesServiceMock(nodeSettings);
        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(1));
    }

    public void testMetadataServerValues() {
        Settings nodeSettings = Settings.EMPTY;
        mock = new GceInstancesServiceMock(nodeSettings);
        assertThat(mock.projectId(), not(projectName));

        List<TransportAddress> dynamicHosts = buildDynamicNodes(mock, nodeSettings);
        assertThat(dynamicHosts, hasSize(1));
    }
}
