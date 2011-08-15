/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.hcfdis;

import static com.sun.cri.ci.CiHexCodeFile.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.*;
import com.sun.max.asm.*;
import com.sun.max.asm.InlineDataDescriptor.JumpTable32;
import com.sun.max.asm.InlineDataDescriptor.LookupTable32;
import com.sun.max.asm.dis.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.option.*;

/**
 * Utility for converting a {@link CiHexCodeFile} to a commented disassembly.
 */
public class HexCodeFileDis extends DisassemblyPrinter {

    public static final OptionSet options = new OptionSet();

    public static final Option<String> commentPrefixOption = options.newStringOption("comment-prefix", ";; ", "The prefix prepended to each line of instruction comments.");
    public static final Option<String> hcfOpenOption = options.newStringOption("hcf-open", EMBEDDED_HCF_OPEN, "Start delimiter for HexCodeFile.");
    public static final Option<String> hcfCloseOption = options.newStringOption("hcf-close", EMBEDDED_HCF_CLOSE, "End delimiter for HexCodeFile.");
    public static final Option<Boolean> copyDelimitersOption = options.newBooleanOption("copy-delimiters", false, "Copy delimiters to output.");
    public static final Option<String> dirOption = options.newStringOption("d", null, "Output directory (input files are overwritten if not specified).");
    public static final Option<Boolean> verboseOption = options.newBooleanOption("v", true, "Verbose operation.");

    private static final Field offsetField;
    static {
        try {
            offsetField = String.class.getDeclaredField("offset");
        } catch (Exception e) {
            throw new Error("Could not get reflective access to field " + String.class.getName() + ".offset");
        }
        offsetField.setAccessible(true);
    }

    /**
     * Prefix prepended to each instruction comment line in the disassembly.
     */
    public String commentLinePrefix = ";; ";

    /**
     * The input being processed.
     */
    protected String input;

    /**
     * A name for the input source to be used in error messages.
     */
    protected String inputSource;

    /**
     * The HexCodeFile currently being processed.
     */
    protected CiHexCodeFile hcf;

    /**
     * Current machine code position during disassembly.
     */
    protected int pos;

    /**
     * Count of embedded HexCodeFiles disassembled.
     */
    protected int hcfCount;

    public HexCodeFileDis(boolean includeHeader) {
        super(includeHeader);
    }

    /**
     * Decoding method called from c1visualizer. The visualizer loads this class and calls this method using reflection.
     */
    public static String processEmbeddedString(String source) {
        if (!source.startsWith(EMBEDDED_HCF_OPEN) || !source.endsWith(EMBEDDED_HCF_CLOSE)) {
            throw new IllegalArgumentException("Input string is not in embedded format");
        }
        source = source.substring(EMBEDDED_HCF_OPEN.length(), source.length() - EMBEDDED_HCF_OPEN.length() - EMBEDDED_HCF_CLOSE.length());

        HexCodeFileDis dis = new HexCodeFileDis(false);
        CiHexCodeFile hcf = CiHexCodeFile.parse(source, "");
        return dis.process(hcf, null);
    }

    /**
     * Disassembles all HexCodeFiles embedded in a given input string.
     *
     * @param input some input containing 0 or more HexCodeFiles
     * @param inputName name for the input source to be used in error messages
     * @param startDelim the delimiter just before to an embedded HexCodeFile in {@code input}
     * @param endDelim the delimiter just after to an embedded HexCodeFile in {@code input}
     * @return the value of {@code input} with all embedded HexCodeFiles converted to their disassembled form
     */
    public String processAll(String input, String inputName, String startDelim, String endDelim, boolean verbose) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length() * 2);
        PrintStream out = new PrintStream(baos);

        int codeEnd = 0;
        int index;
        while ((index = input.indexOf(startDelim, codeEnd)) != -1) {
            int codeStart = index + startDelim.length();

            String copy = input.substring(codeEnd, codeStart);
            if (!copyDelimitersOption.getValue()) {
                if (copy.startsWith(endDelim)) {
                    copy = copy.substring(endDelim.length());
                }
                if (copy.endsWith(startDelim)) {
                    copy = copy.substring(0, copy.length() - startDelim.length());
                }
            }
            out.println(copy);

            int endIndex = input.indexOf(endDelim, codeStart);
            assert endIndex != -1;

            String source = input.substring(codeStart, endIndex);

            CiHexCodeFile hcf = CiHexCodeFile.parse(source, inputName);
            process(hcf, out);

            if (verbose) {
                System.out.print(".");
            }

            codeEnd = endIndex;
        }
        if (verbose && hcfCount != 0) {
            System.out.println();
        }

        String copy = input.substring(codeEnd);
        if (!copyDelimitersOption.getValue()) {
            if (copy.startsWith(endDelim)) {
                copy = copy.substring(endDelim.length());
            }
        }
        out.print(copy);
        out.flush();
        return baos.toString();
    }

    /**
     * Disassembles a given HexCodeFile.
     *
     * @param out if not {@code null}, this is where the HexCodeFile disassembly should be printed
     * @return the disassembled HexCodeFile if {@code out == null} otherwise {@code null}
     */
    public String process(CiHexCodeFile hcf, PrintStream out) {
        final InlineDataDecoder inlineDataDecoder = makeInlineDataDecoder(hcf);
        ByteArrayOutputStream buf = null;
        if (out == null) {
            buf = new ByteArrayOutputStream();
            out = new PrintStream(buf);
        }
        this.hcf = hcf;
        this.hcfCount++;
        Disassembler.disassemble(out, hcf.code, parseISA(hcf.isa), WordWidth.fromInt(hcf.wordWidth), hcf.startAddress, inlineDataDecoder, this);
        this.hcf = null;
        out.flush();
        if (buf != null) {
            return buf.toString();
        }
        return null;
    }

    public static InlineDataDecoder makeInlineDataDecoder(CodeAnnotation[] annotations) {
        ArrayList<InlineDataDescriptor> descriptors = new ArrayList<InlineDataDescriptor>();
        for (CodeAnnotation a : annotations) {
            if (a instanceof JumpTable) {
                JumpTable table = (JumpTable) a;
                if (table.entrySize == 4) {
                    descriptors.add(new JumpTable32(table.position, table.low, table.high));
                } else {
                    System.err.println("WARNING: Ignoring jump table with an entry size != 4");
                }
            } else if (a instanceof LookupTable) {
                LookupTable table = (LookupTable) a;
                if (table.keySize == 4 && table.offsetSize == 4) {
                    descriptors.add(new LookupTable32(table.position, table.npairs));
                } else {
                    System.err.println("WARNING: Ignoring lookup table with a key or offset size != 4");
                }
            }
        }
        final InlineDataDecoder inlineDataDecoder = descriptors.isEmpty() ? null : new InlineDataDecoder(descriptors);
        return inlineDataDecoder;
    }

    public static InlineDataDecoder makeInlineDataDecoder(CiHexCodeFile hcf) {
        ArrayList<InlineDataDescriptor> descriptors = new ArrayList<InlineDataDescriptor>();
        for (JumpTable table : hcf.jumpTables) {
            if (table.entrySize == 4) {
                descriptors.add(new JumpTable32(table.position, table.low, table.high));
            } else {
                System.err.println("WARNING: Ignoring jump table with an entry size != 4");
            }
        }
        for (LookupTable table : hcf.lookupTables) {
            if (table.keySize == 4 && table.offsetSize == 4) {
                descriptors.add(new LookupTable32(table.position, table.npairs));
            } else {
                System.err.println("WARNING: Ignoring lookup table with a key or offset size != 4");
            }
        }
        final InlineDataDecoder inlineDataDecoder = descriptors.isEmpty() ? null : new InlineDataDecoder(descriptors);
        return inlineDataDecoder;
    }

    @Override
    protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
        String string = super.disassembledObjectString(disassembler, disassembledObject);
        String s = hcf.operandComments.get(pos);
        if (s != null) {
            string += " " + s;
        }
        return string;
    }

    @Override
    protected void printDisassembledObject(Disassembler disassembler, PrintStream stream, int nOffsetChars, int nLabelChars, DisassembledObject disassembledObject) {
        pos = disassembledObject.startPosition();
        List<String> comments = hcf.comments.get(pos);
        if (comments != null) {
            for (String comment : comments) {
                stream.println(commentLinePrefix + comment.replace(CiHexCodeFile.NEW_LINE, CiHexCodeFile.NEW_LINE + commentLinePrefix));
            }
        }
        super.printDisassembledObject(disassembler, stream, nOffsetChars, nLabelChars, disassembledObject);
    }


    ISA parseISA(String value) {
        try {
            return ISA.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new Error("Unsupported ISA - must be one of " + Arrays.toString(ISA.values()));
        }
    }

    public static void main(String[] args) throws IOException {
        options.parseArguments(args);

        File outDir = null;
        if (dirOption.getValue() != null) {
            outDir = new File(dirOption.getValue());
            if (!outDir.isDirectory()) {
                if (!outDir.mkdirs()) {
                    throw new Error("Could not create output directory " + outDir.getAbsolutePath());
                }
            }
        }

        for (String arg : options.getArguments()) {
            HexCodeFileDis dis = new HexCodeFileDis(false);
            File inputFile = new File(arg);
            String input = new String(Files.toChars(inputFile));
            String inputSource = inputFile.getAbsolutePath();
            String output = dis.processAll(input, inputSource, hcfOpenOption.getValue(), hcfCloseOption.getValue(), verboseOption.getValue());

            File outputFile;
            if (outDir == null) {
                outputFile = inputFile;
            } else {
                outputFile = new File(outDir, inputFile.getName());
            }

            System.out.println(outputFile + ": disassembled " + dis.hcfCount + " embedded HexCodeFiles");
            if (outputFile.equals(inputFile)) {
                if (!output.equals(input)) {
                    Files.fill(inputFile, output);
                }
            } else {
                Files.fill(outputFile, output);
            }
        }
    }
}
