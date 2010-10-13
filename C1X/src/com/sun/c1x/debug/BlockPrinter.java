/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.debug;

import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * Prints a listing for a {@linkplain BlockBegin block}.
 *
 * @author Doug Simon
 */
public class BlockPrinter implements BlockClosure {

    private final InstructionPrinter ip;
    private final boolean cfgOnly;
    private final boolean liveOnly;

    public BlockPrinter(IR ir, InstructionPrinter ip, boolean cfgOnly, boolean liveOnly) {
        this.ip = ip;
        this.cfgOnly = cfgOnly;
        this.liveOnly = liveOnly;
    }

    public void apply(BlockBegin block) {
        if (cfgOnly) {
            ip.printInstruction(block);
            ip.out().println();
        } else {
            printBlock(block, liveOnly);
        }
    }

    public void printBlock(BlockBegin block, boolean liveOnly) {
        ip.printInstruction(block);
        LogStream out = ip.out();
        out.println();
        printStack(block.stateBefore(), out);
        out.println();

        out.println("inlining depth " + block.stateBefore().scope().level);

        ip.printInstructionListingHeader();

        for (Instruction i = block.next(); i != null; i = i.next()) {
            if (!liveOnly || i.isLive()) {
                ip.printInstructionListing(i);
            }
        }
        out.println();

    }

    private static void printStack(FrameState newFrameState, LogStream out) {
        int startPosition = out.position();
        if (newFrameState.stackEmpty()) {
          out.print("empty stack");
        } else {
          out.print("stack [");
          int i = 0;
          while (i < newFrameState.stackSize()) {
            if (i > 0) {
                out.print(", ");
            }
            Value value = newFrameState.stackAt(i);
            out.print(i + ":" + Util.valueString(value));
              i += value.kind.sizeInSlots();
            if (value instanceof Phi) {
                Phi phi = (Phi) value;
                if (phi.operand() != null) {
                    out.print(" ");
                    out.print(phi.operand().toString());
                }
            }
          }
          out.print(']');
        }
        if (newFrameState.locksSize() != 0) {
            // print out the lines on the line below this
            // one at the same indentation level.
            out.println();
            out.fillTo(startPosition, ' ');
            out.print("locks [");
            for (int i = 0; i < newFrameState.locksSize(); i++) {
                Value value = newFrameState.lockAt(i);
                if (i > 0) {
                    out.print(", ");
                }
                out.print(i + ":");
                if (value == null) {
                    // synchronized methods push null on the lock stack
                    out.print("this");
                } else {
                    out.print(Util.valueString(value));
                }
            }
            out.print("]");
        }
    }
}
