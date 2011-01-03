/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir;

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
            writer.println("maxStack: " + codeAttribute.maxStack);
            writer.println("maxLocals: " + codeAttribute.maxLocals);
            writer.outdent();
            final ConstantPool constantPool = codeAttribute.constantPool;
            writer.print(BytecodePrinter.toString(constantPool, new BytecodeBlock(codeAttribute.code())));
            if (codeAttribute.exceptionHandlerTable().length != 0) {
                writer.println("Exception handlers:");
                for (ExceptionHandlerEntry entry : codeAttribute.exceptionHandlerTable()) {
                    writer.println(entry.toString());
                }
            }
        }
        return charArrayWriter.toString();
    }
}
