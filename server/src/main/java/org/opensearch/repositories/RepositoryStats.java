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

package org.opensearch.repositories;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RepositoryStats implements Writeable {

    public static final RepositoryStats EMPTY_STATS = new RepositoryStats(Collections.emptyMap());

    public final Map<String, Long> requestCounts;

    public RepositoryStats(Map<String, Long> requestCounts) {
        this.requestCounts = Collections.unmodifiableMap(requestCounts);
    }

    public RepositoryStats(StreamInput in) throws IOException {
        this.requestCounts = in.readMap(StreamInput::readString, StreamInput::readLong);
    }

    public RepositoryStats merge(RepositoryStats otherStats) {
        final Map<String, Long> result = new HashMap<>();
        result.putAll(requestCounts);
        for (Map.Entry<String, Long> entry : otherStats.requestCounts.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(), Math::addExact);
        }
        return new RepositoryStats(result);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeMap(requestCounts, StreamOutput::writeString, StreamOutput::writeLong);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepositoryStats that = (RepositoryStats) o;
        return requestCounts.equals(that.requestCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestCounts);
    }

    @Override
    public String toString() {
        return "RepositoryStats{" +
            "requestCounts=" + requestCounts +
            '}';
    }
}
