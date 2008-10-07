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
/*VCSID=72344d35-eff3-46ee-9df4-dd959afeb26b*/
package util;

/**
 * Test class supplied with Mantis Bug#3.
 *
 * @author Michael Van De Vanter
 *
 * Reproduce: Set the breakpoint at f.dummy() in the <program> below. When the program hits the breakpoint, f.b()
 * should be re-optimized.
 *
 * We cannot inspect the re-optimized code for f.b(). The following error is produced:
 *
 * Caused by: java.lang.NullPointerException at com.sun.max.vm.jni.MethodID.toMethodActor(MethodID.java:50) at
 * com.sun.max.vm.actor.member.MethodActor.read(MethodActor.java:464) at
 * com.sun.max.vm.compiler.target.TargetJavaFrameDescriptor.inflateForMemoizer(TargetJavaFrameDescriptor.java:187) at
 * com.sun.max.vm.compiler.target.TargetJavaFrameDescriptor.inflate(TargetJavaFrameDescriptor.java:255) at
 * com.sun.max.ins.tele.TeleTargetMethod$1.call(TeleTargetMethod.java:180) at
 * com.sun.max.ins.tele.TeleTargetMethod$1.call(TeleTargetMethod.java:1) at
 * com.sun.max.ins.type.InspectorClassRegistry.usingTeleClassIDs(InspectorClassRegistry.java:104) ... 47 more
 */
public class MantisBug3Test {

    static class S {

        public int _x = 12;

        public int c() {
            return _x++;
        }

        public int b() {
            final int x = this.c();
            final int y = x + x;
            return y;
        }

        public void dummy() {
            _x++;
        }
    }

    public static void main(String[] args) {
        System.out.println("Hello World!");
        final S f = new S();
        String s = "";
        int i;
        for (i = 0; i < 10000; i++) {
            s += f.b();
        }
        f.dummy();
        f.b();
        System.out.println(s.length());
        System.gc();
        System.out.println("Hello World!");
    }
}
