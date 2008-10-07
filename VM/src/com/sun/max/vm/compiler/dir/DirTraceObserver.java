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
package com.sun.max.vm.compiler.dir;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;

/**
 * Extends {@link IrTraceObserver} to show traces of each DIR transformation. This class deals with the fact that the
 * DIR blocks are not attached to the enclosing {@link DirMethod} instance until the DIR compilation has completed.
 *
 * <p>
 * To enable DIR tracing during compilation at prototyping time or in the target, pass the following system property:
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
    }

    public enum Transformation {
        BLOCK_UNIFICATION("Unifying DIR blocks", 3);


        Transformation(String description, int traceLevel) {
            _description = description;
            _traceLevel = traceLevel;
        }

        private final int _traceLevel;
        private final String _description;

        @Override
        public String toString() {
            return _description;
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        final int transformTraceLevel = transformTraceLevel(transform);
        if (hasLevel(transformTraceLevel)) {
            if (irMethod instanceof DirMethod) {
                _out.println(traceString(irMethod, "before transformation: " + transform));
                final DirMethod dirMethod = (DirMethod) irMethod;
                if (dirMethod.isGenerated() || context == null) {
                    _out.println(irMethod.traceToString());
                } else {
                    traceDirBlocks(context);
                }
            }
        }
    }

    private void traceDirBlocks(Object context) {
        final Class<Iterable<DirBlock>> type = null;
        final Iterable<DirBlock> dirBlocks = StaticLoophole.cast(type, context);
        for (DirBlock block : dirBlocks) {
            block.printTo(_out);
        }
        _out.flush();
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        final int transformTraceLevel = transformTraceLevel(transform);
        if (Trace.hasLevel(transformTraceLevel)) {
            if (irMethod instanceof DirMethod) {
                _out.println(traceString(irMethod, "after transformation: " + transform));
                final DirMethod dirMethod = (DirMethod) irMethod;
                if (dirMethod.isGenerated() || context == null) {
                    _out.println(irMethod.traceToString());
                } else {
                    traceDirBlocks(context);
                }
            }
        }
    }

    @Override
    protected int transformTraceLevel(Object transform) {
        return ((Transformation) transform)._traceLevel;
    }
}
