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
package com.sun.max.vm.cps.eir;

import java.util.*;

import com.sun.max.*;
import com.sun.max.io.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;

/**
 * Extends {@link IrTraceObserver} to show traces of each EIR transformation.
 *
 * <p>
 * To enable EIR tracing during compilation while bootstrapping or in the target, pass the following system property:
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
        super(EirMethod.class);
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
            this.description = description;
            this.traceLevel = traceLevel;
        }

        private final int traceLevel;
        private final String description;

        void traceContext(Object context) {
            traceContext(context, out);
            out.flush();
        }
        abstract void traceContext(Object context, IndentWriter writer);

        void traceEirBlocks(Object context, IndentWriter writer) {
            final Class<Iterable<EirBlock>> type = null;
            final Iterable<EirBlock> eirBlocks = Utils.cast(type, context);
            for (EirBlock block : eirBlocks) {
                block.printTo(writer);
            }
        }

        void traceLiveRanges(Object context, IndentWriter writer) {
            final Class<Iterable<EirVariable>> type = null;
            final Iterable<EirVariable> variables = Utils.cast(type, context);
            for (EirVariable variable : variables) {
                writer.println(variable + " is live at: " + variable.liveRange());
            }
            writer.println();
        }

        void traceInterferenceGraph(Object context, IndentWriter writer) {
            final Class<Iterable<EirVariable>> type = null;
            final Iterable<EirVariable> variables = Utils.cast(type, context);
            for (EirVariable variable : variables) {
                // Cannot sort variable's by their weight as they may not yet have been calculated: use serial numbers instead
                final Comparator<EirVariable> serialNumberComparator = new Comparator<EirVariable>() {
                    public int compare(EirVariable o1, EirVariable o2) {
                        return o1.serial() - o2.serial();
                    }
                };
                if (variable.interferingVariables() == null) {
                    writer.println(variable + " interferes with:");
                } else {
                    final Set<EirVariable> sortedSet = new TreeSet<EirVariable>(serialNumberComparator);
                    for (EirVariable v : variable.interferingVariables()) {
                        sortedSet.add(v);
                    }
                    writer.println(variable + " interferes with: " + sortedSet);
                }
            }
            writer.println();
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof EirMethod) {
            final Transformation transformation = (Transformation) transform;
            final int transformTraceLevel = transformation.traceLevel;
            if (hasLevel(transformTraceLevel)) {
                if (irMethod instanceof EirMethod) {
                    out.println(traceString(irMethod, "before transformation: " + transform));
                    final EirMethod eirMethod = (EirMethod) irMethod;
                    if (context == null || eirMethod.isGenerated()) {
                        out.println(irMethod.traceToString());
                    } else {
                        transformation.traceContext(context);
                    }
                } else {
                    super.observeBeforeTransformation(irMethod, context, transform);
                }
            }
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof EirMethod) {
            final Transformation transformation = (Transformation) transform;
            final int transformTraceLevel = transformation.traceLevel;
            if (hasLevel(transformTraceLevel)) {
                if (irMethod instanceof EirMethod) {
                    out.println(traceString(irMethod, "after transformation: " + transform));
                    final EirMethod eirMethod = (EirMethod) irMethod;
                    if (context == null || eirMethod.isGenerated()) {
                        out.println(irMethod.traceToString());
                    } else {
                        transformation.traceContext(context);
                    }
                } else {
                    super.observeAfterTransformation(irMethod, context, transform);
                }
            }
        }
    }

    @Override
    protected int transformTraceLevel(Object transform) {
        return ((Transformation) transform).traceLevel;
    }
}
