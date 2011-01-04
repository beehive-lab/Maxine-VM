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
package com.sun.max.vm.cps.dir;

import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;

/**
 * Extends {@link IrTraceObserver} to show traces of each DIR transformation. This class deals with the fact that the
 * DIR blocks are not attached to the enclosing {@link DirMethod} instance until the DIR compilation has completed.
 *
 * <p>
 * To enable DIR tracing during compilation while bootstrapping or in the target, pass the following system property:
 * <p>
 *
 * <pre>
 *     -Dmax.ir.observers=com.sun.max.vm.compiler.dir.DirTraceObserver
 * </pre>
 *
 * @author Doug Simon
 */
public class DirTraceObserver extends IrTraceObserver {

    public DirTraceObserver() {
        super(DirMethod.class);
    }

    public enum Transformation {
        BLOCK_UNIFICATION("Unifying DIR blocks", 3);

        Transformation(String description, int traceLevel) {
            this.description = description;
            this.traceLevel = traceLevel;
        }

        private final int traceLevel;
        private final String description;

        @Override
        public String toString() {
            return description;
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof DirMethod) {
            final int transformTraceLevel = transformTraceLevel(transform);
            if (hasLevel(transformTraceLevel)) {
                if (irMethod instanceof DirMethod) {
                    out.println(traceString(irMethod, "before transformation: " + transform));
                    final DirMethod dirMethod = (DirMethod) irMethod;
                    if (dirMethod.isGenerated() || context == null) {
                        out.println(irMethod.traceToString());
                    } else {
                        traceDirBlocks(context);
                    }
                }
            }
        }
    }

    private void traceDirBlocks(Object context) {
        final Class<Iterable<DirBlock>> type = null;
        final Iterable<DirBlock> dirBlocks = Utils.cast(type, context);
        for (DirBlock block : dirBlocks) {
            block.printTo(out);
        }
        out.flush();
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof DirMethod) {
            final int transformTraceLevel = transformTraceLevel(transform);
            if (Trace.hasLevel(transformTraceLevel)) {
                if (irMethod instanceof DirMethod) {
                    out.println(traceString(irMethod, "after transformation: " + transform));
                    final DirMethod dirMethod = (DirMethod) irMethod;
                    if (dirMethod.isGenerated() || context == null) {
                        out.println(irMethod.traceToString());
                    } else {
                        traceDirBlocks(context);
                    }
                }
            }
        }
    }

    @Override
    protected int transformTraceLevel(Object transform) {
        return ((Transformation) transform).traceLevel;
    }
}
