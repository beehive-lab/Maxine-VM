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

import java.io.*;
import java.util.*;

import com.sun.max.io.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;

/**
 * The result container object for a method compilation to DIR.
 *
 * @author Bernd Mathiske
 * @author Hiroshi Yamauchi
 */
public class DirMethod extends AbstractIrMethod {

    public DirMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor);
    }

    private DirVariable[] parameters = null;
    private List<DirBlock> blocks = null;
    private boolean usesMakeStackVariable;

    public DirVariable[] parameters() {
        return parameters;
    }

    public List<DirBlock> blocks() {
        return blocks;
    }

    public boolean usesMakeStackVariable() {
        return usesMakeStackVariable;
    }

    public void setGenerated(DirVariable[] parameters, List<DirBlock> blocks, boolean usesMakeStackVariable) {
        this.parameters = parameters;
        this.blocks = blocks;
        this.usesMakeStackVariable = usesMakeStackVariable;
    }

    @Override
    public void cleanup() {
        for (DirBlock block : blocks) {
            block.cleanup();
        }
    }

    public boolean isGenerated() {
        return blocks != null;
    }

    public String traceToString() {
        String parametersAsString = " ";
        if (parameters == null) {
            parametersAsString = "null";
        } else {
            for (DirVariable parameter : parameters) {
                parametersAsString += parameter + " ";
            }
        }
        final CharArrayWriter charArrayWriter = new CharArrayWriter();
        final IndentWriter writer = new IndentWriter(charArrayWriter);
        writer.println("DIR(" + parametersAsString + ")");
        if (blocks != null) {
            writer.indent();
            for (DirBlock block : blocks) {
                block.printTo(writer);
            }
            writer.outdent();
        }
        return charArrayWriter.toString();
    }

    @Override
    public Class<? extends IrTraceObserver> irTraceObserverType() {
        return DirTraceObserver.class;
    }

    @Override
    public boolean contains(final Builtin builtin, boolean defaultResult) {
        for (DirBlock block : blocks) {
            for (DirInstruction instruction : block.instructions()) {
                if (instruction instanceof DirBuiltinCall) {
                    final DirBuiltinCall call = (DirBuiltinCall) instruction;
                    if (call.builtin() == builtin) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
