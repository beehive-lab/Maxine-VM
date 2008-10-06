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
/*VCSID=3a9ccf39-fb1a-4ed4-9ac8-b7be9d56220c*/
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

    private final Bytecode _bytecode;
    private final TargetMethod _targetMethod;
    private final TemplateChooser.Initialized _initialized;
    private final TemplateChooser.Resolved _resolved;
    private final TemplateChooser.Instrumented _instrumented;
    private final TemplateChooser.Traced _traced;
    private final Kind _kind;

    public CompiledBytecodeTemplate(TargetMethod targetMethod) {
        final Method javaMethod = targetMethod.classMethodActor().toJava();
        final BYTECODE_TEMPLATE bytecodeAnnotation = javaMethod.getAnnotation(BYTECODE_TEMPLATE.class);


        final TEMPLATE templateAnnotation = javaMethod.getDeclaringClass().getAnnotation(TEMPLATE.class);
        assert templateAnnotation != null : "bytecode template not within a template class";
        _targetMethod = targetMethod;

        if (bytecodeAnnotation == null) {
            _bytecode = Bytecode.valueOf(javaMethod.getName().toUpperCase());
            _kind = Kind.VOID;
            _initialized = templateAnnotation.initialized();
            _resolved = templateAnnotation.resolved();
            _instrumented = templateAnnotation.instrumented();
            _traced = templateAnnotation.traced();
        } else {
            _bytecode = bytecodeAnnotation.bytecode();
            _kind = bytecodeAnnotation.kind().asKind();
            _instrumented = bytecodeAnnotation.instrumented() == TemplateChooser.Instrumented.DEFAULT
                          ? templateAnnotation.instrumented()
                          : bytecodeAnnotation.instrumented();
            _resolved = bytecodeAnnotation.resolved() == TemplateChooser.Resolved.DEFAULT
                      ? templateAnnotation.resolved()
                      : bytecodeAnnotation.resolved();
            _initialized = bytecodeAnnotation.initialized() == TemplateChooser.Initialized.DEFAULT
                         ? templateAnnotation.initialized()
                         : bytecodeAnnotation.initialized();
            _traced = bytecodeAnnotation.traced() == TemplateChooser.Traced.DEFAULT
                    ? templateAnnotation.traced()
                    : bytecodeAnnotation.traced();
        }
    }

    public TargetMethod targetMethod() {
        return _targetMethod;
    }

    public TemplateChooser.Instrumented instrumented() {
        return _instrumented;
    }

    public TemplateChooser.Initialized initialized() {
        return _initialized;
    }

    public TemplateChooser.Resolved resolved() {
        return _resolved;
    }

    public TemplateChooser.Traced traced() {
        return _traced;
    }

    /**
     * Gets the bytecode this template provides an implementation of.
     */
    public Bytecode bytecode() {
        return _bytecode;
    }

    /**
     * Return the {@linkplain Kind kind} (if any) for which this template is specialized.
     * Only field access bytecode are specialized to kind.
     * @return kind for which this template is specialized, {@code null} otherwise
     */
    public Kind kind() {
        return _kind;
    }

    @Override
    public String toString() {
        return "template " + bytecode() + " (" + initialized() + resolved() + instrumented() + " )";
    }
}
