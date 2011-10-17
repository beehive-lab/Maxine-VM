/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.criutils;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * A implementation of {@link RiMethod} for an unresolved method.
 */
public class BaseUnresolvedMethod implements RiMethod {

    public final String name;
    public final RiType holder;
    public final RiSignature signature;
    public Map<Object, Object> compilerStorage;

    public BaseUnresolvedMethod(RiType holder, String name, RiSignature signature) {
        this.name = name;
        this.holder = holder;
        this.signature = signature;
    }

    public String name() {
        return name;
    }

    public String jniSymbol() {
        throw unresolved("jniSymbol()");
    }

    public RiType holder() {
        return holder;
    }

    public RiSignature signature() {
        return signature;
    }

    public byte[] code() {
        throw unresolved("code()");
    }

    public int codeSize() {
        return 0;
    }

    public int maxLocals() {
        throw unresolved("maxLocals()");
    }

    public int maxStackSize() {
        throw unresolved("maxStackSize()");
    }

    public boolean hasBalancedMonitors() {
        throw unresolved("hasBalancedMonitors()");
    }

    public boolean isResolved() {
        return false;
    }

    public int accessFlags() {
        throw unresolved("accessFlags()");
    }

    public boolean isLeafMethod() {
        throw unresolved("isLeafMethod()");
    }

    public boolean isClassInitializer() {
        throw unresolved("isClassInitializer()");
    }

    public boolean isConstructor() {
        throw unresolved("isConstructor()");
    }

    public boolean isOverridden() {
        throw unresolved("isOverridden()");
    }

    public boolean noSafepointPolls() {
        throw unresolved("noSafepoints()");
    }

    public boolean minimalDebugInfo() {
        throw unresolved("minimalDebugInfo()");
    }

    public RiMethodProfile methodData() {
        throw unresolved("methodData()");
    }

    public CiBitMap[] livenessMap() {
        throw unresolved("livenessMap()");
    }

    public boolean canBeStaticallyBound() {
        throw unresolved("canBeStaticallyBound()");
    }

    public RiExceptionHandler[] exceptionHandlers() {
        throw unresolved("exceptionHandlers()");
    }

    private CiUnresolvedException unresolved(String operation) {
        throw new CiUnresolvedException(operation + " not defined for unresolved method " + CiUtil.format("%H.%n(%p)", this));
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    public StackTraceElement toStackTraceElement(int bci) {
        return new StackTraceElement(CiUtil.toJavaName(holder), name, null, -1);
    }

    @Override
    public String toString() {
        return CiUtil.format("%H.%n(%p) [unresolved]", this);
    }

    public RiType accessor() {
        return null;
    }

    public String intrinsic() {
        return null;
    }

    public int invocationCount() {
        return -1;
    }

    public int exceptionProbability(int bci) {
        return -1;
    }

    public RiTypeProfile typeProfile(int bci) {
        return null;
    }

    public double branchProbability(int bci) {
        return -1;
    }

    public double[] switchProbability(int bci) {
        return null;
    }

    public Map<Object, Object> compilerStorage() {
        if (compilerStorage == null) {
            compilerStorage = new HashMap<Object, Object>();
        }
        return compilerStorage;
    }

    @Override
    public int compiledCodeSize() {
        return -1;
    }
}
