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
package com.sun.max.vm.prototype;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;

import com.sun.max.program.option.*;
import com.sun.max.vm.prototype.BootImage.*;


/**
 * A utility for printing the contents of a {@link BootImage}.
 *
 * @author Doug Simon
 */
public class BootImagePrinter {

    private final OptionSet options = new OptionSet();

    private final Option<Boolean> help = options.newBooleanOption("help", false,
            "Show help message and exit.");

    private final Option<File> outputFileOption = options.newFileOption("o", (File) null,
            "The file to which output is written instead of standard out.");

    private final Option<String> sectionsOption = options.newStringOption("s", "HST",
            "The sections to be printed: (H)eader, (S)tringInfo, (T)railer, (h)eap, (c)ode, (r)elocation data, (p)adding");

    private int heapStart;
    private int codeStart;
    private int wordSize;

    public static void main(String[] args) throws IOException, BootImageException {
        new BootImagePrinter().run(args);
    }

    public int run(String[] args) throws BootImageException, IOException {
        options.parseArguments(args);

        if (help.getValue()) {
            options.printHelp(System.out, 80);
            return 0;
        }

        String[] arguments = options.getArguments();
        if (arguments.length != 1) {
            System.out.println("Expected exactly 1 non-option command line argument, got " + arguments.length);
            options.printHelp(System.out, 80);
            return 1;
        }

        String bootImageFilePath = arguments[0];
        File bootImageFile = new File(bootImageFilePath);
        BootImage bootImage = new BootImage(bootImageFile);

        heapStart = 0;
        codeStart = heapStart + bootImage.header.heapSize;
        wordSize = bootImage.header.wordSize;

        PrintStream out = System.out;
        if (outputFileOption.getValue() != null) {
            out = new PrintStream(new FileOutputStream(outputFileOption.getValue()));
        }

        // Checkstyle: stop
        for (char section : sectionsOption.getValue().toCharArray()) {
            switch (section) {
                case 'H': printHeader(out, bootImage); break;
                case 'S': printStringInfo(out, bootImage); break;
                case 'T': printTrailer(out, bootImage); break;
                case 'h': printHeap(out, bootImage); break;
                case 'c': printCode(out, bootImage); break;
                case 'r': printRelocationData(out, bootImage); break;
                case 'p': printPadding(out, bootImage); break;
            }
        }
        // Checkstyle: resume

        if (outputFileOption.getValue() != null) {
            out.close();
        }

        return 0;
    }

    private void printPadding(PrintStream out, BootImage bootImage) {
        byte[] padding = bootImage.padding;
        printData(out, "PADDING", padding.length, bootImage.paddingOffset(), ByteBuffer.wrap(padding));
    }

    private void printRelocationData(PrintStream out, BootImage bootImage) {
        byte[] relocationData = bootImage.relocationData;
        printData(out, "RELOCATION DATA", relocationData.length, bootImage.relocationDataOffset(), ByteBuffer.wrap(relocationData));
    }

    private void printHeader(PrintStream out, BootImage bootImage) {
        printSection(out, bootImage.header, "HEADER");
    }

    private void printTrailer(PrintStream out, BootImage bootImage) {
        printSection(out, bootImage.trailer, "TRAILER");
    }

    private void printStringInfo(PrintStream out, BootImage bootImage) {
        printSection(out, bootImage.stringInfo, "STRING INFO");
    }

    private void printHeap(PrintStream out, BootImage bootImage) {
        int size = bootImage.header.heapSize;
        assert heapStart + size == codeStart;
        printData(out, "HEAP", size, heapStart, bootImage.heap());
    }

    private void printCode(PrintStream out, BootImage bootImage) {
        int size = bootImage.header.codeSize;
        printData(out, "CODE", size, codeStart, bootImage.code());
    }

    private void printData(PrintStream out, String name, int size, int start, ByteBuffer buffer) {
        ByteBuffer copy = buffer.duplicate();
        out.println(sectionHeader(name, size, start));
        int end = start + size;
        for (int a = start; a < end; a += wordSize) {
            if (wordSize == 8) {
                out.printf("0x%08x: 0x%016x%n", a, copy.getLong());
            } else {
                out.printf("0x%08x: 0x%08x%n", a, copy.getInt());
            }
        }
    }

    private String sectionHeader(String name, int size, int start) {
        int end = start + size;
        int wordSizeWidth = wordSize * 2;
        return String.format("--- %s: start=0x%0" + wordSizeWidth + "x, end=0x%0" + wordSizeWidth + "x, size=%d[0x%08x] ---", name, start, end, size, size);
    }

    private void printSection(PrintStream out, FieldSection section, String name) {
        int nameWidth = 0;
        int valueWidth = 0;
        for (Field field : section.fields()) {
            nameWidth = Math.max(nameWidth, field.getName().length());
            try {
                valueWidth = Math.max(valueWidth, String.valueOf(field.get(section)).length());
            } catch (Exception e) {
            }
        }
        out.println(sectionHeader(name, section.size(), section.offset()));
        for (Field field : section.fields()) {
            Object value;
            try {
                value = field.get(section);
            } catch (Exception e) {
                value = "error: " + e;
            }
            if (section.fieldType() == int.class) {
                out.printf("%" + nameWidth + "s:   %-" + valueWidth + "s   0x%08x%n", field.getName(), value, value);
            } else {
                assert section.fieldType() == String.class;
                out.printf("%" + nameWidth + "s:   %-" + valueWidth + "s%n", field.getName(), value);
            }
        }
    }
}
