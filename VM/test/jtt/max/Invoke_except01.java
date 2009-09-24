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
package jtt.max;

import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.value.Value;
import com.sun.max.vm.value.ReferenceValue;

import java.lang.reflect.*;

/*
* @Harness: java
* @Runs: 0=0; 1=3; 2=!java.lang.reflect.InvocationTargetException; 3=!java.lang.IllegalArgumentException; 4=!java.lang.IllegalArgumentException
*/
public class Invoke_except01 {

    public static int test(int arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Value[] args;
        if (arg == 0) {
            args = new Value[] {ReferenceValue.from(new int[0])};
        } else if (arg == 1) {
            args = new Value[] {ReferenceValue.from(new int[3])};
        } else if (arg == 2) {
            args = new Value[] {ReferenceValue.NULL};
        } else if (arg == 3) {
            args = new Value[] {ReferenceValue.from(new char[3])};
        } else {
            args = null;
        }
        for (Method m : Invoke_except01.class.getDeclaredMethods()) {
            if ("method".equals(m.getName())) {
                ClassMethodActor cma = ClassMethodActor.fromJava(m);
                return cma.invoke(args).toInt();
            }
        }
        return 42;
    }

    public static int method(int[] arg) {
        return arg.length;
    }
}
