/*
 * Copyright (c) 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.oracle.max.vm.ext.maxri;

import com.oracle.max.asm.target.aarch64.Aarch64;
import com.oracle.max.asm.target.amd64.AMD64;
import com.oracle.max.asm.target.armv7.ARMV7;
import com.oracle.max.asm.target.riscv64.RISCV64;
import com.sun.cri.ci.CiRegister;
import com.sun.cri.ci.CiTargetMethod;
import com.sun.cri.ci.CiUtil;
import com.sun.max.lang.ISA;
import com.sun.max.platform.Platform;
import com.sun.max.vm.compiler.target.Safepoints;
import com.sun.max.vm.runtime.FatalError;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.max.platform.Platform.target;

/**
 * A provider that uses the {@code GNU objdump} utility to disassemble code.
 */
public class ObjdumpDisassembler {

    public static String disassemble(CiTargetMethod tm) {
        File tmp = null;
        try {
            tmp = File.createTempFile("compiledBinary", ".bin");
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(tm.targetCode(), 0, tm.targetCodeSize());
            }

            final Platform platform = Platform.platform();
            CiRegister fp;
            int refMapToFPOffset = 0;
            if (platform.isa == ISA.AMD64) {
                fp = AMD64.rsp;
            } else if (platform.isa == ISA.ARM) {
                fp = ARMV7.r13;
            } else if (platform.isa == ISA.Aarch64) {
                fp = Aarch64.sp;
            } else if (platform.isa == ISA.RISCV64) {
                fp = RISCV64.sp;
            } else {
                throw FatalError.unimplemented("com.oracle.max.vm.ext.maxri.MaxRuntime.objdump(com.sun.cri.ci.CiTargetMethod, com.oracle.max.vm.ext.maxri.MaxTargetMethod)");
            }

            Map<Integer, String> annotations = new HashMap<>();
            addAnnotaions(tm.annotations(), annotations);
            addExceptionHandlersComment(tm, annotations);
            CiUtil.RefMapFormatter slotFormatter = new CiUtil.RefMapFormatter(target().arch, target().spillSlotSize, fp, refMapToFPOffset);
            for (CiTargetMethod.Safepoint safepoint : tm.safepoints) {
                if (safepoint instanceof CiTargetMethod.Call) {
                    CiTargetMethod.Call call = (CiTargetMethod.Call) safepoint;
                    if (call.debugInfo != null) {
                        putAnnotation(annotations, Safepoints.safepointPosForCall(call.pcOffset, call.size), CiUtil.append(new StringBuilder(100), call.debugInfo, slotFormatter).toString());
                    }
                    putAnnotation(annotations, call.pcOffset, "{" + call.target + "}");
                } else {
                    if (safepoint.debugInfo != null) {
                        putAnnotation(annotations, safepoint.pcOffset, CiUtil.append(new StringBuilder(100), safepoint.debugInfo, slotFormatter).toString());
                    }
                    putAnnotation(annotations, safepoint.pcOffset, "{safepoint}");
                }
            }
            for (CiTargetMethod.DataPatch site : tm.dataReferences) {
                putAnnotation(annotations, site.pcOffset, "{" + site.constant + "}");
            }

            return objdump(tmp, annotations, 0);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
    }

    public static void addAnnotaions(List<CiTargetMethod.CodeAnnotation> codeAnnotations, Map<Integer, String> annotations) {
        if (codeAnnotations == null || codeAnnotations.isEmpty()) {
            return;
        }
        for (CiTargetMethod.CodeAnnotation a : codeAnnotations) {
            if (a instanceof CiTargetMethod.JumpTable) {
                System.err.println("WARNING: Ignoring jump tables in disassemble");
            } else if (a instanceof CiTargetMethod.LookupTable) {
                System.err.println("WARNING: Ignoring lookup tables in disassemble");
            } else if (a instanceof CiTargetMethod.CodeComment) {
                CiTargetMethod.CodeComment comment = (CiTargetMethod.CodeComment) a;
                putAnnotation(annotations, comment.position, comment.value);
            }
        }
    }

    private static void addExceptionHandlersComment(CiTargetMethod tm, Map<Integer, String> annotations) {
        if (!tm.exceptionHandlers.isEmpty()) {
            String nl = System.getProperty("line.separator");
            StringBuilder buf = new StringBuilder("------ Exception Handlers ------").append(nl);
            for (CiTargetMethod.ExceptionHandler e : tm.exceptionHandlers) {
                buf.append("    ").
                        append(e.pcOffset).append(" -> ").
                        append(e.handlerPos).
                        append("  ").append(e.exceptionType == null ? "<any>" : e.exceptionType).
                        append(nl);
            }
            putAnnotation(annotations, 0, buf.toString());
        }
    }

    public static String disassemble(byte[] code, long startAddress) {
        File tmp = null;
        try {
            tmp = File.createTempFile("compiledBinary", ".bin");
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(code);
            }

            return objdump(tmp, null, startAddress);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
    }

    private static String objdump(File binaryFile, Map<Integer, String> annotations, long startAddress) throws IOException {
        final String[] cmdline;
        final Platform platform = Platform.platform();
        if (platform.isa == ISA.AMD64) {
            cmdline = new String[]{"objdump", "-D", "-b", "binary", "-M", "x86-64", "-m", "i386", binaryFile.getAbsolutePath()};
        } else if (platform.isa == ISA.ARM) {
            cmdline = new String[]{"arm-none-eabi-objdump", "-D", "-b", "binary", "-m", "arm", binaryFile.getAbsolutePath()};
        } else if (platform.isa == ISA.Aarch64) {
            cmdline = new String[]{"aarch64-linux-gnu-objdump", "-D", "-b", "binary", "-m", "aarch64", binaryFile.getAbsolutePath()};
        } else if (platform.isa == ISA.RISCV64) {
            cmdline = new String[]{"riscv64-linux-gnu-objdump", "-D", "-b", "binary", "-m", "riscv:rv64", binaryFile.getAbsolutePath()};
        } else {
            throw FatalError.unimplemented("com.oracle.max.vm.ext.maxri.MaxRuntime.objdump(com.sun.cri.ci.CiTargetMethod, com.oracle.max.vm.ext.maxri.MaxTargetMethod)");
        }

        Process proc = Runtime.getRuntime().exec(cmdline);
        InputStream is = proc.getInputStream();
        StringBuilder sb = new StringBuilder();
        Pattern hexAddressPattern = Pattern.compile(" *([0-9a-fA-F]+)(:\t.*)");

        InputStreamReader isr = new InputStreamReader(is);
        try (BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = hexAddressPattern.matcher(line);
                if (m.find()) {
                    int address = Integer.parseInt(m.group(1), 16);
                    if (annotations != null) {
                        String annotation = annotations.get(address);
                        if (annotation != null) {
                            annotation = annotation.replace("\n", "\n; ");
                            sb.append("; ").append(annotation).append('\n');
                        }
                    }
                    line = m.replaceFirst("0x" + Long.toHexString(address + startAddress) + "$2");
                    sb.append(line).append("\n");
                }
            }
        }
        try (BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
            String errLine = ebr.readLine();
            if (errLine != null) {
                System.err.print("Error output from executing: ");
                for (int i = 0; i < cmdline.length; i++) {
                    System.err.print(quoteShellArg(cmdline[i]) + " ");
                }
                System.err.println();
                System.err.println(errLine);
                while ((errLine = ebr.readLine()) != null) {
                    System.err.println(errLine);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Pattern for a single shell command argument that does not need to quoted.
     */
    private static final Pattern SAFE_SHELL_ARG = Pattern.compile("[A-Za-z0-9@%_\\-\\+=:,\\./]+");

    /**
     * Reliably quote a string as a single shell command argument.
     */
    public static String quoteShellArg(String arg) {
        if (arg.isEmpty()) {
            return "\"\"";
        }
        Matcher m = SAFE_SHELL_ARG.matcher(arg);
        if (m.matches()) {
            return arg;
        }
        // See http://stackoverflow.com/a/1250279
        return "'" + arg.replace("'", "'\"'\"'") + "'";
    }

    /**
     * Searches for a valid GNU objdump executable.
     */
    private static String getObjdump() {
        // On macOS, `brew install binutils` will provide
        // an executable named gobjdump
        for (String candidate : new String[]{"objdump", "gobjdump"}) {
            try {
                String[] cmd = {candidate, "--version"};
                Process proc = Runtime.getRuntime().exec(cmd);
                InputStream is = proc.getInputStream();
                int exitValue = proc.waitFor();
                if (exitValue == 0) {
                    byte[] buf = new byte[is.available()];
                    int pos = 0;
                    while (pos < buf.length) {
                        int read = is.read(buf, pos, buf.length - pos);
                        pos += read;
                    }
                    String output = new String(buf);
                    if (output.contains("GNU objdump")) {
                        return candidate;
                    }
                }
            } catch (IOException | InterruptedException e) {
            }
        }
        return null;
    }

    private static void putAnnotation(Map<Integer, String> annotations, int idx, String txt) {
        String newAnnotation = annotations.getOrDefault(idx, "") + "\n" + txt;
        annotations.put(idx, newAnnotation);
    }

    public String getName() {
        return "hsdis-objdump";
    }
}
