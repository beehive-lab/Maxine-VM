/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.optimize;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.hosted.*;

/**
 * An aggregate of rules concerning inlining.
 *
 * Reimplememt and exchange this to juggle different inlining strategies.
 *
 * @author Bernd Mathiske
 */
public class CirInliningPolicy {

    private final Class<? extends Accessor> accessorClass;

    /**
     * @return the accessor method holder to select when encountering Accessor interface calls
     */
    public Class<? extends Accessor> accessorClass() {
        return accessorClass;
    }

    public CirInliningPolicy(Class<? extends Accessor> accessorClass) {
        this.accessorClass = accessorClass;
    }

    public boolean isInlineable(CirOptimizer cirOptimizer, CirBlock block, CirValue[] arguments) {
        return block.isInlineable(cirOptimizer, arguments);
    }

    public boolean isInlineable(CirOptimizer cirOptimizer, CirMethod cirMethod, CirValue[] arguments) {
        final ClassMethodActor compilee = cirMethod.classMethodActor().compilee();
        if (compilee.isNeverInline() || compilee.isDeclaredFoldable() || cirMethod.mustNotInline(cirOptimizer, arguments)) {
            return false;
        }
        if (compilee.isInline()) {
            if (MaxineVM.isHosted() && compilee.isInlineAfterSnippetsAreCompiled() && !cirOptimizer.cirGenerator().compilerScheme().areSnippetsCompiled()) {
                return false;
            }
            return true;
        } else if (MaxineVM.isHosted() && CompiledPrototype.forbidCPSCompile(cirMethod.classMethodActor())) {
            // for testing purposes, don't inline methods that are marked to be compiled by the JIT
            return false;
        }
        if (cirMethod.isFoldable(cirOptimizer, arguments)) {
            return false;
        }
        return shouldInline(cirOptimizer, cirMethod, arguments);
    }

    protected boolean shouldInline(CirOptimizer cirOptimizer, CirMethod method, CirValue[] arguments) {
        return false;
    }

    public static final CirInliningPolicy NONE = new CirInliningPolicy(Accessor.class) {
        @Override
        public boolean isInlineable(CirOptimizer cirOptimizer, CirBlock block, CirValue[] arguments) {
            return false; // TODO: find out if this override is correct
        }
    };

    public static class Static extends CirInliningPolicy {
        public Static() {
            super(Accessor.class);
        }

        @Override
        public boolean shouldInline(CirOptimizer cirOptimizer, CirMethod method, CirValue[] arguments) {
            if (MaxineVM.isHosted() && cirOptimizer.hasNoInlining()) {
                // static inlining heuristics have been turned off for this method or class
                return false;
            }
            final boolean result = BytecodeAssessor.hasSmallStraightlineCode(method.classMethodActor());
            if (result) {
                Trace.line(7, "should inline: " + method.classMethodActor().qualifiedName());
            }
            return result;
        }
    }

    public static class Dynamic extends Static {
        public Dynamic() {
            super();
        }
    }

    public static final CirInliningPolicy STATIC = new Static();

    public static final CirInliningPolicy DYNAMIC = new Dynamic();

}
