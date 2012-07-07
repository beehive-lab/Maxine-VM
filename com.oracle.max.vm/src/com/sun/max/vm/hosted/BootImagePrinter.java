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
package com.sun.max.vm.hosted;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;

import com.sun.max.program.option.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.hosted.BootImage.FieldSection;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;

/**
 * A utility for printing the contents of a {@link BootImage}.
 */
public class BootImagePrinter {

    public static class Range {
        final int start;
        final int size;

        public Range(int start, int size) {
            this.start = start;
            this.size = size;
        }

        public boolean contains(Range other) {
            return other.start >= start && other.end() <= end();
        }

        public int end() {
            return start + size;
        }

        @Override
        public String toString() {
            return "[" + start + '-' + (end() - 1) + ']';
        }
    }

    private final OptionSet options = new OptionSet();

    private final Option<Boolean> help = options.newBooleanOption("help", false,
            "Show help message and exit.");

    private final Option<File> outputFileOption = options.newFileOption("o", (File) null,
            "The file to which output is written instead of standard out.");

    private final Option<String> sectionsOption = options.newStringOption("s", "HST",
            "The sections to be printed: (H)eader, (S)tringInfo, (T)railer, (h)eap, (c)ode, (r)elocation data, (p)adding. " +
            "A sub-range of the data sections ('h', 'c', 'r', 'p') can be specified with a range suffix of the form '{<start>:<size>}' where " +
            "<start> and <size> can be hexadecimal (with a '0x' prefix) or plain decimal. For example, " +
            "'-s=h{0x100:300}c{+100:20}' will dump 300 bytes of the heap starting at heap address 0x100 and 20 bytes " +
            "of the code starting at code address 100 (the '+' prefix specifies that <start> is relative to the first " +
            "address in the code section). Note that the heap and code are contiguous and share the same address space. " +
            "All other data sections have their own address space.");

    private final Option<String> filterOption = options.newStringOption("filter", null,
            "Only heap and code values matching containing the specified hex value substring are printed.");

    private Range heapRegion;
    private Range codeRegion;
    private int wordSize;
    private BootImage bootImage;
    private String filter;

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
        bootImage = new BootImage(bootImageFile);

        heapRegion = new Range(0, bootImage.header.heapSize);
        codeRegion = new Range(heapRegion.end(), bootImage.header.codeSize);
        wordSize = bootImage.header.wordSize;

        PrintStream out = System.out;
        if (outputFileOption.getValue() != null) {
            out = new PrintStream(new FileOutputStream(outputFileOption.getValue()));
        }

        filter = filterOption.getValue();

        int cursor = 0;
        String sections = sectionsOption.getValue();
        while (cursor < sections.length()) {
            char section = sections.charAt(cursor++);
            switch (section) {
                case 'H': {
                    printHeader(out);
                    break;
                }
                case 'S': {
                    printStringInfo(out);
                    break;
                }
                case 'T': {
                    printTrailer(out);
                    break;
                }
                case 'h': {
                    Range subregion = parseNarrowingRange(sections, cursor, heapRegion);
                    if (subregion != null) {
                        cursor = sections.indexOf('}', cursor) + 1;
                    }
                    printHeap(out, subregion);
                    break;
                }
                case 'c': {
                    Range subregion = parseNarrowingRange(sections, cursor, codeRegion);
                    if (subregion != null) {
                        cursor = sections.indexOf('}', cursor) + 1;
                    }
                    printCode(out, subregion);
                    break;
                }
                case 'r': {
                    Range subregion = parseNarrowingRange(sections, cursor, new Range(0, bootImage.header.relocationDataSize));
                    if (subregion != null) {
                        cursor = sections.indexOf('}', cursor) + 1;
                    }
                    printRelocationData(out, subregion);
                    break;
                }
                case 'p': {
                    Range subregion = parseNarrowingRange(sections, cursor, new Range(0, bootImage.paddingSize()));
                    if (subregion != null) {
                        cursor = sections.indexOf('}', cursor) + 1;
                    }
                    printPadding(out, subregion);
                    break;
                }
            }
        }

        if (outputFileOption.getValue() != null) {
            out.close();
        }

        return 0;
    }

    private static int parseInt(String s) {
        if (s.startsWith("0x")) {
            return Integer.parseInt(s.substring(2), 16);
        }
        return Integer.parseInt(s);
    }

    private static Range parseNarrowingRange(String s, int cursor, Range range) {
        if (cursor < s.length() && s.charAt(cursor) == '{') {
            int offset = 0;
            if (cursor + 1 < s.length() && s.charAt(cursor + 1) == '+') {
                offset = range.start;
                cursor++;
            }

            int colon = s.indexOf(':', cursor + 1);
            if (colon == -1) {
                throw new IllegalArgumentException("Range at index " + cursor + " in sections string is missing ':'");
            }
            int closingBrace = s.indexOf('}', colon + 1);
            if (closingBrace == -1) {
                throw new IllegalArgumentException("Range at index " + cursor + " in sections string is missing '}'");
            }

            Range result = new Range(offset + parseInt(s.substring(cursor + 1, colon)), parseInt(s.substring(colon + 1, closingBrace)));
            if (!range.contains(result)) {
                throw new IllegalArgumentException("Narrowing range " + result + " is not completely contained by " + range);
            }
            return result;
        }
        return null;
    }

    private void printPadding(PrintStream out, Range subregion) {
        byte[] padding = bootImage.padding;
        printData(out, "PADDING", bootImage.paddingOffset(), padding, subregion);
    }

    private void printRelocationData(PrintStream out, Range subregion) {
        byte[] relocationData = bootImage.relocationData;
        printData(out, "RELOCATION DATA", bootImage.relocationDataOffset(), relocationData, subregion);
    }

    private void printHeader(PrintStream out) {
        printSection(out, bootImage.header, "HEADER");
    }

    private void printTrailer(PrintStream out) {
        printSection(out, bootImage.trailer, "TRAILER");
    }

    private void printHeap(PrintStream out, Range subregion) {
        printHeapOrCodeData(out, "HEAP", heapRegion, bootImage.heapAndCode(), subregion, bootImage.heapOffset());
    }

    private void printCode(PrintStream out, Range subregion) {
        printHeapOrCodeData(out, "CODE", codeRegion, bootImage.heapAndCode(), subregion, bootImage.codeOffset());
    }

    private void printStringInfo(PrintStream out) {
        printSection(out, bootImage.stringInfo, "STRING INFO");
    }

    private int wordAlign(int value) {
        return (value + (wordSize - 1)) & ~(wordSize - 1);
    }

    private void printData(PrintStream out, String name, int offsetInImage, byte[] buffer, Range subregion) {
        int startAddress = 0;
        int size = buffer.length;
        out.println(sectionHeader(name, size, startAddress, offsetInImage));

        int end = startAddress + size;
        int address;

        if (subregion != null) {
            address = subregion.start;
            end = address + subregion.size;
        } else {
            address = startAddress;
            end = startAddress + size;
        }
        assert end <= buffer.length;
        while (address < end) {
            out.printf("0x%08x:", address);
            for (int i = 0; i < 32 && address < end; ++i) {
                out.printf(" %02x", buffer[address]);
                address++;
            }
            out.println();
        }
    }

    private long readWord(int pointer, ByteBuffer memory) {
        if (wordSize == 8) {
            return memory.getLong(pointer);
        }
        return memory.getInt(pointer);
    }

    private static boolean contains(int pointer, int size, ByteBuffer memory) {
        return pointer >= 0 && pointer + size <= memory.limit();
    }

    private boolean isValidOrigin(int origin, ByteBuffer memory) {
        GeneralLayout layout = bootImage.vmConfiguration.layoutScheme().generalLayout;
        int hubOffset = layout.getOffsetFromOrigin(HeaderField.HUB).toInt();
        if (contains(origin + hubOffset, wordSize, memory)) {
            int hub = (int) readWord(origin + hubOffset, memory);
            if (contains(hub + hubOffset, wordSize, memory)) {
                int hubHub = (int) readWord(hub + hubOffset, memory);
                if (contains(hubHub + hubOffset, wordSize, memory)) {
                    int hubHubHub = (int) readWord(hubHub + hubOffset, memory);
                    return hubHub == hubHubHub;
                }
            }
        }
        return false;
    }

    private void printHeapOrCodeData(PrintStream out, String name, Range region, ByteBuffer heapAndCode, Range subregion, int offsetInImage) {
        int startAddress = region.start;
        int size = region.size;
        out.println(sectionHeader(name, size, startAddress, offsetInImage));

        int end;
        int address;

        ByteArrayBitMap relocMap = new ByteArrayBitMap(bootImage.relocationData);
        if (subregion != null) {
            address = wordAlign(subregion.start);
            end = address + subregion.size;
        } else {
            address = startAddress;
            end = startAddress + size;
        }
        while (address < end) {
            int addressIndex = address / wordSize;
            char pointerMark = relocMap.isSet(addressIndex) ? '*' : ' ';
            String originLabel = isValidOrigin(address, heapAndCode) ? " <-- origin" : "";
            if (wordSize == 8) {
                String value = String.format("%016x", heapAndCode.getLong(address));
                if (filter == null || value.contains(filter)) {
                    out.printf("%c0x%08x: 0x%s%s%n", pointerMark, address, value, originLabel);
                }
            } else {
                String value = String.format("%08x", heapAndCode.getInt(address));
                if (filter == null || value.contains(filter)) {
                    out.printf("%c0x%08x: 0x%s%s%n", pointerMark, address, value, originLabel);
                }
            }
            address += wordSize;
        }
    }

    private String sectionHeader(String name, int size, int startAddress, int offsetInImage) {
        int end = startAddress + size;
        int wordSizeWidth = wordSize * 2;
        return String.format("--- %s: start=0x%0" + wordSizeWidth + "x, end=0x%0" + wordSizeWidth +
            "x, size=%d[0x%08x], image-offset=%d[0x%08x] ---", name, startAddress, end, size, size, offsetInImage, offsetInImage);
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
        out.println(sectionHeader(name, section.size(), 0, section.offset()));
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
