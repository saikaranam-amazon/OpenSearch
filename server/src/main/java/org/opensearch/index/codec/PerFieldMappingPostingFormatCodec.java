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

package org.opensearch.index.codec;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.lucene80.Lucene80DocValuesFormat;
import org.apache.lucene.codecs.lucene87.Lucene87Codec;
import org.opensearch.common.lucene.Lucene;
import org.opensearch.index.mapper.CompletionFieldMapper;
import org.opensearch.index.mapper.MappedFieldType;
import org.opensearch.index.mapper.MapperService;

/**
 * {@link PerFieldMappingPostingFormatCodec This postings format} is the default
 * {@link PostingsFormat} for OpenSearch. It utilizes the
 * {@link MapperService} to lookup a {@link PostingsFormat} per field. This
 * allows users to change the low level postings format for individual fields
 * per index in real time via the mapping API. If no specific postings format is
 * configured for a specific field the default postings format is used.
 */
public class PerFieldMappingPostingFormatCodec extends Lucene87Codec {
    private final Logger logger;
    private final MapperService mapperService;
    private final DocValuesFormat dvFormat = new Lucene80DocValuesFormat(Lucene80DocValuesFormat.Mode.BEST_COMPRESSION);

    static {
        assert Codec.forName(Lucene.LATEST_CODEC).getClass().isAssignableFrom(PerFieldMappingPostingFormatCodec.class) :
            "PerFieldMappingPostingFormatCodec must subclass the latest " + "lucene codec: " + Lucene.LATEST_CODEC;
    }

    public PerFieldMappingPostingFormatCodec(Mode compressionMode, MapperService mapperService, Logger logger) {
        super(compressionMode);
        this.mapperService = mapperService;
        this.logger = logger;
    }

    @Override
    public PostingsFormat getPostingsFormatForField(String field) {
        final MappedFieldType fieldType = mapperService.fieldType(field);
        if (fieldType == null) {
            logger.warn("no index mapper found for field: [{}] returning default postings format", field);
        } else if (fieldType instanceof CompletionFieldMapper.CompletionFieldType) {
            return CompletionFieldMapper.CompletionFieldType.postingsFormat();
        }
        return super.getPostingsFormatForField(field);
    }

    @Override
    public DocValuesFormat getDocValuesFormatForField(String field) {
        return dvFormat;
    }
}
