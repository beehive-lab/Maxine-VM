/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.member;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.stackmap.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.types.*;

/**
 * Adapts liveness information in a {@link StackMapTable} to the format
 * used by the compiler interface.
 */
public final class LivenessAdapter implements FrameModel {
    private int activeLocals;
    private CiBitMap locals;
    private boolean[] isSecondDoubleWord;

    public final CiBitMap[] livenessMap;

    public static final CiBitMap[] NO_LIVENESS_MAP = {};

    /**
     * Interprets the parameters in a method's signature to initialize the frame state of the entry block.
     */
    public LivenessAdapter(ClassMethodActor method) {
        CiBitMap[] livenessMap = NO_LIVENESS_MAP;
        CodeAttribute codeAttribute = method.codeAttribute();
        if (codeAttribute != null) {
            StackMapTable stackMapTable = codeAttribute.stackMapTable();
            if (stackMapTable != null) {
                livenessMap = new CiBitMap[codeAttribute.code().length];
                locals = new CiBitMap(codeAttribute.maxLocals);
                isSecondDoubleWord = new boolean[codeAttribute.maxLocals];
                SignatureDescriptor signature = method.descriptor();
                if (!method.isStatic()) {
                    locals.set(0);
                    activeLocals = 1;
                }
                for (int i = 0; i < signature.numberOfParameters(); ++i) {
                    final TypeDescriptor parameter = signature.parameterDescriptorAt(i);
                    final Kind parameterKind = parameter.toKind();
                    locals.set(activeLocals);
                    if (parameterKind.isCategory1) {
                        activeLocals++;
                    } else {
                        activeLocals += 2;
                        isSecondDoubleWord[activeLocals - 1] = true;
                    }
                }

                livenessMap[0] = new CiBitMap(locals.toByteArray());
                int previousPos = -1;
                for (StackMapFrame frame : stackMapTable.getFrames(null)) {
                    int pos = frame.getBCI(previousPos);
                    frame.applyTo(this);
                    livenessMap[pos] = new CiBitMap(locals.toByteArray());
                    previousPos = pos;
                }
            }
        }
        this.livenessMap = livenessMap;
        assert livenessMap.length != 0 || livenessMap == NO_LIVENESS_MAP;
    }

    public void store(VerificationType type, int index) {
        locals.set(index);
        isSecondDoubleWord[index] = false;
        if (type.isCategory2()) {
            locals.clear(index + 1);
            isSecondDoubleWord[index + 1] = true;
            activeLocals = Math.max(activeLocals, index + 2);
        } else {
            activeLocals = Math.max(activeLocals, index + 1);
        }
    }

    public void push(VerificationType type) {
    }

    public void clearStack() {
    }

    public void clear() {
        locals.clearAll();
        Arrays.fill(isSecondDoubleWord, false);
        activeLocals = 0;
    }

    public void chopLocals(int numberOfLocals) {
        for (int i = 0; i < numberOfLocals; i++) {
            if (isSecondDoubleWord[activeLocals - 1]) {
                isSecondDoubleWord[activeLocals - 1] = false;
                activeLocals -= 2;
                locals.clear(activeLocals);
                locals.clear(activeLocals + 1);
            } else {
                locals.clear(--activeLocals);
            }
        }
    }

    public int activeLocals() {
        return activeLocals;
    }
}
