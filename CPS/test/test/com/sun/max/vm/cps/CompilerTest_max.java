/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps;

import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.jdk.*;

/**
 * Translates almost all of the packages in the project to test the translator.
 *
 * @author Bernd Mathiske
 */
public abstract class CompilerTest_max<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    public CompilerTest_max(String name) {
        super(name);
    }

    /**
     * Stub testing takes too long when compiling so many classes which prevents the auto-tests from completing in a timely manner.
     */
    @Override
    protected boolean shouldTestStubs() {
        return false;
    }

    public void test_1() {
        compileMethod(JDK_java_lang_Throwable.class, "fillInStackTrace");
    }

//    public void test_basePackages() {
//        compilePackages(CompilerTestSetup.javaPrototype().basePackages());
//    }
//
//    public void test_BootImagePackages() {
//        compilePackages(CompilerTestSetup.javaPrototype().BootImagePackages());
//    }
//
//    public void test_asmPackages() {
//        compilePackages(CompilerTestSetup.javaPrototype().asmPackages());
//    }
}
