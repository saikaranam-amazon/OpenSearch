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

package org.opensearch.index.fielddata;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.util.NumericUtils;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class FieldDataTests extends OpenSearchTestCase {

    private static class DummyValues extends AbstractNumericDocValues {

        private final long value;
        private int docID = -1;

        DummyValues(long value) {
            this.value = value;
        }

        @Override
        public boolean advanceExact(int target) throws IOException {
            docID = target;
            return true;
        }

        @Override
        public int docID() {
            return docID;
        }

        @Override
        public long longValue() throws IOException {
            return value;
        }
    }

    public void testSortableLongBitsToDoubles() throws IOException {
        final double value = randomDouble();
        final long valueBits = NumericUtils.doubleToSortableLong(value);

        NumericDocValues values = new DummyValues(valueBits);

        SortedNumericDoubleValues asMultiDoubles = FieldData.sortableLongBitsToDoubles(DocValues.singleton(values));
        NumericDoubleValues asDoubles = FieldData.unwrapSingleton(asMultiDoubles);
        assertNotNull(asDoubles);
        assertTrue(asDoubles.advanceExact(0));
        assertEquals(value, asDoubles.doubleValue(), 0);

        values = new DummyValues(valueBits);
        asMultiDoubles = FieldData.sortableLongBitsToDoubles(DocValues.singleton(values));
        NumericDocValues backToLongs = DocValues.unwrapSingleton(FieldData.toSortableLongBits(asMultiDoubles));
        assertSame(values, backToLongs);

        SortedNumericDocValues multiValues = new AbstractSortedNumericDocValues() {

            @Override
            public boolean advanceExact(int target) throws IOException {
                return true;
            }

            @Override
            public long nextValue() {
                return valueBits;
            }

            @Override
            public int docValueCount() {
                return 1;
            }
        };

        asMultiDoubles = FieldData.sortableLongBitsToDoubles(multiValues);
        assertEquals(value, asMultiDoubles.nextValue(), 0);
        assertSame(multiValues, FieldData.toSortableLongBits(asMultiDoubles));
    }

    public void testDoublesToSortableLongBits() throws IOException {
        final double value = randomDouble();
        final long valueBits = NumericUtils.doubleToSortableLong(value);

        NumericDoubleValues values = new NumericDoubleValues() {
            @Override
            public boolean advanceExact(int doc) throws IOException {
                return true;
            }
            @Override
            public double doubleValue() {
                return value;
            }
        };

        SortedNumericDocValues asMultiLongs = FieldData.toSortableLongBits(FieldData.singleton(values));
        NumericDocValues asLongs = DocValues.unwrapSingleton(asMultiLongs);
        assertNotNull(asLongs);
        assertTrue(asLongs.advanceExact(0));
        assertEquals(valueBits, asLongs.longValue());

        SortedNumericDoubleValues multiValues = new SortedNumericDoubleValues() {
            @Override
            public double nextValue() {
                return value;
            }

            @Override
            public boolean advanceExact(int target) throws IOException {
                return true;
            }

            @Override
            public int docValueCount() {
                return 1;
            }
        };

        asMultiLongs = FieldData.toSortableLongBits(multiValues);
        assertEquals(valueBits, asMultiLongs.nextValue());
        assertSame(multiValues, FieldData.sortableLongBitsToDoubles(asMultiLongs));
    }

    private static NumericDocValues asNumericDocValues(Long... values) {
        return new AbstractNumericDocValues() {

            int docID = -1;

            @Override
            public int docID() {
                return docID;
            }

            @Override
            public boolean advanceExact(int target) throws IOException {
                docID = target;
                return target < values.length && values[target] != null;
            }

            @Override
            public long longValue() throws IOException {
                return values[docID];
            }
        };
    }

    public void testReplaceMissingLongs() throws IOException {
        final NumericDocValues values = asNumericDocValues(null, 3L, 2L, null, 5L, null);
        final NumericDocValues replaced = FieldData.replaceMissing(values, 4);

        assertTrue(replaced.advanceExact(0));
        assertEquals(4L, replaced.longValue());

        assertTrue(replaced.advanceExact(1));
        assertEquals(3L, replaced.longValue());

        assertTrue(replaced.advanceExact(2));
        assertEquals(2L, replaced.longValue());

        assertTrue(replaced.advanceExact(3));
        assertEquals(4L, replaced.longValue());

        assertTrue(replaced.advanceExact(4));
        assertEquals(5L, replaced.longValue());

        assertTrue(replaced.advanceExact(5));
        assertEquals(4L, replaced.longValue());
    }

    private static NumericDoubleValues asNumericDoubleValues(Double... values) {
        return new NumericDoubleValues() {

            int docID = -1;

            @Override
            public boolean advanceExact(int target) throws IOException {
                docID = target;
                return target < values.length && values[target] != null;
            }

            @Override
            public double doubleValue() throws IOException {
                return values[docID];
            }
        };
    }

    public void testReplaceMissingDoubles() throws IOException {
        final NumericDoubleValues values = asNumericDoubleValues(null, 1.3, 1.2, null, 1.5, null);
        final NumericDoubleValues replaced = FieldData.replaceMissing(values, 1.4);

        assertTrue(replaced.advanceExact(0));
        assertEquals(1.4, replaced.doubleValue(), 0d);

        assertTrue(replaced.advanceExact(1));
        assertEquals(1.3, replaced.doubleValue(), 0d);

        assertTrue(replaced.advanceExact(2));
        assertEquals(1.2, replaced.doubleValue(), 0d);

        assertTrue(replaced.advanceExact(3));
        assertEquals(1.4, replaced.doubleValue(), 0d);

        assertTrue(replaced.advanceExact(4));
        assertEquals(1.5, replaced.doubleValue(), 0d);

        assertTrue(replaced.advanceExact(5));
        assertEquals(1.4, replaced.doubleValue(), 0d);
    }
}
