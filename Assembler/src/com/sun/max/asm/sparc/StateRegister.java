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
package com.sun.max.asm.sparc;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * The argument to the Write State Register and Read State Register instructions.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class StateRegister extends AbstractSymbolicArgument {

    StateRegister(int value) {
        super(value);
    }

    public static class Writable extends StateRegister {
        Writable(int value) {
            super(value);
        }
    }

    public static final class ASR extends Writable {
        private ASR(int value) {
            super(value);
        }
    }

    /**
     * @return true if this is the Y register or an Ancillary State register
     */
    public boolean isYorASR() {
        return this == Y || value() >= 16 && value() <= 31;
    }

    public static final Writable Y = new Writable(0);
    public static final Writable CCR = new Writable(2);
    public static final Writable ASI = new Writable(3);
    public static final StateRegister TICK = new StateRegister(4);
    public static final StateRegister PC = new StateRegister(5);
    public static final Writable FPRS = new Writable(6);
    public static final ASR ASR16 = new ASR(16);
    public static final ASR ASR17 = new ASR(17);
    public static final ASR ASR18 = new ASR(18);
    public static final ASR ASR19 = new ASR(19);
    public static final ASR ASR20 = new ASR(20);
    public static final ASR ASR21 = new ASR(21);
    public static final ASR ASR22 = new ASR(22);
    public static final ASR ASR23 = new ASR(23);
    public static final ASR ASR24 = new ASR(24);
    public static final ASR ASR25 = new ASR(25);
    public static final ASR ASR26 = new ASR(26);
    public static final ASR ASR27 = new ASR(27);
    public static final ASR ASR28 = new ASR(28);
    public static final ASR ASR29 = new ASR(29);
    public static final ASR ASR30 = new ASR(30);
    public static final ASR ASR31 = new ASR(31);

    public static final Symbolizer<StateRegister> SYMBOLIZER = Symbolizer.Static.initialize(StateRegister.class);
    public static final Symbolizer<Writable> WRITE_ONLY_SYMBOLIZER = Symbolizer.Static.initialize(StateRegister.class, Writable.class);
}
