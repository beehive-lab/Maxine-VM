/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.max.vm.ext.graal;

import java.util.concurrent.*;

import com.oracle.graal.api.meta.*;
import com.oracle.max.vm.ext.graal.MaxRuntime.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.*;


public class MaxConstantPool implements ConstantPool {

    private static MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration;

    private RiConstantPool riConstantPool;

    private static ConcurrentHashMap<RiConstantPool, MaxConstantPool> map = new ConcurrentHashMap<RiConstantPool, MaxConstantPool>();


    static MaxConstantPool get(RiConstantPool constantPool) {
        MaxConstantPool result = map.get(constantPool);
        if (result == null) {
            result = new MaxConstantPool(constantPool);
            map.put(constantPool, result);
        }
        return result;
    }

    private MaxConstantPool(RiConstantPool constantPool) {
        this.riConstantPool = constantPool;
    }

    static void setGraphBuilderConfig(MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration) {
        MaxConstantPool.maxSnippetGraphBuilderConfiguration = maxSnippetGraphBuilderConfiguration;
    }

    @Override
    public void loadReferencedType(int cpi, int opcode) {
        // The default eager resolution for snippets can cause a HostOnly errors
        // for a field/method/class access that is guarded by isHosted and so will (eventually) fold away
        try {
            riConstantPool.loadReferencedType(cpi, opcode);
        } catch (HostOnlyFieldError ex) {
            checkHostedError(ex);
        } catch (HostOnlyMethodError ex) {
            checkHostedError(ex);
        } catch (HostOnlyClassError ex) {
            checkHostedError(ex);
        }
    }

    private static void checkHostedError(Error ex) throws Error {
        if (maxSnippetGraphBuilderConfiguration != null) {
            // Ignore during snippet installation
        } else {
            throw ex;
        }
    }

    @Override
    public JavaField lookupField(int cpi, int opcode) {
        return MaxJavaField.get(riConstantPool.lookupField(cpi, opcode));
    }

    @Override
    public JavaMethod lookupMethod(int cpi, int opcode) {
        return MaxJavaMethod.get(riConstantPool.lookupMethod(cpi, opcode));
    }

    @Override
    public JavaType lookupType(int cpi, int opcode) {
        return MaxJavaType.get(riConstantPool.lookupType(cpi, opcode));
    }

    @Override
    public Signature lookupSignature(int cpi) {
        return MaxSignature.get(riConstantPool.lookupSignature(cpi));
    }

    @Override
    public Object lookupConstant(int cpi) {
        Object result = riConstantPool.lookupConstant(cpi);
        if (result instanceof CiConstant) {
            return ConstantMap.toGraal((CiConstant) result);
        } else {
            return MaxJavaType.get((RiType) result);
        }

    }

    @Override
    public Object lookupAppendix(int cpi, int opcode) {
        return null;
    }

    @Override
    public int length() {
        return ((com.sun.max.vm.classfile.constant.ConstantPool) riConstantPool).numberOfConstants();
    }

}
