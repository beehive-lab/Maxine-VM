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
package test.com.sun.max.vm.compiler;

import com.sun.max.vm.compiler.ir.*;

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
        compileMethod(Class.class, "getName");
    }

    public void test_basePackages() {
        compilePackages(CompilerTestSetup.javaPrototype().basePackages());
    }

    public void test_vmPackages() {
        compilePackages(CompilerTestSetup.javaPrototype().vmPackages());
    }

    public void test_asmPackages() {
        compilePackages(CompilerTestSetup.javaPrototype().asmPackages());
    }
}
