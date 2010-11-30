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
package com.sun.max.vm.trampoline.compile;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.trampoline.*;

/**
 * Trampoline Generator. The generator generates trampoline by recompiling using the optimizing compiler the same method.
 * Before each compilation, a constant static variable holding the dynamic trampoline object is set so that each compiled
 * trampoline has an appropriate dynamic trampoline object set with the appropriate dispatch table index.
 *
 * @author Laurent Daynes
 *
 */
public abstract class RecompileTrampolineGenerator extends TrampolineGenerator {

    private static final BootstrapCompilerScheme compilerScheme = vmConfig().bootCompilerScheme();

    protected RecompileTrampolineGenerator() {
    }

    public static class VtableTrampolineGenerator extends RecompileTrampolineGenerator {

        protected final ClassMethodActor trampolineClassMethodActor;

        public VtableTrampolineGenerator(ClassMethodActor trampolineClassMethodActor) {
            this.trampolineClassMethodActor = trampolineClassMethodActor;
        }

        @Override
        public DynamicTrampoline createTrampoline(int dispatchTableIndex) {
            final DynamicTrampoline vTableTrampoline = new DynamicTrampoline(dispatchTableIndex, null);
            VTableTrampolineSnippet.fixTrampoline(vTableTrampoline);
            vTableTrampoline.initTrampoline(CompilationScheme.Static.forceFreshCompile(trampolineClassMethodActor));
            return vTableTrampoline;
        }

    }

    public static class ItableTrampolineGenerator extends RecompileTrampolineGenerator  {

        @CONSTANT
        private static DynamicTrampoline iTableTrampoline = new DynamicTrampoline(0, null);
        @INLINE
        public static DynamicTrampoline iTableTrampoline() {
            return iTableTrampoline;
        }
        protected final ClassMethodActor trampolineClassMethodActor;

        public ItableTrampolineGenerator(ClassMethodActor trampolineClassMethodActor) {
            this.trampolineClassMethodActor = trampolineClassMethodActor;
        }
        @Override
        public DynamicTrampoline createTrampoline(int dispatchTableIndex) {
            iTableTrampoline = new DynamicTrampoline(dispatchTableIndex, null);
            iTableTrampoline.initTrampoline(CompilationScheme.Static.forceFreshCompile(trampolineClassMethodActor));
            return iTableTrampoline;
        }
    }
}
