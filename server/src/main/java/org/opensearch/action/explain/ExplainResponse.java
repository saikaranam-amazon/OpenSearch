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

package org.opensearch.action.explain;

import org.apache.lucene.search.Explanation;
import org.opensearch.action.ActionResponse;
import org.opensearch.common.ParseField;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.ConstructingObjectParser;
import org.opensearch.common.xcontent.StatusToXContentObject;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.index.get.GetResult;
import org.opensearch.rest.RestStatus;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import static org.opensearch.common.lucene.Lucene.readExplanation;
import static org.opensearch.common.lucene.Lucene.writeExplanation;

/**
 * Response containing the score explanation.
 */
public class ExplainResponse extends ActionResponse implements StatusToXContentObject {

    private static final ParseField _INDEX = new ParseField("_index");
    private static final ParseField _TYPE = new ParseField("_type");
    private static final ParseField _ID = new ParseField("_id");
    private static final ParseField MATCHED = new ParseField("matched");
    private static final ParseField EXPLANATION = new ParseField("explanation");
    private static final ParseField VALUE = new ParseField("value");
    private static final ParseField DESCRIPTION = new ParseField("description");
    private static final ParseField DETAILS = new ParseField("details");
    private static final ParseField GET = new ParseField("get");

    private String index;
    private String type;
    private String id;
    private boolean exists;
    private Explanation explanation;
    private GetResult getResult;

    // TODO(talevy): remove dependency on empty constructor from ExplainResponseTests
    ExplainResponse() {
    }

    public ExplainResponse(String index, String type, String id, boolean exists) {
        this.index = index;
        this.type = type;
        this.id = id;
        this.exists = exists;
    }

    public ExplainResponse(String index, String type, String id, boolean exists, Explanation explanation) {
        this(index, type, id, exists);
        this.explanation = explanation;
    }

    public ExplainResponse(String index, String type, String id, boolean exists, Explanation explanation, GetResult getResult) {
        this(index, type, id, exists, explanation);
        this.getResult = getResult;
    }

    public ExplainResponse(StreamInput in) throws IOException {
        super(in);
        index = in.readString();
        type = in.readString();
        id = in.readString();
        exists = in.readBoolean();
        if (in.readBoolean()) {
            explanation = readExplanation(in);
        }
        if (in.readBoolean()) {
            getResult = new GetResult(in);
        }
    }

    public String getIndex() {
        return index;
    }

    /**
     * @deprecated Types are in the process of being removed.
     */
    @Deprecated
    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Explanation getExplanation() {
        return explanation;
    }

    public boolean isMatch() {
        return explanation != null && explanation.isMatch();
    }

    public boolean hasExplanation() {
        return explanation != null;
    }

    public boolean isExists() {
        return exists;
    }

    public GetResult getGetResult() {
        return getResult;
    }

    @Override
    public RestStatus status() {
        return exists ? RestStatus.OK : RestStatus.NOT_FOUND;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(index);
        out.writeString(type);
        out.writeString(id);
        out.writeBoolean(exists);
        if (explanation == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            writeExplanation(out, explanation);
        }
        if (getResult == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            getResult.writeTo(out);
        }
    }

    private static final ConstructingObjectParser<ExplainResponse, Boolean> PARSER = new ConstructingObjectParser<>("explain", true,
        (arg, exists) -> new ExplainResponse((String) arg[0], (String) arg[1], (String) arg[2], exists, (Explanation) arg[3],
            (GetResult) arg[4]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), _INDEX);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), _TYPE);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), _ID);
        final ConstructingObjectParser<Explanation, Boolean> explanationParser = new ConstructingObjectParser<>("explanation", true,
            arg -> {
                if ((float) arg[0] > 0) {
                    return Explanation.match((float) arg[0], (String) arg[1], (Collection<Explanation>) arg[2]);
                } else {
                    return Explanation.noMatch((String) arg[1], (Collection<Explanation>) arg[2]);
                }
            });
        explanationParser.declareFloat(ConstructingObjectParser.constructorArg(), VALUE);
        explanationParser.declareString(ConstructingObjectParser.constructorArg(), DESCRIPTION);
        explanationParser.declareObjectArray(ConstructingObjectParser.constructorArg(), explanationParser, DETAILS);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), explanationParser, EXPLANATION);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), (p, c) -> GetResult.fromXContentEmbedded(p), GET);
    }

    public static ExplainResponse fromXContent(XContentParser parser, boolean exists) {
        return PARSER.apply(parser, exists);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(_INDEX.getPreferredName(), index);
        builder.field(_TYPE.getPreferredName(), type);
        builder.field(_ID.getPreferredName(), id);
        builder.field(MATCHED.getPreferredName(), isMatch());
        if (hasExplanation()) {
            builder.startObject(EXPLANATION.getPreferredName());
            buildExplanation(builder, explanation);
            builder.endObject();
        }
        if (getResult != null) {
            builder.startObject(GET.getPreferredName());
            getResult.toXContentEmbedded(builder, params);
            builder.endObject();
        }
        builder.endObject();
        return builder;
    }

    private void buildExplanation(XContentBuilder builder, Explanation explanation) throws IOException {
        builder.field(VALUE.getPreferredName(), explanation.getValue());
        builder.field(DESCRIPTION.getPreferredName(), explanation.getDescription());
        Explanation[] innerExps = explanation.getDetails();
        if (innerExps != null) {
            builder.startArray(DETAILS.getPreferredName());
            for (Explanation exp : innerExps) {
                builder.startObject();
                buildExplanation(builder, exp);
                builder.endObject();
            }
            builder.endArray();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ExplainResponse other = (ExplainResponse) obj;
        return index.equals(other.index)
            && type.equals(other.type)
            && id.equals(other.id)
            && Objects.equals(explanation, other.explanation)
            && getResult.isExists() == other.getResult.isExists()
            && Objects.equals(getResult.sourceAsMap(), other.getResult.sourceAsMap())
            && Objects.equals(getResult.getFields(), other.getResult.getFields());
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, type, id, explanation, getResult.isExists(), getResult.sourceAsMap(), getResult.getFields());
    }
}
