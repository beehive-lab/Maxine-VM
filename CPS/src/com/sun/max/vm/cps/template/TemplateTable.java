/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.template;

import static com.sun.max.vm.stack.VMFrameLayout.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.template.generate.*;
import com.sun.max.vm.runtime.*;

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
