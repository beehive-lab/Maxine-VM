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

import static com.sun.max.vm.stack.CompiledStackFrameLayout.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.template.generate.*;

/**
 * Table of precompiled {@linkplain BytecodeTemplate templates} for a template based JIT compiler.
 *
 * @author Laurent Daynes
 * @author Doug Simon
 */
public final class TemplateTable {

    public final int maxFrameSlots;
    public final TargetMethod[] templates;

    @HOSTED_ONLY
    public TemplateTable(Class... templateSources) {
        templates = new TargetMethod[BytecodeTemplate.values().length];

        EnumMap<BytecodeTemplate, TargetMethod> templates = new EnumMap<BytecodeTemplate, TargetMethod>(BytecodeTemplate.class) {
            @Override
            public TargetMethod put(BytecodeTemplate key, TargetMethod value) {
                TargetMethod oldValue = super.put(key, value);
                FatalError.check(oldValue == null, "Cannot overwrite existing template for " + key);
                return null;
            }
        };
        final BytecodeTemplateGenerator templateGenerator = new BytecodeTemplateGenerator();
        for (Class <?> templateSource : templateSources) {
            templateGenerator.generateBytecodeTemplates(templateSource, templates);
        }

        for (BytecodeTemplate bt : BytecodeTemplate.values()) {
            TargetMethod code = templates.get(bt);
            this.templates[bt.ordinal()] = code;
        }

        maxFrameSlots = Ints.roundUp(templateGenerator.maxTemplateFrameSize(), STACK_SLOT_SIZE) / STACK_SLOT_SIZE;
    }
}
