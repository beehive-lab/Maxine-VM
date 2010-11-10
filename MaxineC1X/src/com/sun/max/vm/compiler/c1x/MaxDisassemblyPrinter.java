/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiDebugInfo.Frame;
import com.sun.cri.ci.CiRegister.RegisterFlag;
import com.sun.cri.ci.CiTargetMethod.Call;
import com.sun.cri.ci.CiTargetMethod.DataPatch;
import com.sun.cri.ci.CiTargetMethod.Safepoint;
import com.sun.cri.ci.CiTargetMethod.Site;
import com.sun.max.annotate.*;
import com.sun.max.asm.dis.*;

/**
 * Enhanced disassembly for input to the C1Visualizer.
 *
 * @author Doug Simon
 */
@HOSTED_ONLY
public class MaxDisassemblyPrinter extends DisassemblyPrinter {

    private CiTargetMethod targetMethod;
    private final HashMap<Integer, Site> sitesMap = new HashMap<Integer, Site>();
    private Site currentSite;

    private void addSites(List<? extends Site> sites) {
        for (Site site : sites) {
            sitesMap.put(site.pcOffset, site);
        }
    }

    public MaxDisassemblyPrinter(final CiTargetMethod targetMethod) {
        super(false);
        this.targetMethod = targetMethod;

        addSites(targetMethod.directCalls);
        addSites(targetMethod.indirectCalls);
        addSites(targetMethod.safepoints);
        addSites(targetMethod.dataReferences);
    }

    private String toString(Call call) {
        if (call.runtimeCall != null) {
            return "{" + call.runtimeCall.name() + "}";
        } else if (call.symbol != null) {
            return "{" + call.symbol + "}";
        } else if (call.globalStubID != null) {
            return "{" + call.globalStubID + "}";
        } else {
            return "{" + call.method + "}";
        }
    }
    private String siteInfo(int pcOffset) {
        for (Call call : targetMethod.directCalls) {
            if (call.pcOffset == pcOffset) {
                return toString(call);
            }
        }
        for (Call call : targetMethod.indirectCalls) {
            if (call.pcOffset == pcOffset) {
                return toString(call);
            }
        }
        for (Site site : targetMethod.safepoints) {
            if (site.pcOffset == pcOffset) {
                return "{safepoint}";
            }
        }
        for (DataPatch site : targetMethod.dataReferences) {
            if (site.pcOffset == pcOffset) {
                return "{" + site.constant + "}";
            }
        }
        return null;
    }
    @Override
    protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
        String string = super.disassembledObjectString(disassembler, disassembledObject);
        Site site = currentSite;
        if (site != null) {
            String extra;
            if (site instanceof Call) {
                extra = toString((Call) site);
            } else if (site instanceof Safepoint) {
                extra = "{safepoint}";
            } else if (site instanceof DataPatch) {
                extra = "{" + ((DataPatch) site).constant + "}";
            } else {
                extra = "{??? " + site.getClass().getSimpleName() + "???}";
            }
            string += " " + extra;
        }
        return string;
    }

    public static String tabulateValues(Frame frame) {
        int cols = Math.max(frame.numLocals, Math.max(frame.numStack, frame.numLocks));
        assert cols > 0;
        ArrayList<Object> cells = new ArrayList<Object>();
        cells.add("");
        for (int i = 0; i < cols; i++) {
            cells.add(String.valueOf(i));
        }
        cols++;
        if (frame.numLocals != 0) {
            cells.add("locals:");
            cells.addAll(Arrays.asList(frame.values).subList(0, frame.numLocals));
            cells.addAll(Collections.nCopies(cols - frame.numLocals - 1, ""));
        }
        if (frame.numStack != 0) {
            cells.add("stack:");
            cells.addAll(Arrays.asList(frame.values).subList(frame.numLocals, frame.numLocals + frame.numStack));
            cells.addAll(Collections.nCopies(cols - frame.numStack - 1, ""));
        }
        if (frame.numLocks != 0) {
            cells.add("locks:");
            cells.addAll(Arrays.asList(frame.values).subList(frame.numLocals + frame.numStack, frame.values.length));
            cells.addAll(Collections.nCopies(cols - frame.numLocks - 1, ""));
        }
        return CiUtil.tabulate(cells.toArray(), cols, 1, 1);
    }

    @Override
    protected void printDisassembledObject(Disassembler disassembler, PrintStream stream, int nOffsetChars, int nLabelChars, DisassembledObject disassembledObject) {
        int pc = disassembledObject.startPosition();
        currentSite = sitesMap.get(pc);
        if (currentSite != null) {
            CiDebugInfo info = currentSite.debugInfo();
            CiTarget target = target();
            CiArchitecture arch = target.arch;
            if (info != null) {
                if (info.hasRegisterRefMap()) {
                    stream.print(";;   reg-ref-map:");
                    BitSet bs = CiUtil.asBitSet(info.registerRefMap);
                    for (int enc = bs.nextSetBit(0); enc >= 0; enc = bs.nextSetBit(enc + 1)) {
                        stream.print(" " + arch.registerFor(enc, RegisterFlag.CPU));
                    }
                    stream.println(" " + bs);
                }
                if (info.hasStackRefMap()) {
                    stream.print(";; frame-ref-map:");
                    BitSet bs = CiUtil.asBitSet(info.frameRefMap);
                    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                        stream.print(" +" + i * target.spillSlotSize);
                    }
                    stream.println(" " + bs);
                }

                CiCodePos pos = info.codePos;
                while (pos != null) {
                    stream.println(";; at " + CiUtil.appendLocation(new StringBuilder(), pos.method, pos.bci));
                    if (pos instanceof Frame) {
                        Frame frame = (Frame) pos;
                        if (frame.values != null && frame.values.length > 0) {
                            String table = tabulateValues(frame);
                            String nl = String.format("%n");
                            for (String row : table.split(nl)) {
                                if (!row.trim().isEmpty()) {
                                    stream.println(";;   " + row);
                                }
                            }
                        }
                    }
                    pos = pos.caller;
                }
            }
        }
        super.printDisassembledObject(disassembler, stream, nOffsetChars, nLabelChars, disassembledObject);
    }
}
