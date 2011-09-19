/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.handlers;

import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.MethodCommands.*;
import com.sun.max.jdwp.protocol.MethodCommands.LineTable.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class MethodHandlers extends Handlers {

    public MethodHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new LineTableHandler());
        registry.addCommandHandler(new VariableTableHandler());
        registry.addCommandHandler(new BytecodesHandler());
        registry.addCommandHandler(new IsObsoleteHandler());
        registry.addCommandHandler(new VariableTableWithGenericHandler());
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_Method_LineTable"> JDWP
     * specification for command Method-LineTable.</a>
     */
    private class LineTableHandler extends MethodCommands.LineTable.Handler {

        @Override
        public LineTable.Reply handle(LineTable.IncomingRequest incomingRequest) throws JDWPException {
            final MethodProvider method = session().getMethod(incomingRequest.refType, incomingRequest.methodID);

            final Reply reply = new Reply();
            reply.start = -1;
            reply.end = -1;
            reply.lines = new LineInfo[0];

            final LineTableEntry[] entries = method.getLineTable();
            if (entries.length > 0) {
                long min = Integer.MAX_VALUE;
                long max = -1;
                reply.lines = new LineInfo[entries.length];
                for (int i = 0; i < entries.length; i++) {
                    reply.lines[i] = new LineInfo();
                    min = Math.min(min, entries[i].getCodeIndex());
                    max = Math.max(max, entries[i].getCodeIndex());
                    reply.lines[i].lineCodeIndex = entries[i].getCodeIndex();
                    reply.lines[i].lineNumber = entries[i].getLineNumber();
                }

                reply.start = min;
                reply.end = max;
            }

            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_Method_VariableTable">
     * JDWP specification for command Method-VariableTable.</a>
     */
    private class VariableTableHandler extends MethodCommands.VariableTable.Handler {

        @Override
        public VariableTable.Reply handle(VariableTable.IncomingRequest incomingRequest) throws JDWPException {
            final MethodProvider method = session().getMethod(incomingRequest.refType, incomingRequest.methodID);
            final VariableTableEntry[] entries = method.getVariableTable();

            final VariableTable.Reply r = new VariableTable.Reply();
            r.argCnt = method.getNumberOfArguments();
            r.slots = new VariableTable.SlotInfo[entries.length];

            for (int i = 0; i < entries.length; i++) {
                r.slots[i] = new VariableTable.SlotInfo();
                r.slots[i].codeIndex = entries[i].getCodeIndex();
                r.slots[i].length = entries[i].getLength();
                r.slots[i].name = entries[i].getName();
                r.slots[i].signature = entries[i].getSignature();
                r.slots[i].slot = entries[i].getSlot();
            }

            return r;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_Method_Bytecodes"> JDWP
     * specification for command Method-Bytecodes.</a>
     */
    private class BytecodesHandler extends MethodCommands.Bytecodes.Handler {

        @Override
        public Bytecodes.Reply handle(Bytecodes.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider to add implementation.
            throw new JDWPNotImplementedException();
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_Method_IsObsolete"> JDWP
     * specification for command Method-IsObsolete.</a>
     */
    private class IsObsoleteHandler extends MethodCommands.IsObsolete.Handler {

        @Override
        public IsObsolete.Reply handle(IsObsolete.IncomingRequest incomingRequest) {
            // TODO: Return true if method is redefined.
            return new IsObsolete.Reply(false);
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_Method_VariableTableWithGeneric">
     * JDWP specification for command Method-VariableTableWithGeneric.</a>
     */
    private class VariableTableWithGenericHandler extends MethodCommands.VariableTableWithGeneric.Handler {

        @Override
        public VariableTableWithGeneric.Reply handle(VariableTableWithGeneric.IncomingRequest incomingRequest) throws JDWPException {
            final MethodProvider method = session().getMethod(incomingRequest.refType, incomingRequest.methodID);
            final VariableTableEntry[] entries = method.getVariableTable();

            final VariableTableWithGeneric.Reply r = new VariableTableWithGeneric.Reply();
            r.argCnt = method.getNumberOfArguments();
            r.slots = new VariableTableWithGeneric.SlotInfo[entries.length];

            for (int i = 0; i < entries.length; i++) {
                r.slots[i] = new VariableTableWithGeneric.SlotInfo();
                r.slots[i].codeIndex = entries[i].getCodeIndex();
                r.slots[i].length = entries[i].getLength();
                r.slots[i].name = entries[i].getName();
                r.slots[i].signature = entries[i].getSignature();
                r.slots[i].genericSignature = entries[i].getGenericSignature();
                r.slots[i].slot = entries[i].getSlot();
            }

            return r;
        }
    }
}
