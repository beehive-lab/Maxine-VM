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
package com.sun.max.io;

import java.io.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.util.*;

/**
 * @author Doug Simon
 */
public final class Files {

    private Files() {
    }

    public static void copy(File from, File to) throws FileNotFoundException, IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(from);
            outputStream = new FileOutputStream(to);
            Streams.copy(inputStream, outputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    public static boolean equals(File file1, File file2) throws FileNotFoundException, IOException {
        final long length1 = file1.length();
        final long length2 = file2.length();
        if (length1 != length2) {
            return false;
        }
        if (length1 <= 0) {
            return true;
        }
        InputStream inputStream1 = null;
        InputStream inputStream2 = null;
        try {
            inputStream1 = new BufferedInputStream(new FileInputStream(file1), (int) length1);
            inputStream2 = new BufferedInputStream(new FileInputStream(file2), (int) length2);
            return Streams.equals(inputStream1, inputStream2);
        } finally {
            if (inputStream1 != null) {
                inputStream1.close();
            }
            if (inputStream2 != null) {
                inputStream2.close();
            }
        }
    }

    public static boolean equals(File file, Iterator<String> lines) throws FileNotFoundException, IOException {
        final BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!lines.hasNext()) {
                    return false;
                }
                if (!line.equals(lines.next())) {
                    return false;
                }
            }
        } finally {
            bufferedReader.close();
        }
        return !lines.hasNext();
    }

    public static byte[] toBytes(File file) throws IOException {
        if (file.length() > Integer.MAX_VALUE) {
            throw new IOException("file is too big to read into an array: " + file);
        }
        final InputStream stream = new BufferedInputStream(new FileInputStream(file), (int) file.length());
        try {
            return Streams.readFully(stream, new byte[(int) file.length()]);
        } finally {
            stream.close();
        }
    }

    /**
     * Creates/overwrites a file with a given string.
     */
    public static void fill(File file, String content) throws IOException {
        final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        try {
            bufferedWriter.write(content);
        } finally {
            bufferedWriter.close();
        }
    }

    /**
     * Creates/overwrites a file from a reader.
     */
    public static void fill(File file, Reader reader, boolean append) throws IOException {
        final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        try {
            int ch;
            while ((ch = reader.read()) != -1) {
                bufferedWriter.write(ch);
            }
        } finally {
            bufferedWriter.close();
        }
    }

    public static char[] toChars(File file) throws IOException {
        int length = (int) file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("file is too big to read into an array: " + file);
        }
        final Reader fileReader = new BufferedReader(new FileReader(file), length);
        char[] chars = new char[length];
        try {
            chars = Streams.readFully(fileReader, chars);
        } catch (TruncatedInputException truncatedInputException) {
            // Must have been multi-byte characters in the file
            length = truncatedInputException.inputLength();
            final char[] oldChars = chars;
            chars = new char[length];
            System.arraycopy(oldChars, 0, chars, 0, length);
        } finally {
            fileReader.close();
        }
        return chars;
    }

    /**
     * Updates the generated content part of a file. A generated content part is delimited by a line containing
     * only {@code start} and a line containing only {@code end}. If the given file already exists and
     * has these delimiters, the content between these lines is compared with {@code content} and replaced
     * if it is different. If the file does not exist, a new file is created with {@code content} surrounded
     * by the specified delimiters. If the file exists and does not currently have the specified delimiters, an
     * IOException is thrown.
     * 
     * @return true if the file was modified or created
     */
    public static boolean updateGeneratedContent(File file, ReadableSource content, String start, String end) throws IOException {

        if (!file.exists()) {
            final PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            try {
                final Reader reader = content.reader(true);
                try {
                    printWriter.println(start);
                    Streams.copy(reader, printWriter);
                    printWriter.println(end);
                } finally {
                    reader.close();
                }
            } finally {
                printWriter.close();
            }
            return true;
        }

        final File tempFile = File.createTempFile(file.getName() + ".", null);
        PrintWriter printWriter = null;
        BufferedReader contentReader = null;
        BufferedReader existingFileReader = null;
        try {
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(tempFile)));
            contentReader = (BufferedReader) content.reader(true);
            existingFileReader = new BufferedReader(new FileReader(file));

            // Copy existing file up to generated content opening delimiter
            String line;
            while ((line = existingFileReader.readLine()) != null) {
                printWriter.println(line);
                if (line.equals(start)) {
                    break;
                }
            }

            if (line == null) {
                throw new IOException("generated content starting delimiter \"" + start + "\" not found in existing file: " + file);
            }

            boolean changed = false;
            boolean seenEnd = false;

            // Copy new content, noting if it differs from existing generated content
            while ((line = contentReader.readLine()) != null) {
                if (!seenEnd) {
                    final String existingLine = existingFileReader.readLine();
                    if (existingLine != null) {
                        if (end.equals(existingLine)) {
                            seenEnd = true;
                            changed = true;
                        } else {
                            changed = changed || !line.equals(existingLine);
                        }
                    }
                }
                printWriter.println(line);
            }

            // Find the generated content closing delimiter
            if (!seenEnd) {
                while ((line = existingFileReader.readLine()) != null) {
                    if (line.equals(end)) {
                        seenEnd = true;
                        break;
                    }
                    changed = true;
                }
                if (!seenEnd) {
                    throw new IOException("generated content ending delimiter \"" + end + "\" not found in existing file: " + file);
                }
            }
            printWriter.println(end);

            // Copy existing file after generated content closing delimiter
            while ((line = existingFileReader.readLine()) != null) {
                printWriter.println(line);
            }

            printWriter.close();
            printWriter = null;
            existingFileReader.close();
            existingFileReader = null;

            if (changed) {
                copy(tempFile, file);
                return true;
            }
            return false;
        } finally {
            quietClose(printWriter);
            quietClose(contentReader);
            quietClose(existingFileReader);
            if (!tempFile.delete()) {
                throw new IOException("could not delete file for update: " + file);
            }
        }
    }

    private static void quietClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Sequence<File> find(File directory, final String suffix, AppendableSequence<File> listing) {
        final Predicate<File> suffixPredicate = new Predicate<File>() {
            public boolean evaluate(File file) {
                return file.getName().endsWith(suffix);
            }

        };
        return find(directory, suffixPredicate, listing);
    }

    public static Sequence<File> find(File directory, Predicate<File> filter, AppendableSequence<File> listing) {
        assert directory.isDirectory();
        return find(directory, listing == null ? new LinkSequence<File>() : listing, filter);
    }

    private static AppendableSequence<File> find(File directory, AppendableSequence<File> listing, Predicate<File> filter) {
        assert directory.isDirectory();
        final File[] entries = directory.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (!entry.isDirectory()) {
                    if (filter == null || filter.evaluate(entry)) {
                        listing.append(entry);
                    }
                } else {
                    find(entry, listing, filter);
                }
            }
        }
        return listing;
    }
}

