/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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

import com.sun.max.vm.actor.holder.ClassActor;

/*
 * @Harness: java
 * @Runs: 0=true; 1=true; 2=true; 3=true; 4=true; 5=false
 */
public class Hub_Subtype01 {
    public static boolean test(int arg) {
        Object obj = null;
        if (arg == 0) {
            obj = Hub_Subtype01.class;
        } else if (arg == 1) {
            obj = ClassActor.fromJava(Hub_Subtype01.class);
        } else if (arg == 2) {
            obj = ClassActor.fromJava(Hub_Subtype01.class).dynamicHub();
        } else if (arg == 3) {
            obj = ClassActor.fromJava(Hub_Subtype01.class).staticHub();
        } else if (arg == 4) {
            obj = ClassActor.fromJava(Hub_Subtype01.class).staticTuple();
        }
        return obj instanceof Object;
    }
}
