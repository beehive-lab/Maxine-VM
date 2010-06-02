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
