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

import static com.oracle.max.vm.ext.graal.MaxGraal.unimplemented;

import com.oracle.graal.api.meta.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;


public class MaxResolvedJavaField extends MaxJavaField implements ResolvedJavaField {

    protected MaxResolvedJavaField(RiResolvedField riField) {
        super(riField);
    }

    private RiResolvedField riResolvedField() {
        return (RiResolvedField) riField;
    }

    public static MaxResolvedJavaField get(RiResolvedField riResolvedField) {
        return (MaxResolvedJavaField) MaxJavaField.get(riResolvedField);
    }

    public static RiResolvedField getRiResolvedField(ResolvedJavaField resolvedJavaField) {
        return (RiResolvedField) MaxJavaField.getRiField(resolvedJavaField);
    }

    @Override
    public ResolvedJavaType getDeclaringClass() {
        return (ResolvedJavaType) super.getDeclaringClass();
    }

    @Override
    public int getModifiers() {
        return riResolvedField().accessFlags();
    }

    @Override
    public boolean isInternal() {
        unimplemented("MaxResolvedJavaField.isInternal");
        return false;
    }

    @Override
    public Constant readConstantValue(Constant receiver) {
        CiConstant ciConstant = riResolvedField().constantValue(ConstantMap.toCi(receiver));
        if (ciConstant != null && ciConstant.kind == CiKind.Object && ciConstant.asObject() instanceof WordUtil.WrappedWord) {
            if (MaxineVM.isHosted() && !MaxGraal.inLowTier()) {
                // Special handling of WrappedWord constants when building the boot image
                // We want to keep the type in the high level phases in case the constant is used as a receiver
                // in a (Word) method invocation (which will assert false unless the type is Object).
            } else {
                WordUtil.WrappedWord wrappedWord = (WordUtil.WrappedWord) ciConstant.asObject();
                ciConstant = wrappedWord.archConstant();
            }
        }
        return ConstantMap.toGraal(ciConstant);
    }

    @Override
    public Constant readValue(Constant receiver) {
        unimplemented("MaxResolvedJavaField.readValue");
        return null;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        unimplemented("MaxResolvedJavaField.getAnnotation");
        return null;
    }

    @Override
    public String toString() {
        return riField.toString();
    }

    @Override
    public boolean isSynthetic() {
        FieldActor fieldActor = (FieldActor) riField;
        return fieldActor.isSynthetic();
    }

}
