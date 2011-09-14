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
package com.sun.max.vm;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.program.ProgramWarning.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * This class presents a low-level VM logging facility that closely resembles (but extends) that offered by standard
 * {@link PrintStream}s. All output of the methods in this class goes to a configurable {@linkplain #os output stream}.
 */
@NEVER_INLINE
public final class Log {

    private Log() {
    }

    @C_FUNCTION
    static native void log_print_buffer(Address val);

    @C_FUNCTION
    static native void log_print_boolean(boolean val);

    @C_FUNCTION
    static native void log_print_char(int val);

    @C_FUNCTION
    static native void log_print_int(int val);

    @C_FUNCTION
    static native void log_print_long(long val);

    @C_FUNCTION
    static native void log_print_float(float val);

    @C_FUNCTION
    static native void log_print_double(double val);

    @C_FUNCTION
    static native void log_print_word(Word val);

    @C_FUNCTION
    static native void log_print_newline();

    @C_FUNCTION
    static native void log_print_symbol(Address address);

    @C_FUNCTION
    static native void log_lock();

    @C_FUNCTION
    static native void log_unlock();

    @C_FUNCTION
    static native void log_flush();

    /**
     * The singleton VM log output stream.
     *
     * This output stream writes to the native standard output stream by default. It can be redirected
     * to write to the native standard error stream or a file instead by setting the value of the
     * environment variable (<b>not</b> system property) named {@code MAXINE_LOG_FILE}.
     * If set, the value of this environment variable is interpreted as a file path to which VM
     * output will be written. The special values {@code "stdout"} and {@code "stderr"} are
     * interpreted to mean the native standard output and error streams respectively.
     */
    public static final OutputStream os = new LogOutputStream();

    /**
     * The singleton VM print stream. This print stream sends all its output to {@link #os}.
     */
    public static final LogPrintStream out = new LogPrintStream(os);

    /**
     * The non-moving raw memory buffer used to pass data to the native log functions.
     * The log {@linkplain Log#lock() lock} must be held when using this buffer.
     */
    private static final BootMemory buffer = new BootMemory(Ints.K);

    /**
     * Equivalent to calling {@link LogPrintStream#print(String)} on {@link #out}.
     */
    public static void print(String s) {
        out.print(s);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#printSymbol(Word)} on {@link #out}.
     */
    public static void printSymbol(Word address) {
        out.printSymbol(address);
    }

    /**
     * Equivalent to calling {@link com.sun.max.vm.Log.LogPrintStream#printf(String, Object[])} on {@link #out}.
     */
    public static void print(String... arr) {
        for (String s : arr) {
            out.print(s);
        }
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(String, boolean)} on {@link #out}.
     */
    public static void print(String s, boolean withNewLine) {
        out.print(s, withNewLine);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(String, int)} on {@link #out}.
     */
    public static void print(String s, int width) {
        out.print(s, width);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(Object)} on {@link #out}.
     */
    public static void print(Object object) {
        out.print(object);
    }

    public static void flush() {
        out.flush();
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(int)} on {@link #out}.
     */
    public static void print(int i) {
        out.print(i);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(long)} on {@link #out}.
     */
    public static void print(long i) {
        out.print(i);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(char)} on {@link #out}.
     */
    public static void print(char c) {
        out.print(c);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(boolean))} on {@link #out}.
     */
    public static void print(boolean b) {
        out.print(b);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(double)} on {@link #out}.
     */
    public static void print(double d) {
        out.print(d);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(float)} on {@link #out}.
     */
    public static void print(float f) {
        out.print(f);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(char[])} on {@link #out}.
     */
    public static void print(char[] c) {
        out.print(c);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(Word)} on {@link #out}.
     */
    public static void print(Word word) {
        out.print(word);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(String)} on {@link #out}.
     */
    public static void println(String s) {
        out.println(s);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#print(String, int)} on {@link #out}.
     */
    public static void println(String s, int width) {
        out.println(s, width);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(Object)} on {@link #out}.
     */
    public static void println(Object object) {
        out.println(object);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println()} on {@link #out}.
     */
    public static void println() {
        out.println();
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(int)} on {@link #out}.
     */
    public static void println(int i) {
        out.println(i);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(long)} on {@link #out}.
     */
    public static void println(long i) {
        out.println(i);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(char)} on {@link #out}.
     */
    public static void println(char c) {
        out.println(c);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(boolean)} on {@link #out}.
     */
    public static void println(boolean b) {
        out.println(b);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(double)} on {@link #out}.
     */
    public static void println(double d) {
        out.println(d);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(float)} on {@link #out}.
     */
    public static void println(float f) {
        out.println(f);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(char[])} on {@link #out}.
     */
    public static void println(char[] c) {
        out.println(c);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#println(Word)} on {@link #out}.
     */
    public static void println(Word word) {
        out.println(word);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#printField(FieldActor, boolean)} on {@link #out}.
     */
    public static void printFieldActor(FieldActor fieldActor, boolean withNewline) {
        out.printField(fieldActor, withNewline);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#printMethod(MethodActor, boolean)} on {@link #out}.
     */
    public static void printMethod(MethodActor methodActor, boolean withNewline) {
        if (methodActor == null) {
            out.print("<no method actor>");
            if (withNewline) {
                out.println();
            }
        } else {
            out.printMethod(methodActor, withNewline);
        }
    }

    /**
     * Equivalent to calling {@link LogPrintStream#printMethod(TargetMethod, boolean)} on {@link #out}.
     */
    public static void printMethod(TargetMethod targetMethod, boolean withNewline) {
        if (targetMethod == null) {
            out.print("<no target method>");
            if (withNewline) {
                out.println();
            }
        } else {
            out.printMethod(targetMethod, withNewline);
        }
    }

    /**
     * Prints the current VM thread via a call to {@link LogPrintStream#printThread(VmThread, boolean)} on {@link #out}.
     */
    public static void printCurrentThread(boolean withNewline) {
        out.printThread(VmThread.current(), withNewline);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#printThread(VmThread, boolean)} on {@link #out}.
     */
    public static void printThread(VmThread vmThread, boolean withNewline) {
        out.printThread(vmThread, withNewline);
    }

    /**
     * Equivalent to calling {@link LogPrintStream#printThreadLocals(Pointer, boolean)} on {@link #out}.
     */
    public static void printThreadLocals(Pointer tla, boolean all) {
        out.printThreadLocals(tla, all);
    }

    private static final class LogOutputStream extends OutputStream {

        /**
         * Only a {@linkplain Log#os singleton} instance of this class exists.
         */
        private LogOutputStream() {
        }

        @HOSTED_ONLY
        private static final OutputStream hostedOutputStream;
        static {
            // Use the same environment variable as used by the native code - see Native/share/debug.c
            String path = System.getenv("MAXINE_LOG_FILE");
            if (path == null) {
                path = "stdout";
            }
            if (path.equals("stdout")) {
                hostedOutputStream = System.out;
            } else if (path.equals("stderr")) {
                hostedOutputStream = System.err;
            } else {
                try {
                    hostedOutputStream = new FileOutputStream(path);
                } catch (FileNotFoundException fileNotFoundException) {
                    throw ProgramError.unexpected("Could not open file for VM output stream: " + path, fileNotFoundException);
                }
            }
        }

        @Override
        public void write(int b) throws IOException {
            if (MaxineVM.isHosted()) {
                hostedOutputStream.write(b);
                if (b == '\n') {
                    hostedOutputStream.flush();
                }
            } else {
                log_print_char(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (MaxineVM.isHosted()) {
                hostedOutputStream.write(b, off, len);
                for (int i = (off + len) - 1; i >= off; --i) {
                    if (b[i] == '\n') {
                        hostedOutputStream.flush();
                        break;
                    }
                }
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                int i = off;
                final int end = off + len;
                while (i < end) {
                    i = CString.writeBytes(b, i, end, buffer.address(), buffer.size());
                    log_print_buffer(buffer.address());
                }
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }

    public static final class LogPrintStream extends PrintStream {

        /**
         * Prints a given char array to the log stream. The log {@linkplain Log#lock() lock}
         * must be held by the caller.
         */
        private void printChars(char[] ch) {
            int i = 0;
            while (i < ch.length) {
                i = CString.writePartialUtf8(ch, i, buffer.address(), buffer.size());
                log_print_buffer(buffer.address());
            }
        }

        /**
         * Prints symbolic information available for a given address (if any). This method
         * uses the dladdr(3) function on Unix platforms. Typical output for a call to this
         * method may be:
         * <pre>
         *
         * </pre>
         *
         * If no symbolic information is available for {@code address}, then this method is
         * equivalent to calling {@link #print(Word)} with the value of {@code address}.
         *
         * @param address
         */
        public void printSymbol(Word address) {
            if (MaxineVM.isHosted()) {
                super.print(address.toHexString());
            } else {
                log_print_symbol(address.asAddress());
            }
        }

        /**
         * Prints a given string to the log stream. The log {@linkplain Log#lock() lock}
         * must be held by the caller.
         */
        private void printString(String string) {
            final String s = string == null ? "null" : string;
            int i = 0;
            while (i < s.length()) {
                i += CString.writePartialUtf8(s, i, s.length() - i, buffer.address(), buffer.size()) - 1;
                log_print_buffer(buffer.address());
            }
        }

        /**
         * Only a {@linkplain Log#out singleton} instance of this class exists.
         */
        private LogPrintStream(OutputStream output) {
            super(output);
        }

        @Override
        public void flush() {
            if (MaxineVM.isHosted()) {
                super.flush();
            } else {
                log_flush();
            }
        }

        public void print(String s, boolean withNewline) {
            if (withNewline) {
                println(s);
            } else {
                print(s);
            }
        }

        /**
         * Prints a given string to this stream, padded by zero or more spaces.
         *
         * @param s the string to print
         * @param width the minimum number of characters to be printed, {@code w}, is the absolute value of {@code
         *            width}. The number of padding spaces printed is {@code s.length() - w}. If {@code width} is
         *            negative, then the padding spaces are printed before {@code s} otherwise they are printed after
         *            {@code s}.
         */
        public void print(String s, int width) {
            if (width < 0) {
                int padding = s.length() + width;
                while (padding-- >= 0) {
                    print(' ');
                }
            }
            print(s);
            if (width > 0) {
                int padding = s.length() - width;
                while (padding-- >= 0) {
                    print(' ');
                }
            }
        }

        /**
         * Convenience method for a call to {@link #print(String, int)} followed by {@link #println()}.
         */
        public void println(String s, int width) {
            print(s, width);
            println();
        }

        @Override
        public void print(String s) {
            if (MaxineVM.isHosted()) {
                super.print(s);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                printString(s);
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void print(Object object) {
            if (MaxineVM.isHosted()) {
                super.print(object);
            } else {
                final String string = String.valueOf(object);
                final boolean lockDisabledSafepoints = Log.lock();
                printString(string);
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void print(int i) {
            if (MaxineVM.isHosted()) {
                super.print(i);
            } else {
                // locking is not really necessary for primitives
                log_print_int(i);
            }
        }
        @Override
        public void print(long i) {
            if (MaxineVM.isHosted()) {
                super.print(i);
            } else {
                // locking is not really necessary for primitives
                log_print_long(i);
            }
        }
        @Override
        public void print(char c) {
            if (MaxineVM.isHosted()) {
                super.print(c);
            } else {
                // locking is not really necessary for primitives
                log_print_char(c);
            }
        }
        @Override
        public void print(boolean b) {
            if (MaxineVM.isHosted()) {
                super.print(b);
            } else {
                // locking is not really necessary for primitives
                log_print_boolean(b);
            }
        }
        @Override
        public void print(double d) {
            if (MaxineVM.isHosted()) {
                super.print(d);
            } else {
                // locking is not really necessary for primitives
                log_print_double(d);
            }
        }
        @Override
        public void print(float f) {
            if (MaxineVM.isHosted()) {
                super.print(f);
            } else {
                // locking is not really necessary for primitives
                log_print_float(f);
            }
        }
        @Override
        public void print(char[] c) {
            if (MaxineVM.isHosted()) {
                super.print(c);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                printChars(c);
                Log.unlock(lockDisabledSafepoints);
            }
        }
        public void print(Word word) {
            if (MaxineVM.isHosted()) {
                super.print(word.toHexString());
            } else {
                // locking is not really necessary for primitives
                log_print_word(word);
            }
        }
        @Override
        public void println(String s) {
            if (MaxineVM.isHosted()) {
                super.println(s);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                printString(s);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void println() {
            if (MaxineVM.isHosted()) {
                super.println();
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void println(int i) {
            if (MaxineVM.isHosted()) {
                super.println(i);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                log_print_int(i);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void println(long i) {
            if (MaxineVM.isHosted()) {
                super.println(i);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                log_print_long(i);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void println(char c) {
            if (MaxineVM.isHosted()) {
                super.println(c);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                log_print_char(c);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void println(boolean b) {
            if (MaxineVM.isHosted()) {
                super.println(b);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                log_print_boolean(b);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void println(double d) {
            if (MaxineVM.isHosted()) {
                super.println(d);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                log_print_double(d);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void println(float f) {
            if (MaxineVM.isHosted()) {
                super.println(f);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                log_print_float(f);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void println(char[] c) {
            if (MaxineVM.isHosted()) {
                super.println(c);
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                printChars(c);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }
        @Override
        public void println(Object object) {
            if (MaxineVM.isHosted()) {
                super.println(object);
            } else {
                final String string = String.valueOf(object);
                final boolean lockDisabledSafepoints = Log.lock();
                printString(string);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }

        public void println(Word word) {
            if (MaxineVM.isHosted()) {
                super.println(word.toHexString());
            } else {
                final boolean lockDisabledSafepoints = Log.lock();
                log_print_word(word);
                log_print_newline();
                Log.unlock(lockDisabledSafepoints);
            }
        }

        /**
         * Convenience routine for printing a {@link FieldActor} to this stream. The output is of the form:
         *
         * <pre>
         *     &lt;holder&gt;.&lt;name&gt;:&lt;descriptor&gt;
         * </pre>
         *
         * For example, the output for {@link System#err} is:
         *
         * <pre>
         * &quot;java.lang.System.err:Ljava/io/PrintStream;&quot;
         * </pre>
         * @param fieldActor the field actor to print
         * @param withNewline specifies if a newline should be appended to the stream after the field actor
         */
        public void printField(FieldActor fieldActor, boolean withNewline) {
            boolean lockDisabledSafepoints = false;
            if (!MaxineVM.isHosted()) {
                lockDisabledSafepoints = lock();
            }
            print(fieldActor.holder().name.string);
            print('.');
            print(fieldActor.name.string);
            print(":");
            print(fieldActor.descriptor().string, withNewline);
            if (!MaxineVM.isHosted()) {
                unlock(lockDisabledSafepoints);
            }
        }

        /**
         * Convenience routine for printing a {@link MethodActor} to this stream. The output is of the form:
         *
         * <pre>
         *     &lt;holder&gt;.&lt;name&gt;&lt;descriptor&gt;
         * </pre>
         *
         * For example, the output for {@link Runnable#run()} is:
         *
         * <pre>
         * &quot;java.lang.Runnable.run()V&quot;
         * </pre>
         * @param methodActor the method actor to print
         * @param withNewline specifies if a newline should be appended to the stream after the method actor
         */
        public void printMethod(MethodActor methodActor, boolean withNewline) {
            boolean lockDisabledSafepoints = false;
            if (!MaxineVM.isHosted()) {
                lockDisabledSafepoints = lock();
            }
            print(methodActor.holder().name.string);
            print('.');
            print(methodActor.name.string);
            print(methodActor.descriptor().string, withNewline);
            if (!MaxineVM.isHosted()) {
                unlock(lockDisabledSafepoints);
            }
        }

        /**
         * Convenience routine for printing a {@link TargetMethod} to this stream. If the target method has a non-null
         * {@linkplain TargetMethod#classMethodActor}, then the output is of the form:
         *
         * <pre>
         *     &lt;holder&gt;.&lt;name&gt;&lt;descriptor&gt; '{'&lt;TargetMethod class&gt;'@'&lt;code start address&gt;'}'
         * </pre>
         *
         * Otherwise, it is of the form:
         *
         * <pre>
         *     &lt;description&gt; '{'&lt;TargetMethod class&gt;'@'&lt;code start address&gt;'}'
         * </pre>
         *
         * @param tm the target method to print
         * @param withNewline specifies if a newline should be appended to the stream after the target method
         */
        public void printMethod(TargetMethod tm, boolean withNewline) {
            if (tm.classMethodActor != null) {
                printMethod(tm.classMethodActor, false);
            } else {
                print(tm.regionName(), false);
            }

            Log.print(" {");
            String tmClass = ObjectAccess.readClassActor(tm).name.string;
            // cannot use substring as it allocates
            int index = tmClass.lastIndexOf('.');
            for (int i = index + 1; i < tmClass.length(); i++) {
                Log.print(tmClass.charAt(i));
            }
            Log.print('@');
            Log.print(tm.codeStart());
            Log.print('}');
            if (withNewline) {
                println();
            }
        }

        /**
         * Convenience routine for printing a {@link VmThread} to this stream. The output is of the form:
         *
         * <pre>
         *     &lt;name&gt;[&lt;id&gt;]
         * </pre>
         *
         * For example, the output for the main thread of execution may be:
         *
         * <pre>
         * &quot;main[id=1]&quot;
         * </pre>
         * @param vmThread the thread to print
         * @param withNewline specifies if a newline should be appended to the stream after the thread
         */
        public void printThread(VmThread vmThread, boolean withNewline) {
            boolean lockDisabledSafepoints = false;
            if (!MaxineVM.isHosted()) {
                lockDisabledSafepoints = lock();
            }
            print(vmThread == null ? "<null thread>" : vmThread.getName());
            print("[id=");
            if (vmThread == null) {
                print("?");
            } else {
                print(vmThread.id());
            }
            print("]", withNewline);
            if (!MaxineVM.isHosted()) {
                unlock(lockDisabledSafepoints);
            }
        }

        /**
         * Convenience routine for printing {@linkplain VmThreadLocal VM thread locals} to this stream.
         *
         * @param tla a pointer to VM thread locals
         * @param all specifies if all 3 {@linkplain VmThreadLocal TLS} areas are to be printed
         */
        public void printThreadLocals(Pointer tla, boolean all) {
            boolean lockDisabledSafepoints = false;
            if (!MaxineVM.isHosted()) {
                lockDisabledSafepoints = lock();
            }
            if (!all) {
                final List<VmThreadLocal> values = VmThreadLocal.values();
                for (int i = 0; i != values.size(); i++) {
                    final VmThreadLocal vmThreadLocal = values.get(i);
                    for (int j = 0; j < 45 - vmThreadLocal.name.length(); j++) {
                        print(' ');
                    }
                    print(vmThreadLocal.name);
                    print(": ");
                    vmThreadLocal.log(this, tla, false);
                    println();
                }
            } else {
                final List<VmThreadLocal> values = VmThreadLocal.values();
                final Pointer enabled = ETLA.load(tla);
                final Pointer disabled = DTLA.load(tla);
                final Pointer triggered = TTLA.load(tla);
                for (int i = 0; i != values.size(); i++) {
                    final VmThreadLocal vmThreadLocal = values.get(i);
                    for (int j = 0; j < 45 - vmThreadLocal.name.length(); j++) {
                        print(' ');
                    }
                    print(vmThreadLocal.name);
                    print(": {E} ");
                    vmThreadLocal.log(this, enabled, false);
                    print("  {D} ");
                    vmThreadLocal.log(this, disabled, false);
                    print("  {T} ");
                    vmThreadLocal.log(this, triggered, false);
                    println();
                }
            }
            if (!MaxineVM.isHosted()) {
                unlock(lockDisabledSafepoints);
            }
        }
    }

    private static VmThread lockOwner;
    private static int lockDepth;

    /**
     * Gets the thread that current holds the log lock.
     */
    public static VmThread lockOwner() {
        return lockOwner;
    }

    /**
     * Attempts to acquire the global lock on all debug output, blocking until the lock is successfully acquired. This
     * lock can be acquired recursively by a thread. The lock is not released for other threads until the thread that
     * owns the lock calls {@link #unlock} the same number of times it called this method.
     *
     * This method ensures that safepoints are disabled before it returns.
     *
     * @return true if this call caused safepoints to be disabled (i.e. they were enabled upon entry to this method).
     *         This value must be passed to the paired call to {@link #unlock} so that safepoints are restored to the
     *         appropriate state (i.e. the state they had before the sequence of code protected by this lock was
     *         entered).
     */
    public static boolean lock() {
        if (isHosted()) {
            return true;
        }

        boolean wasDisabled = SafepointPoll.disable();
        Log.log_lock();
        if (lockDepth == 0) {
            FatalError.check(lockOwner == null, "log lock should have no owner with depth 0");
            lockOwner = VmThread.current();
        }
        lockDepth++;
        return !wasDisabled;
    }

    /**
     * Attempts to releases the global lock on all debug output. This must only be called by a thread that currently
     * owns the lock - failure to do so causes the VM to exit. The lock is not released for other threads until this
     * method is called the same number of times as {@link #lock()} was called when acquiring the lock.
     *
     * @param lockDisabledSafepoints specifies if the adjoining call to {@link #lock()} disabled safepoints. If so, then
     *            this call will re-enable them.
     */
    public static void unlock(boolean lockDisabledSafepoints) {
        if (isHosted()) {
            return;
        }

        --lockDepth;
        FatalError.check(lockOwner == VmThread.current(), "log lock should be owned by current thread");
        if (lockDepth == 0) {
            lockOwner = null;
        }
        Log.log_unlock();
        ProgramError.check(SafepointPoll.isDisabled(), "Safepoints must not be re-enabled in code surrounded by Debug.lock() and Debug.unlock()");
        if (lockDisabledSafepoints) {
            SafepointPoll.enable();
        }
    }

    static {
        ProgramWarning.setHandler(new Handler() {
            public void handle(String message) {
                if (MaxineVM.isHosted()) {
                    System.err.println(message);
                } else {
                    Log.println(message);
                }
            }
        });
    }
}
