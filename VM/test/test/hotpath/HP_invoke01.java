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
// Checkstyle: stop

package test.hotpath;
/*
 * @Harness: java
 * @Runs: 40 = 3049480; 80 = 6098960;
 */
public class HP_invoke01 {
    private static int _sum;

    public static int test(int count) {
        _sum = 0;
        final Instruction[] instructions = new Instruction [] {new Instruction.Add(), new Instruction.Sub(), new Instruction.Mul(), new Instruction.Div()};
        final Visitor v = new Visitor();
        for (int i = 0; i < count; i++) {
            instructions[i % 4].accept(v);
        }
        return _sum;
    }

    public static abstract class Instruction {
        public abstract void accept(Visitor v);

        public static abstract class Binary extends Instruction {

        }

        public static class Add extends Binary {
            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }

        public static class Sub extends Binary {
            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }

        public static class Mul extends Binary {
            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }

        public static class Div extends Binary {
            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }
    }

    public static class Visitor {
        public void visit(Instruction.Add i) {
            _sum += 7;
        }

        public void visit(Instruction.Sub i) {
            _sum += 194127;
        }

        public void visit(Instruction.Mul i) {
            _sum += 18991;
        }

        public void visit(Instruction.Div i) {
            _sum += 91823;
        }
    }
}
