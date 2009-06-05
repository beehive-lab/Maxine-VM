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
package test.com.sun.max.vm.verifier;

/**
 * This class triggers a bug in the Eclipse JDT compiler up to version 0.883_R34x at least.
 * The StackMapTable produced for {@link #willNotVerify()} is broken and causes a VerifyError
 * under HotSpot when the '-Xverify:all' and '-XX:-FailOverToOldVerifier' options are used.
 *
 * @author Doug Simon
 */
class JdtBadStackMapTable {
    public static void main(String[] args) {
        System.out.println("ok");
    }

    private static int willNotVerify() {
        int limit = 100;
        int match;
        final int result = 200;

        do {
            if (limit > 0) {
                continue;
            }

            match = 0;

            while (++match < 100) {
                // empty
            }

        } while (--limit != 0);

        return result;
    }
}
