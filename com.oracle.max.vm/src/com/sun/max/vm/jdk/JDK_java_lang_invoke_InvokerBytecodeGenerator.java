/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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

package com.sun.max.vm.jdk;

import com.sun.max.annotate.ALIAS;
import com.sun.max.annotate.METHOD_SUBSTITUTIONS;
import com.sun.max.annotate.SUBSTITUTE;

import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

@METHOD_SUBSTITUTIONS(className = "java.lang.invoke.InvokerBytecodeGenerator")
public final class JDK_java_lang_invoke_InvokerBytecodeGenerator {

    private final static HashMap<String, Integer> UNIQUE_CLASS_NAME_COUNTER = new HashMap<>();

    @ALIAS(declaringClassName = "java.lang.invoke.InvokerBytecodeGenerator")
    private static String     superName;
    @ALIAS(declaringClassName = "java.lang.invoke.InvokerBytecodeGenerator")
    private        Map        cpPatches;
    @ALIAS(declaringClassName = "java.lang.invoke.InvokerBytecodeGenerator")
    private        String     className;
    @ALIAS(declaringClassName = "java.lang.invoke.InvokerBytecodeGenerator")
    private        String     sourceFile;
    @ALIAS(declaringClassName = "java.lang.invoke.InvokerBytecodeGenerator", descriptor = "Ljava/lang/invoke/LambdaForm;")
    private        Object     lambdaForm;
    @ALIAS(declaringClassName = "java.lang.invoke.InvokerBytecodeGenerator")
    private        String     invokerName;
    @ALIAS(declaringClassName = "java.lang.invoke.InvokerBytecodeGenerator")
    private        MethodType invokerType;
    @ALIAS(declaringClassName = "java.lang.invoke.InvokerBytecodeGenerator")
    private        int[]      localsMap;

    /**
     * Customized copy of {@code makeDumpableClassName}.
     */
    private static String getUniqueClassName(String className) {
        Integer ctr;
        synchronized (UNIQUE_CLASS_NAME_COUNTER) {
            ctr = UNIQUE_CLASS_NAME_COUNTER.get(className);
            if (ctr == null) ctr = 0;
            UNIQUE_CLASS_NAME_COUNTER.put(className, ctr + 1);
        }
        StringBuilder sfx = new StringBuilder(ctr.toString());
        while (sfx.length() < 3)
            sfx.insert(0, "0");
        className += sfx;
        return className;
    }

    /**
     * Override of the {@link java.lang.invoke.InvokerBytecodeGenerator} constructor to assign unique class names to
     * the synthetic classes of lambdas.
     *
     * @param lambdaForm
     * @param localsMapSize
     * @param className
     * @param invokerName
     * @param invokerType
     */
    @SUBSTITUTE(constructor = true, signatureDescriptor = "(Ljava/lang/invoke/LambdaForm;ILjava/lang/String;Ljava/lang/String;Ljava/lang/invoke/MethodType;)V")
    private void InvokerBytecodeGenerator(Object lambdaForm, int localsMapSize,
                                          String className, String invokerName, MethodType invokerType) {
        if (invokerName.contains(".")) {
            int p = invokerName.indexOf(".");
            className = invokerName.substring(0, p);
            invokerName = invokerName.substring(p + 1);
        }
        className = getUniqueClassName(className);
        this.className = superName + "$" + className;
        this.sourceFile = "LambdaForm$" + className;
        this.lambdaForm = lambdaForm;
        this.invokerName = invokerName;
        this.invokerType = invokerType;
        this.localsMap = new int[localsMapSize];

        // When substituting a constructor the non-static initializers of the original class are no longer invoked, thus
        // we need to initialize cpPatches explicitly here
        cpPatches = new HashMap<>();
    }
}
