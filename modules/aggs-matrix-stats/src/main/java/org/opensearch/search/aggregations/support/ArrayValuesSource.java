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

package org.opensearch.search.aggregations.support;

import org.apache.lucene.index.LeafReaderContext;
import org.opensearch.index.fielddata.NumericDoubleValues;
import org.opensearch.search.MultiValueMode;
import org.opensearch.search.aggregations.support.ValuesSource;

import java.io.IOException;
import java.util.Map;

/**
 * Class to encapsulate a set of ValuesSource objects labeled by field name
 */
public abstract class ArrayValuesSource<VS extends ValuesSource> {
    protected MultiValueMode multiValueMode;
    protected String[] names;
    protected VS[] values;

    public static class NumericArrayValuesSource extends ArrayValuesSource<ValuesSource.Numeric> {
        public NumericArrayValuesSource(Map<String, ValuesSource.Numeric> valuesSources, MultiValueMode multiValueMode) {
            super(valuesSources, multiValueMode);
            if (valuesSources != null) {
                this.values = valuesSources.values().toArray(new ValuesSource.Numeric[0]);
            } else {
                this.values = new ValuesSource.Numeric[0];
            }
        }

        public NumericDoubleValues getField(final int ordinal, LeafReaderContext ctx) throws IOException {
            if (ordinal > names.length) {
                throw new IndexOutOfBoundsException("ValuesSource array index " + ordinal + " out of bounds");
            }
            return multiValueMode.select(values[ordinal].doubleValues(ctx));
        }
    }

    public static class BytesArrayValuesSource extends ArrayValuesSource<ValuesSource.Bytes> {
        public BytesArrayValuesSource(Map<String, ValuesSource.Bytes> valuesSources, MultiValueMode multiValueMode) {
            super(valuesSources, multiValueMode);
            this.values = valuesSources.values().toArray(new ValuesSource.Bytes[0]);
        }

        public Object getField(final int ordinal, LeafReaderContext ctx) throws IOException {
            return values[ordinal].bytesValues(ctx);
        }
    }

    public static class GeoPointValuesSource extends ArrayValuesSource<ValuesSource.GeoPoint> {
        public GeoPointValuesSource(Map<String, ValuesSource.GeoPoint> valuesSources, MultiValueMode multiValueMode) {
            super(valuesSources, multiValueMode);
            this.values = valuesSources.values().toArray(new ValuesSource.GeoPoint[0]);
        }
    }

    private ArrayValuesSource(Map<String, ?> valuesSources, MultiValueMode multiValueMode) {
        if (valuesSources != null) {
            this.names = valuesSources.keySet().toArray(new String[0]);
        }
        this.multiValueMode = multiValueMode;
    }

    public boolean needsScores() {
        boolean needsScores = false;
        for (ValuesSource value : values) {
            needsScores |= value.needsScores();
        }
        return needsScores;
    }

    public String[] fieldNames() {
        return this.names;
    }
}
