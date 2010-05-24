/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A. All rights
 * reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 *
 * Parts of the product may be derived from Berkeley BSD systems, licensed from the University of California. UNIX is a
 * registered trademark in the U.S. and in other countries, exclusively licensed through X/Open Company, Ltd.
 *
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered trademarks of Sun Microsystems, Inc. in the
 * U.S. and other countries.
 *
 * This product is covered and controlled by U.S. Export Control laws and may be subject to the export or import laws in
 * other countries. Nuclear, missile, chemical biological weapons or nuclear maritime end uses or end users, whether
 * direct or indirect, are strictly prohibited. Export or reexport to countries subject to U.S. embargo or to entities
 * identified on U.S. export exclusion lists, including, but not limited to, the denied persons and specially designated
 * nationals lists is strictly prohibited.
 */
package com.sun.max.elf.xen.section.prstatus;

/**
 * @author Puneeet Lakhina
 *
 */
public class X86_64Registers {

    public static final int TOTAL_SIZE = 200;
// byte[] teleCanonicalizedRegisterData;
    byte[] originalRegisterData;
    boolean bigEndian;
    public static enum Register {
        //The canonical index is based on amd64.c . The original Index is based on cpu_user_regs in xen-x86_64.c in the xen distribution
        R15(0, 8, 120), R14(8, 8, 112), R13(16, 8, 104), R12(24, 8, 96), RBP(8, 8, 40), RBX(32, 8, 24), R11(40, 8, 88), R10(48, 8, 80), R9(56, 8, 72), R8(64, 8, 64), RAX(72, 8, 0), RCX(80, 8, 8), RDX(
                        88, 8, 16), RSI(96, 8, 48), RDI(104, 8, 56), RFLAGS(128, 8, -1), RSP(136, 8, 32);

        private int originalIndex;
        private int length;
        private int canonicalIndex;

        Register(int index, int length, int canonicalIndex) {
            this.originalIndex = index;
            this.length = length;
            this.canonicalIndex = canonicalIndex;
        }

        public int getOriginalIndex() {
            return this.originalIndex;
        }

        public int getLength() {
            return this.length;
        }

        public int getCanonicalIndex() {
            return this.canonicalIndex;
        }
    }

    public X86_64Registers(byte[] originalData,boolean bigEndian) {
        this.originalRegisterData = originalData;
        this.bigEndian = bigEndian;
    }

    public byte[] canonicalize() {
        byte[] canonicalArr = new byte[128];
        for(Register register:Register.values()) {
            if(register.getCanonicalIndex() == -1) {
                continue;
            }
            if(bigEndian) {
                for(int i=register.getOriginalIndex(),j=register.getCanonicalIndex(),ctr=0;ctr < register.getLength();i++,j++,ctr++) {
                    canonicalArr[j] = originalRegisterData[i];
                }
            }else {
                for(int i=register.getOriginalIndex()+register.getLength()-1,j=register.getCanonicalIndex(),ctr=0;ctr < register.getLength();i--,j++,ctr++) {
                    canonicalArr[j] = originalRegisterData[i];
                }
            }
        }
        return canonicalArr;
    }




//    private long r15;
//    private long r14;
//    private long r13;
//    private long r12;
//    private long rbp;
//    private long rbx;
//    private long r11;
//    private long r10;
//    private long r19;
//    private long r8;
//    private long rax;
//    private long rcx;
//    private long rdx;
//    private long rsi;
//    private long rdi;
//    private long errorCode;// this is unit32
//    private int entryVector;// this is unit32
//    private long rip;
//    private int cs;
//    private int pad0;
//    private short savedUpCallMask;
//    private short[] pad1 = new short[3];
//    private long rflags;
//    private long rsp;
//    // all following are uint16
//    private int ss;
//    private int[] pad2 = new int[3];
//    private int es;
//    private int[] pad3 = new int[3];
//    private int ds;
//    private int[] pad4 = new int[3];
//    private int fs;
//    private int[] pad5 = new int[3];
//    private int gs;
//    private int[] pad6 = new int[3];


}
