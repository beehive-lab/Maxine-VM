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
/*VCSID=0df2cfcb-2054-4ee7-b6fb-99b7ff90e7dc*/
package com.sun.max.vm.compiler.eir.ia32;

import com.sun.max.asm.ia32.*;
import com.sun.max.asm.x86.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class IA32EirRegister extends EirRegister {

    private static final IA32EirRegister[] _registers = new IA32EirRegister[32];

    private static Pool<IA32EirRegister> _pool = new ArrayPool<IA32EirRegister>(_registers);

    public static Pool<IA32EirRegister> pool() {
        return _pool;
    }

    private final int _ordinal;

    private static int _nextSerial;

    private final int _serial;

    protected IA32EirRegister(int ordinal) {
        _ordinal = ordinal;
        _serial = _nextSerial++;
        assert _registers[_serial] == null;
        _registers[_serial] = this;
    }

    @Override
    public final int ordinal() {
        return _ordinal;
    }

    @Override
    public final int serial() {
        return _serial;
    }


    public static final class General extends IA32EirRegister implements StaticFieldName, PoolObject {

        private General(int ordinal) {
            super(ordinal);
        }

        @Override
        public Kind kind() {
            return Kind.WORD;
        }

        public static final General EAX = new General(0);
        public static final General ECX = new General(1);
        public static final General EDX = new General(2);
        public static final General EBX = new General(3);
        public static final General ESP = new General(4);
        public static final General EBP = new General(5);
        public static final General ESI = new General(6);
        public static final General EDI = new General(7);

        private static final General[] _values = {EAX, ECX, EDX, EBX, ESP, EBP, ESI, EDI};

        public static General[] values() {
            return _values;
        }

        private static final PoolSet<IA32EirRegister> _poolSet = PoolSet.of(_pool, _values);

        public static PoolSet<IA32EirRegister> poolSet() {
            return _poolSet;
        }

        public IA32GeneralRegister8 as8() {
            return IA32GeneralRegister8.ENUMERATOR.get(ordinal());
        }

        public IA32GeneralRegister16 as16() {
            return IA32GeneralRegister16.ENUMERATOR.get(ordinal());
        }

        public IA32GeneralRegister32 as32() {
            return IA32GeneralRegister32.ENUMERATOR.get(ordinal());
        }

        public static General from(GeneralRegister register) {
            return _values[register.value()];
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.INTEGER_REGISTER;
        }

        private String _name;

        public String name() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }

        static {
            StaticFieldName.Static.initialize(General.class);
        }

        @Override
        public String toString() {
            return name();
        }

        @Override
        public TargetLocation toTargetLocation() {
            return new TargetLocation.IntegerRegister(serial());
        }
    }

    public static final class XMM extends IA32EirRegister {

        private XMM(int ordinal) {
            super(ordinal);
        }

        @Override
        public Kind kind() {
            return Kind.DOUBLE;
        }

        public static final XMM XMM0 = new XMM(0);
        public static final XMM XMM1 = new XMM(1);
        public static final XMM XMM2 = new XMM(2);
        public static final XMM XMM3 = new XMM(3);
        public static final XMM XMM4 = new XMM(4);
        public static final XMM XMM5 = new XMM(5);
        public static final XMM XMM6 = new XMM(6);
        public static final XMM XMM7 = new XMM(7);

        private static final XMM[] _values = {XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7};

        public static XMM[] values() {
            return _values;
        }

        private static final PoolSet<IA32EirRegister> _poolSet = PoolSet.of(_pool, _values);

        public static PoolSet<IA32EirRegister> poolSet() {
            return _poolSet;
        }

        public IA32XMMRegister as() {
            return IA32XMMRegister.ENUMERATOR.get(ordinal());
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.FLOATING_POINT_REGISTER;
        }

        @Override
        public String toString() {
            return as().name();
        }

        @Override
        public TargetLocation toTargetLocation() {
            return new TargetLocation.FloatingPointRegister(ordinal());
        }
    }

}
