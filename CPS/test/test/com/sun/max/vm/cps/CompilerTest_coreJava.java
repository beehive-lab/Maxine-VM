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
package test.com.sun.max.vm.cps;

import java.util.*;

import com.sun.max.*;
import com.sun.max.config.*;
import com.sun.max.vm.cps.ir.*;

/**
 * Translates almost all of the packages in the project to test the translator.
 *
 * @author Bernd Mathiske
 */
public abstract class CompilerTest_coreJava<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    public CompilerTest_coreJava(String name) {
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
        compileMethod(Hashtable.class, "clone");
    }

    public void test_beans() {
        compilePackage(MaxPackage.fromJava("java.beans"));
    }

    public void test_reflect() {
        compilePackage(MaxPackage.fromJava("java.lang.reflect"));
    }

    public void test_net() {
        compilePackage(MaxPackage.fromJava("java.net"));
    }

    public void test_nio() {
        compilePackage(MaxPackage.fromJava("java.nio"));
    }

    public void test_security() {
        compilePackage(MaxPackage.fromJava("java.security"));
    }

    public void test_lang() {
        compilePackage(MaxPackage.fromJava("java.lang"));
    }

    public void test_util() {
        compilePackage(MaxPackage.fromJava("java.util"));
    }

    public void test_io() {
        compilePackage(MaxPackage.fromJava("java.io"));
    }

}
