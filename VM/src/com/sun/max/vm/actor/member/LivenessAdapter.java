/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.actor.member;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.stackmap.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.types.*;

/**
 * Adapts liveness information in a {@link StackMapTable} to the {@linkplain RiMethod#livenessMap() format}
 * used by C1X.
 *
 * @author Doug Simon
 */
public final class LivenessAdapter implements FrameModel {
    private int activeLocals;
    private CiBitMap locals;

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
                SignatureDescriptor signature = method.descriptor();
                if (!method.isStatic()) {
                    locals.set(0);
                    activeLocals = 1;
                }
                for (int i = 0; i < signature.numberOfParameters(); ++i) {
                    final TypeDescriptor parameter = signature.parameterDescriptorAt(i);
                    final Kind parameterKind = parameter.toKind();
                    locals.set(activeLocals);
                    activeLocals += parameterKind.stackSlots;
                }

                livenessMap[0] = locals.copy();
                int previousPos = -1;
                for (StackMapFrame frame : stackMapTable.getFrames(null)) {
                    frame.applyTo(this);
                    int pos = frame.getPosition(previousPos);
                    livenessMap[pos] = locals.copy();
                    previousPos = pos;
                }
            }
        }
        this.livenessMap = livenessMap;
        assert livenessMap.length != 0 || livenessMap == NO_LIVENESS_MAP;
    }

    public void store(VerificationType type, int index) {
        locals.set(index);
        if (type.isCategory2()) {
            locals.clear(index + 1);
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
        activeLocals = 0;
    }

    public void chopLocals(int numberOfLocals) {
        for (int i = 0; i < numberOfLocals; i++) {
            locals.clear(--activeLocals);
        }
    }

    public int activeLocals() {
        return activeLocals;
    }
}
