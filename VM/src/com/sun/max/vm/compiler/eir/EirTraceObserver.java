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
/*VCSID=a0e26583-ae42-4c24-8b19-8ae5166f06d4*/
package com.sun.max.vm.compiler.eir;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;

/**
 * Extends {@link IrTraceObserver} to show traces of each EIR transformation.
 *
 * <p>
 * To enable EIR tracing during compilation at prototyping time or in the target, pass the following system property:
 * <p>
 *
 * <pre>
 *     -Dmax.ir.observers=com.sun.max.vm.compiler.eir.EirTraceObserver
 * </pre>
 *
 * @author Doug Simon
 */
public class EirTraceObserver extends IrTraceObserver {

    public EirTraceObserver() {
    }

    public enum Transformation {
        INITIAL_EIR_CREATION("Initial EIR blocks", 3) {
            @Override
            void traceContext(Object context, IndentWriter writer) {
                traceEirBlocks(context, writer);
            }
        },
        BLOCK_LAYOUT("EIR block layout", 3) {
            @Override
            void traceContext(Object context, IndentWriter writer) {
                traceEirBlocks(context, writer);
            }
        },
        VARIABLE_SPLITTING("Splitting variables", 3) {
            @Override
            void traceContext(Object context, IndentWriter writer) {
                traceEirBlocks(context, writer);
            }
        },
        LIVE_RANGES("Determining live ranges", 3) {
            @Override
            void traceContext(Object context, IndentWriter writer) {
                traceLiveRanges(context, writer);
            }
        },
        INTERFERENCE_GRAPH("Determining interference graph", 3) {
            @Override
            void traceContext(Object context, IndentWriter writer) {
                traceInterferenceGraph(context, writer);
            }
        },
        VERIFY_LIVE_RANGES("Verifying live ranges", 5) {
            @Override
            void traceContext(Object context, IndentWriter writer) {
                traceLiveRanges(context, writer);
            }
        },
        VERIFY_INTERFERENCE_GRAPH("Verifying interference graph", 5) {
            @Override
            void traceContext(Object context, IndentWriter writer) {
                traceInterferenceGraph(context, writer);
            }
        };

        Transformation(String description, int traceLevel) {
            _description = description;
            _traceLevel = traceLevel;
        }

        private final int _traceLevel;
        private final String _description;

        void traceContext(Object context) {
            traceContext(context, _out);
            _out.flush();
        }
        abstract void traceContext(Object context, IndentWriter writer);

        void traceEirBlocks(Object context, IndentWriter writer) {
            final Class<Iterable<EirBlock>> type = null;
            final Iterable<EirBlock> eirBlocks = StaticLoophole.cast(type, context);
            for (EirBlock block : eirBlocks) {
                block.printTo(writer);
            }
        }

        void traceLiveRanges(Object context, IndentWriter writer) {
            final Class<Iterable<EirVariable>> type = null;
            final Iterable<EirVariable> variables = StaticLoophole.cast(type, context);
            for (EirVariable variable : variables) {
                writer.println(variable + " is live at: " + variable.liveRange());
            }
            writer.println();
        }

        void traceInterferenceGraph(Object context, IndentWriter writer) {
            final Class<Iterable<EirVariable>> type = null;
            final Iterable<EirVariable> variables = StaticLoophole.cast(type, context);
            for (EirVariable variable : variables) {
                // Cannot sort variable's by their weight as they may not yet have been calculated: used serial numbers instead
                final Comparator<EirVariable> serialNumberComparator = new Comparator<EirVariable>() {
                    public int compare(EirVariable o1, EirVariable o2) {
                        return o1.serial() - o2.serial();
                    }
                };
                final Set<EirVariable> sortedSet = new TreeSet<EirVariable>(serialNumberComparator);
                sortedSet.addAll(Iterables.toCollection(variable.interferingVariables()));
                writer.println(variable + " interferes with: " + sortedSet);
            }
            writer.println();
        }

        @Override
        public String toString() {
            return _description;
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        final Transformation transformation = (Transformation) transform;
        final int transformTraceLevel = transformation._traceLevel;
        if (hasLevel(transformTraceLevel)) {
            if (irMethod instanceof EirMethod) {
                _out.println(traceString(irMethod, "before transformation: " + transform));
                final EirMethod eirMethod = (EirMethod) irMethod;
                if (context == null || eirMethod.isGenerated()) {
                    _out.println(irMethod.traceToString());
                } else {
                    transformation.traceContext(context);
                }
            } else {
                super.observeBeforeTransformation(irMethod, context, transform);
            }
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        final Transformation transformation = (Transformation) transform;
        final int transformTraceLevel = transformation._traceLevel;
        if (hasLevel(transformTraceLevel)) {
            if (irMethod instanceof EirMethod) {
                _out.println(traceString(irMethod, "after transformation: " + transform));
                final EirMethod eirMethod = (EirMethod) irMethod;
                if (context == null || eirMethod.isGenerated()) {
                    _out.println(irMethod.traceToString());
                } else {
                    transformation.traceContext(context);
                }
            } else {
                super.observeAfterTransformation(irMethod, context, transform);
            }
        }
    }

    @Override
    protected int transformTraceLevel(Object transform) {
        return ((Transformation) transform)._traceLevel;
    }
}
