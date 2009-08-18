/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.template;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * A compiled bytecode template.
 *
 * @author Laurent Daynes
 */
public final class CompiledBytecodeTemplate implements BytecodeInfo {

    public final Bytecode bytecode;
    public final ExceptionRangeTargetMethod targetMethod;
    public final TemplateChooser.Initialized initialized;
    public final TemplateChooser.Resolved resolved;
    public final TemplateChooser.Instrumented instrumented;
    public final TemplateChooser.Traced traced;
    public final Kind kind;

    public CompiledBytecodeTemplate(ExceptionRangeTargetMethod targetMethod) {
        assert targetMethod.classMethodActor().isTemplate();
        final BYTECODE_TEMPLATE bytecodeAnnotation = targetMethod.classMethodActor().getAnnotation(BYTECODE_TEMPLATE.class);

        final Method javaMethod = targetMethod.classMethodActor().toJava();

        final TEMPLATE templateAnnotation = javaMethod.getDeclaringClass().getAnnotation(TEMPLATE.class);
        assert templateAnnotation != null : "bytecode template not within a template class";
        this.targetMethod = targetMethod;

        if (bytecodeAnnotation == null) {
            this.bytecode = Bytecode.valueOf(javaMethod.getName().toUpperCase());
            this.kind = Kind.VOID;
            this.initialized = templateAnnotation.initialized();
            this.resolved = templateAnnotation.resolved();
            this.instrumented = templateAnnotation.instrumented();
            this.traced = templateAnnotation.traced();
        } else {
            this.bytecode = bytecodeAnnotation.bytecode();
            this.kind = bytecodeAnnotation.kind().asKind();
            this.instrumented = bytecodeAnnotation.instrumented() == TemplateChooser.Instrumented.DEFAULT
                          ? templateAnnotation.instrumented()
                          : bytecodeAnnotation.instrumented();
            this.resolved = bytecodeAnnotation.resolved() == TemplateChooser.Resolved.DEFAULT
                      ? templateAnnotation.resolved()
                      : bytecodeAnnotation.resolved();
            this.initialized = bytecodeAnnotation.initialized() == TemplateChooser.Initialized.DEFAULT
                         ? templateAnnotation.initialized()
                         : bytecodeAnnotation.initialized();
            this.traced = bytecodeAnnotation.traced() == TemplateChooser.Traced.DEFAULT
                    ? templateAnnotation.traced()
                    : bytecodeAnnotation.traced();
        }
    }

    /**
     * Gets the bytecode this template provides an implementation of.
     */
    public Bytecode bytecode() {
        return bytecode;
    }

    @Override
    public String toString() {
        return "template " + bytecode + " (initialized=" + initialized + ", resolved=" + resolved + ", instrumented=" + instrumented + ")";
    }
}
