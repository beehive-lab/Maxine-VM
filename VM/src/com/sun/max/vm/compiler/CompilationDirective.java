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
package com.sun.max.vm.compiler;

import com.sun.max.collect.*;
import com.sun.max.vm.jit.*;

/**
 * Provides a mechanism for compiler clients to specify runtime options to the underlying compiler. For example,
 * during trace recording we need to force the {@link JitCompiler} to compile methods with tracing instrumentation.
 *
 * @author Michael Bebenita
 */
public enum CompilationDirective {
    DEFAULT (false, false) {
        @Override
        public Sequence<CompilationDirective> promotableFrom() {
            return ArraySequence.of(JIT, OPT, DEFAULT);
        }
    },

    OPT (false, false) {
        @Override
        public Sequence<CompilationDirective> promotableFrom() {
            return ArraySequence.of(JIT, OPT, DEFAULT);
        }
    },

    STUB (false, false) {
        @Override
        public Sequence<CompilationDirective> promotableFrom() {
            return ArraySequence.of(STUB);
        }
    },

    JIT (true, false) {
        @Override
        public Sequence<CompilationDirective> promotableFrom() {
            return ArraySequence.of(JIT, OPT, DEFAULT);
        }
    },

    TRACE_JIT (true, true);

    /**
     * The compiled method should be compiled using the JIT compiler.
     */
    protected boolean jitOnly;

    public boolean jitOnly() {
        return jitOnly;
    }

    /**
     * The compiled method should include trace instrumentation.
     */
    protected boolean traceInstrument;

    public boolean traceInstrument() {
        return traceInstrument;
    }

    private CompilationDirective(boolean jitOnly, boolean traceInstrument) {
        this.jitOnly = jitOnly;
        this.traceInstrument = traceInstrument;
    }

    public static int count() {
        return VALUES.length();
    }

    public Sequence<CompilationDirective> promotableFrom() {
        return Sequence.Static.empty(CompilationDirective.class);
    }

    public static final IndexedSequence<CompilationDirective> VALUES = new ArraySequence<CompilationDirective>(values());
}
