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
/*VCSID=7b0f6d30-2759-4672-a03d-18c4e63bf9fc*/

package test.com.sun.max.vm.compiler;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.ir.*;

public abstract class CompilerTest_enumSwitch<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected CompilerTest_enumSwitch(String name) {
        super(name);
    }

    private enum E {
        A, B, C, D
    }

    private static void perform_enumSwitch(E e) {
        switch (e) {
            case A:
                break;
            default:
                break;
        }
    }

    public void test_SWITCH_TABLE() {
        final ClassMethodActor classMethodActor = getEnumSwitchTranslationTableInitializer(CompilerTest_enumSwitch.class, E.class);
        if (classMethodActor != null) {
            compileMethod(classMethodActor);
        }
    }

    public void test_this() {
        compileMethod(CompilerTest_enumSwitch.class, "perform_enumSwitch");
    }

    public void test_classesWithEnumSwitchStatements() {
        compileClass(ClassfileReader.class);
    }
}
