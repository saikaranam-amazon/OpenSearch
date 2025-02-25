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

package org.opensearch.painless.ir;

import org.opensearch.painless.ClassWriter;
import org.opensearch.painless.DefBootstrap;
import org.opensearch.painless.Location;
import org.opensearch.painless.MethodWriter;
import org.opensearch.painless.lookup.PainlessLookupUtility;
import org.opensearch.painless.lookup.def;
import org.opensearch.painless.phase.IRTreeVisitor;
import org.opensearch.painless.symbol.WriteScope;
import org.objectweb.asm.Type;

public class LoadBraceDefNode extends ExpressionNode {

    private Class<?> indexType;

    public void setIndexType(Class<?> indexType) {
        this.indexType = indexType;
    }

    public Class<?> getIndexType() {
        return indexType;
    }

    public String getIndexCanonicalTypeName() {
        return PainlessLookupUtility.typeToCanonicalTypeName(indexType);
    }

    /* ---- end node data, begin visitor ---- */

    @Override
    public <Scope> void visit(IRTreeVisitor<Scope> irTreeVisitor, Scope scope) {
        irTreeVisitor.visitLoadBraceDef(this, scope);
    }

    @Override
    public <Scope> void visitChildren(IRTreeVisitor<Scope> irTreeVisitor, Scope scope) {
        // do nothing; terminal node
    }

    /* ---- end visitor ---- */

    public LoadBraceDefNode(Location location) {
        super(location);
    }

    @Override
    protected void write(ClassWriter classWriter, MethodWriter methodWriter, WriteScope writeScope) {
        methodWriter.writeDebugInfo(getLocation());
        Type methodType = Type.getMethodType(
                MethodWriter.getType(getExpressionType()),
                MethodWriter.getType(def.class),
                MethodWriter.getType(indexType));
        methodWriter.invokeDefCall("arrayLoad", methodType, DefBootstrap.ARRAY_LOAD);
    }
}
