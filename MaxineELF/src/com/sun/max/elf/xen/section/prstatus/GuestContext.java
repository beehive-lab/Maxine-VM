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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import com.sun.max.elf.ELFDataInputStream;
import com.sun.max.elf.ELFHeader;
import com.sun.max.elf.ELFSectionHeaderTable;
import com.sun.max.elf.xen.ImproperDumpFileException;

/**
 * The CPU context dumped by xen
 *
 * @author Puneeet Lakhina
 *
 */
public class GuestContext {

    /**
     * This only includes XMM0 - XMM15
     */
    private byte[] fpuRegisters = new byte[128];
    private long flags;
   private X86_64Registers cpuUserRegs = null;
    private TrapInfo[] trapInfo = new TrapInfo[256];
    private long linearAddressBase, linearAddressEntries;
    private long[] gdtFrames = new long[16];
    private long gdtEntries;
    private long kernelSS;
    private long kernelSP;
    private long[] ctrlreg = new long[8];
    private long[] debugreg = new long[8];
    private long eventCallBackEip;
    private long failsafeCallbackEip;
    private long syscallCallbackEip;
    private long vmAssist;
    private long fsBase;
    private long gsBaseKernel;
    private long gsBaseUser;
    private RandomAccessFile dumpraf;
    private ELFHeader header;
    private ELFSectionHeaderTable.Entry sectionHeader;
    private ByteBuffer sectionDataBuffer;
    private int cpuid;
    private ELFDataInputStream byteBufferInputStream;

    public GuestContext(RandomAccessFile dumpraf, ELFHeader header, ELFSectionHeaderTable.Entry sectionHeader,int cpuid) {
        this.dumpraf = dumpraf;
        this.header = header;
        this.sectionHeader = sectionHeader;
        this.cpuid = cpuid;
    }

    public void read() throws IOException, ImproperDumpFileException {
        dumpraf.seek(sectionHeader.getOffset()+cpuid * 5168);
        byte[] sectionData = new byte[5168];
        dumpraf.read(sectionData);
        sectionDataBuffer = ByteBuffer.wrap(sectionData);
        //read fpu registers
        readfpu();
        //read flags
        byteBufferInputStream = new ELFDataInputStream(header,sectionDataBuffer);
        flags = byteBufferInputStream.read_Elf64_XWord();
        //Read registers
        byte[] registerData = new byte[X86_64Registers.TOTAL_SIZE];
        sectionDataBuffer.get(registerData);
        cpuUserRegs = new X86_64Registers(registerData , header.isBigEndian());
        //Skip trap info ldt gdt kernel ss
        sectionDataBuffer.position(sectionDataBuffer.position() + 4264);
        readctrlregs();
    }

    private void readctrlregs()throws IOException {
        for (int i = 0; i < ctrlreg.length; i++) {
            // for each register read 8 bytes
            ctrlreg[i] = byteBufferInputStream.read_Elf64_XWord();
        }
    }
    private void readfpu() {
        sectionDataBuffer.position(20 * 8);
        // Skip registers we dont want
        for (int i = 0; i < 15; i++) {
            // for each register read 8 bytes
            byte[] data = new byte[8];
            sectionDataBuffer.get(data);
            for (int j = 0; j < 8; j++) {
                fpuRegisters[i*8+j] = data[header.isBigEndian()? j:7 - j];
            }
            sectionDataBuffer.position(sectionDataBuffer.position()+8);
        }
        sectionDataBuffer.position(512);
    }


    /**
     * @return the fpuRegisters
     */
    public byte[] getfpuRegisters() {
        return fpuRegisters;
    }

    /**
     * @return the cpuUserRegs
     */
    public X86_64Registers getCpuUserRegs() {
        return cpuUserRegs;
    }


    /**
     * @return the ctrlreg
     */
    public long[] getCtrlreg() {
        return ctrlreg;
    }


}
