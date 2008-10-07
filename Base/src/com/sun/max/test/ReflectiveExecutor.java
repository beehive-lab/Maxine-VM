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
/**
 * 
 */
package com.sun.max.test;

import java.lang.reflect.*;

import com.sun.max.program.*;
import com.sun.max.test.JavaExecHarness.*;
import com.sun.max.util.*;

public class ReflectiveExecutor implements Executor {
    @Override
    public void initialize(JavaTestCase c, boolean loadingPackages) {
        for (Method m : c._class.getDeclaredMethods()) {
            if (m.getName().equals("test") && (m.getModifiers() & Modifier.STATIC) != 0) {
                c._slot1 = m;
                return;
            }
        }
        ProgramError.unexpected("could not find static test() method");
    }

    @Override
    public Object execute(JavaExecHarness.JavaTestCase c, Object[] vals) throws InvocationTargetException {
        try {
            final Method m = (Method) c._slot1;
            return m.invoke(c._class, vals);
        } catch (IllegalArgumentException e) {
            throw ProgramError.unexpected(e);
        } catch (IllegalAccessException e) {
            throw ProgramError.unexpected(e);
        }
    }

    public static void main(String[] args) {
        final Registry<TestHarness> reg = new Registry<TestHarness>(TestHarness.class, true);
        final JavaExecHarness javaExecHarness = new JavaExecHarness(new ReflectiveExecutor());
        reg.registerObject("java", javaExecHarness);
        final TestEngine e = new TestEngine(reg);
        e.parseAndRunTests(args);
        e.report(System.out);
    }
}
