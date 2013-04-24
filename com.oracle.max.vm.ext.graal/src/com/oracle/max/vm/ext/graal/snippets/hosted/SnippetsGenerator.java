/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.snippets.hosted;

import java.io.*;

import com.oracle.graal.api.meta.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;


public abstract class SnippetsGenerator {

    protected static final String UNSAFE_CAST_BEFORE = "UnsafeCastNode.unsafeCast(";
    protected static final String UNSAFE_CAST_AFTER = ", StampFactory.forNodeIntrinsic())";

    protected PrintStream out;
    private ByteArrayOutputStream baos;

    protected SnippetsGenerator() {
        this.baos = new ByteArrayOutputStream();
        this.out = new PrintStream(baos);
    }


    protected boolean generate(boolean checkOnly, Class<?> target) throws IOException {
        File base = new File(JavaProject.findWorkspace(), "com.oracle.max.vm.ext.graal/src");
        File outputFile = new File(base, target.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        doGenerate();
        ReadableSource content = ReadableSource.Static.fromString(baos.toString());
        boolean result = Files.updateGeneratedContent(outputFile, content, "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
        if (result) {
            System.out.println("Source for " + target + (checkOnly ? " would be" : " was") + " updated");
        }
        return result;
    }

    /**
     * Returns the argument with first character upper-cased.
     * @param s
     */
    protected String toFirstUpper(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    protected String replace(String template, String param, String arg) {
        return template.replaceAll(param, arg);
    }

    protected String replaceKinds(String template, Kind kind) {
        String uJavaName = toFirstUpper(kind.getJavaName());
        String result = replace(template, "#UKIND#", uJavaName);
        return replace(result, "#KIND#", kind.getJavaName());
    }

    protected String replaceUCast(String template, Kind kind) {
        String ucb = kind != Kind.Object ? "" : UNSAFE_CAST_BEFORE;
        String uca = kind != Kind.Object ? "" : UNSAFE_CAST_AFTER;
        return replace(replace(template, "#UCA#", uca), "#UCB#", ucb);
    }

    protected boolean notVoidOrIllegal(Kind kind) {
        return !(kind == Kind.Void || kind == Kind.Illegal);
    }

    protected abstract void doGenerate() throws IOException;


}
