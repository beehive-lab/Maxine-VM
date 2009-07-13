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
package com.sun.max.vm.compiler.ir;

import java.io.*;

import com.sun.max.io.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * An {@link IrMethod} type that simply wraps the {@linkplain ClassMethodActor#codeAttribute() code} of a
 * {@link ClassMethodActor}.
 *
 * @author Doug Simon
 */
public class ActorIrMethod extends AbstractIrMethod {

    public ActorIrMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor);
    }

    public boolean isGenerated() {
        return true;
    }

    public String traceToString() {
        final CharArrayWriter charArrayWriter = new CharArrayWriter();
        final IndentWriter writer = new IndentWriter(charArrayWriter);
        writer.println("ActorIR: " + name());
        final CodeAttribute codeAttribute = classMethodActor().codeAttribute();
        if (codeAttribute != null) {
            writer.indent();
            writer.println("maxStack: " + codeAttribute.maxStack());
            writer.println("maxLocals: " + codeAttribute.maxLocals());
            writer.outdent();
            final ConstantPool constantPool = codeAttribute.constantPool();
            writer.print(BytecodePrinter.toString(constantPool, new BytecodeBlock(codeAttribute.code())));
            if (!codeAttribute.exceptionHandlerTable().isEmpty()) {
                writer.println("Exception handlers:");
                for (ExceptionHandlerEntry entry : codeAttribute.exceptionHandlerTable()) {
                    writer.println(entry.toString());
                }
            }
        }
        return charArrayWriter.toString();
    }
}
