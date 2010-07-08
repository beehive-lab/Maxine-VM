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
package com.sun.max.vm.cps.cir.optimize;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.prototype.*;

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
