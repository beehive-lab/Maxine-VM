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

import com.sun.max.vm.type.*;

/**
 * Select an appropriate template for a bytecode based on validated assumption.
 */
public abstract class TemplateChooser {

    public enum Resolved {
        NO_ASSUMPTION,
        YES, // symbolic references are resolved
        DEFAULT;
    }
    public enum Instrumented {
        NO,
        YES, // bytecode instrumentation is attached
        DEFAULT;
    }
    public enum Initialized {
        NO_ASSUMPTION,
        YES, // operand class is initialized
        DEFAULT;
    }
    public enum Traced {
        NO,
        YES,
        DEFAULT;
    }

    public static class Selector {
        private final Resolved _resolved;
        private final Initialized _initialized;
        private final Instrumented _instrumented;
        private final Traced _traced;

        public Selector(Resolved resolved, Initialized initialized, Instrumented instrumented, Traced traced) {
            _resolved = resolved;
            _initialized = initialized;
            _instrumented = instrumented;
            _traced = traced;
        }
        public boolean matches(Resolved resolved, Initialized initialized, Instrumented instrumented, Traced traced) {
            return resolved == _resolved && initialized == _initialized && instrumented == _instrumented && traced == _traced;
        }
        public Initialized initialized() {
            return _initialized;
        }
        public Resolved resolved() {
            return _resolved;
        }
        public Instrumented instrumented() {
            return _instrumented;
        }
        public Traced traced() {
            return _traced;
        }
        public Selector copyAndModifySelector(Traced traced) {
            return new Selector(_resolved, _initialized, _instrumented, traced);
        }

        public static final Selector RESOLVED = new Selector(Resolved.YES, Initialized.NO_ASSUMPTION, Instrumented.NO, Traced.NO);
        public static final Selector RESOLVED_INSTRUMENTED = new Selector(Resolved.YES, Initialized.NO_ASSUMPTION, Instrumented.YES, Traced.NO);
        public static final Selector INITIALIZED = new Selector(Resolved.NO_ASSUMPTION, Initialized.YES, Instrumented.NO, Traced.NO);
        public static final Selector NO_ASSUMPTION = new Selector(Resolved.NO_ASSUMPTION, Initialized.NO_ASSUMPTION, Instrumented.NO, Traced.NO);
        public static final Selector INSTRUMENTED = new Selector(Resolved.NO_ASSUMPTION, Initialized.NO_ASSUMPTION, Instrumented.YES, Traced.NO);
        public static final Selector TRACED = new Selector(Resolved.NO_ASSUMPTION, Initialized.NO_ASSUMPTION, Instrumented.NO, Traced.YES);
        public static final Selector TRACED_INSTRUMENTED = new Selector(Resolved.NO_ASSUMPTION, Initialized.NO_ASSUMPTION, Instrumented.YES, Traced.YES);
        public static final Selector DEFAULT = NO_ASSUMPTION;

        @Override
        public String toString() {
            return "Resolved: " + _resolved.toString() + ", Initialized: " + _initialized.toString() + ", Instrumented: " + _instrumented.toString() + ", Traced: " + _traced.toString();
        }
    }

    /**
     * Some bytecodes with symbolic reference as operand are untyped (typically, bytecodes that manipulate fields, or that invoke methods).
     * The type is in this case defined by the operand, and is known at template generation time. A selector for such bytecodes
     * takes an extra-argument specifying the type of the operand (a primitive type).
     *
     * @param kind of the operand
     * @param selector for retrieving the most appropriate template
     * @param instrumented selects an instrumented variant of the template
     * @return the selected template, or {@code null} if the selector fails to match the template
     */
    public abstract CompiledBytecodeTemplate select(Kind kind, Selector selector);

    public abstract TemplateChooser extended(CompiledBytecodeTemplate template);
}
