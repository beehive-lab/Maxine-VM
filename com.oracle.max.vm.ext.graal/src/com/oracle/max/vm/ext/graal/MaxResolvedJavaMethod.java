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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.api.meta.ProfilingInfo.ExceptionSeen;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;

/**
 * Likely temporary indirect between a {@link MethodActor} and a {@link ResolvedJavaMethod},
 * since {@code MethodActor} already implements the old {@link RiResolvedMethod} interface.
 */
public class MaxResolvedJavaMethod extends MaxJavaMethod implements ResolvedJavaMethod {

    protected MaxResolvedJavaMethod(RiResolvedMethod riResolvedMethod) {
        super(riResolvedMethod);
    }

    RiResolvedMethod riResolvedMethod() {
        return (RiResolvedMethod) riMethod;
    }

    static MaxResolvedJavaMethod get(RiResolvedMethod riMethod) {
        return (MaxResolvedJavaMethod) MaxJavaMethod.get(riMethod);
    }

    public static RiResolvedMethod getRiResolvedMethod(ResolvedJavaMethod resolvedJavaMethod) {
        return (RiResolvedMethod) MaxJavaMethod.getRiMethod(resolvedJavaMethod);
    }

    @Override
    public byte[] getCode() {
        return riResolvedMethod().code();
    }

    @Override
    public int getCodeSize() {
        return riResolvedMethod().codeSize();
    }

    @Override
    public int getCompiledCodeSize() {
        // TODO implement properly
        return 0;
    }

    @Override
    public int getCompilationComplexity() {
        // TODO implement properly
        return 0;
    }

    @Override
    public ResolvedJavaType getDeclaringClass() {
        return MaxResolvedJavaType.get(riResolvedMethod().holder());
    }

    @Override
    public int getMaxLocals() {
        return riResolvedMethod().maxLocals();
    }

    @Override
    public int getMaxStackSize() {
        return riResolvedMethod().maxStackSize();
    }

    @Override
    public int getModifiers() {
        return riResolvedMethod().accessFlags();
    }

    @Override
    public boolean isClassInitializer() {
        return riResolvedMethod().isClassInitializer();
    }

    @Override
    public boolean isConstructor() {
        return riResolvedMethod().isConstructor();
    }

    @Override
    public boolean canBeStaticallyBound() {
        return riResolvedMethod().canBeStaticallyBound();
    }

    @Override
    public ExceptionHandler[] getExceptionHandlers() {
        RiExceptionHandler[] riExHandlers = riResolvedMethod().exceptionHandlers();
        ExceptionHandler[] exHandlers = new ExceptionHandler[riExHandlers.length];
        for (int i = 0; i < riExHandlers.length; i++) {
            RiExceptionHandler riEx = riExHandlers[i];
            exHandlers[i] = new ExceptionHandler(riEx.startBCI(), riEx.endBCI(), riEx.handlerBCI(),
                            riEx.catchTypeCPI(), MaxJavaType.get(riEx.catchType()));
        }
        return exHandlers;
    }

    @Override
    public StackTraceElement asStackTraceElement(int bci) {
        ClassMethodActor cma = (ClassMethodActor) riMethod;
        return new StackTraceElement(cma.format("%H"), riResolvedMethod().name(), cma.sourceFileName(), cma.codeAttribute().lineNumberTable().findLineNumber(bci));
    }

    @Override
    public ProfilingInfo getProfilingInfo() {
        // We do not want to deal with exception handling right now, so just assume nothing throws an exception...
        return DefaultProfilingInfo.get(ExceptionSeen.FALSE);
    }

    private final Map<Object, Object> compilerStorage = new ConcurrentHashMap<Object, Object>();

    @Override
    public Map<Object, Object> getCompilerStorage() {
        return compilerStorage;
    }

    @Override
    public ConstantPool getConstantPool() {
        return MaxConstantPool.get(riResolvedMethod().getConstantPool());
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return riResolvedMethod().getAnnotation(annotationClass);
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return riResolvedMethod().getParameterAnnotations();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return riResolvedMethod().getGenericParameterTypes();
    }

    @Override
    public boolean canBeInlined() {
        return riResolvedMethod().getAnnotation(NEVER_INLINE.class) == null;
    }

    @Override
    public String toString() {
        return riMethod.toString();
    }

    private static Map<RiMethod, LineNumberTable> lineNumberTableMap = new HashMap<>();

    private static class LineNumberTableImpl implements LineNumberTable {
        int[] lineNumberEntries;
        int[] bciEntries;
        com.sun.max.vm.classfile.LineNumberTable maxLnt;

        LineNumberTableImpl(com.sun.max.vm.classfile.LineNumberTable maxLnt) {
            this.maxLnt = maxLnt;
            com.sun.max.vm.classfile.LineNumberTable.Entry[] entries = maxLnt.entries();
            lineNumberEntries = new int[entries.length];
            bciEntries = new int[entries.length];
            int i = 0;
            for (com.sun.max.vm.classfile.LineNumberTable.Entry entry : entries) {
                lineNumberEntries[i] = entry.lineNumber();
                bciEntries[i] = entry.bci();
                i++;
            }
        }

        @Override
        public int[] getLineNumberEntries() {
            return lineNumberEntries;
        }

        @Override
        public int[] getBciEntries() {
            return bciEntries;
        }

        @Override
        public int getLineNumber(int bci) {
            return maxLnt.findLineNumber(bci);
        }

    }

    @Override
    public LineNumberTable getLineNumberTable() {
        LineNumberTable lnt = lineNumberTableMap.get(riMethod);
        if (lnt == null) {
            ClassMethodActor cma = (ClassMethodActor) riResolvedMethod();
            com.sun.max.vm.classfile.LineNumberTable maxLnt = cma.codeAttribute().lineNumberTable();
            lnt = new LineNumberTableImpl(maxLnt);
            lineNumberTableMap.put(riMethod, lnt);
        }
        return lnt;
    }

    private static Map<RiMethod, LocalVariableTable> localVariableTableMap = new HashMap<>();

    private static class LocalVariableTableImpl implements LocalVariableTable {

        private static class LocalImpl implements Local {

            com.sun.max.vm.classfile.LocalVariableTable.Entry entry;

            LocalImpl(com.sun.max.vm.classfile.LocalVariableTable.Entry entry) {
                this.entry = entry;
            }

            @Override
            public int getStartBCI() {
                return entry.startBCI();
            }

            @Override
            public int getEndBCI() {
                /// TODO
                MaxGraal.unimplemented("Local.getEndBCI");
                return 0;
            }

            @Override
            public int getSlot() {
                return entry.slot();
            }

            @Override
            public String getName() {
                /// TODO
                MaxGraal.unimplemented("Local.getName");
                return null;
            }

            @Override
            public ResolvedJavaType getType() {
                /// TODO
                MaxGraal.unimplemented("Local.getType");
                return null;
            }

        }

        private com.sun.max.vm.classfile.LocalVariableTable maxLvt;

        LocalVariableTableImpl(com.sun.max.vm.classfile.LocalVariableTable maxLvt) {
            this.maxLvt = maxLvt;
        }

        @Override
        public Local[] getLocals() {
            com.sun.max.vm.classfile.LocalVariableTable.Entry[] entries = maxLvt.entries();
            LocalImpl[] result = new LocalImpl[entries.length];
            for (int i = 0; i < entries.length; i++) {
                result[i] = new LocalImpl(entries[i]);
            }
            return result;
        }

        @Override
        public Local[] getLocalsAt(int bci) {
            // TODO
            MaxGraal.unimplemented("LocalVariableTable.getLocalsAt");
            return null;
        }

        @Override
        public Local getLocal(int slot, int bci) {
            com.sun.max.vm.classfile.LocalVariableTable.Entry entry = maxLvt.findLocalVariable(slot, bci);
            if (entry == null) {
                return null;
            }
            return new LocalImpl(entry);
        }

    }

    @Override
    public LocalVariableTable getLocalVariableTable() {
        LocalVariableTable lvt = localVariableTableMap.get(riMethod);
        if (lvt == null) {
            ClassMethodActor cma = (ClassMethodActor) riResolvedMethod();
            com.sun.max.vm.classfile.LocalVariableTable maxLvt = cma.codeAttribute().localVariableTable();
            lvt = new LocalVariableTableImpl(maxLvt);
            localVariableTableMap.put(riMethod, lvt);
        }
        return lvt;
    }

}
