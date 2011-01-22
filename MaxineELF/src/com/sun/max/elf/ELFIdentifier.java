/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/**
 * Copyright (c) 2005, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Creation date: Sep 2, 2005
 */

package com.sun.max.elf;

/**
 * The <code>ELFIndentifier</code> class contains all of the constants and information
 * necessary to identify the architecture of an ELF file, given its machine number.
 *
 * This information is gleaned from the ELF documentation.
 *
 * @author Ben L. Titzer
 */
public final class ELFIdentifier {

    private ELFIdentifier() {
    }

    public static final int EM_NUM = 111;
    public static final String[] EM_names = new String[EM_NUM];
    public static final String[] EM_help = new String[EM_NUM];
    public static final int EM_NONE         = nm(0, null, "No machine");
    public static final int EM_M32          = nm(1, "m32", "AT&T WE 32100");
    public static final int EM_SPARC        = nm(2, "sparc", "SPARC");
    public static final int EM_386          = nm(3, "i386", "Intel 80386");
    public static final int EM_68K          = nm(4, "m68000", "Motorola 68000");
    public static final int EM_88K          = nm(5, "m88000", "Motorola 88000");
    public static final int EM_486          = nm(6, "i486", "Intel i486");
    public static final int EM_860          = nm(7, "i860", "Intel 80860");
    public static final int EM_MIPS         = nm(8, "mips", "MIPS I Architecture");
    public static final int EM_S370         = nm(9, "s370", "IBM System/370 Processor");
    public static final int EM_MIPS_RS3_LE  = nm(10, "rs3000", "MIPS RS3000 Little-endian");
    public static final int EM_SPARC64      = nm(11, "sparc64", "SPARC 64-bit");
    public static final int EM_PARISC       = nm(15, "pa-risc", "Hewlett-Packard PA-RISC");
    public static final int EM_VPP500       = nm(17, "vpp500", "Fujitsu VPP500");
    public static final int EM_SPARC32PLUS  = nm(18, "sparc+", "Enhanced instruction set SPARC");
    public static final int EM_960          = nm(19, "i960", "Intel 80960");
    public static final int EM_PPC          = nm(20, "ppc", "PowerPC");
    public static final int EM_PPC64        = nm(21, "ppc64", "64-bit PowerPC");
    public static final int EM_S390         = nm(22, "s390", "IBM System/390 Processor");
    public static final int EM_V800         = nm(36, "v800", "NEC V800");
    public static final int EM_FR20         = nm(37, "fr20", "Fujitsu FR20");
    public static final int EM_RH32         = nm(38, "rh-32", "TRW RH-32");
    public static final int EM_RCE          = nm(39, "rce", "Motorola RCE");
    public static final int EM_ARM          = nm(40, "arm", "Advanced RISC Machines ARM");
    public static final int EM_ALPHA        = nm(41, "alpha", "Digital Alpha");
    public static final int EM_SH           = nm(42, "sh", "Hitachi SH");
    public static final int EM_SPARCV9      = nm(43, "sparcv9", "SPARC Version 9");
    public static final int EM_TRICORE      = nm(44, "tricore", "Siemens TriCore embedded processor");
    public static final int EM_ARC          = nm(45, "arc", "Argonaut RISC Core, Argonaut Technologies Inc.");
    public static final int EM_H8_300       = nm(46, "h8/300", "Hitachi H8/300");
    public static final int EM_H8_300H      = nm(47, "h8/300h", "Hitachi H8/300H");
    public static final int EM_H8S          = nm(48, "h8s", "Hitachi H8S");
    public static final int EM_H8_500       = nm(49, "h8/500", "Hitachi H8/500");
    public static final int EM_IA_64        = nm(50, "ia64", "Intel IA-64 processor architecture");
    public static final int EM_MIPS_X       = nm(51, "mips-x", "Stanford MIPS-X");
    public static final int EM_COLDFIRE     = nm(52, "coldfire", "Motorola ColdFire");
    public static final int EM_68HC12       = nm(53, "m68hc12", "Motorola M68HC12");
    public static final int EM_MMA          = nm(54, "mma", "Fujitsu MMA Multimedia Accelerator");
    public static final int EM_PCP          = nm(55, "pcp", "Siemens PCP");
    public static final int EM_NCPU         = nm(56, "ncpu", "Sony nCPU embedded RISC processor");
    public static final int EM_NDR1         = nm(57, "ndr1", "Denso NDR1 microprocessor");
    public static final int EM_STARCORE     = nm(58, "starcore", "Motorola Star*Core processor");
    public static final int EM_ME16         = nm(59, "me16", "Toyota ME16 processor");
    public static final int EM_ST100        = nm(60, "st100", "STMicroelectronics ST100 processor");
    public static final int EM_TINYJ        = nm(61, "tinyj", "Advanced Logic Corp. TinyJ embedded processor family");
    public static final int EM_X86_64       = nm(62, "x86-64", "AMD x86-64 architecture");
    public static final int EM_PDSP         = nm(63, "pdsp", "Sony DSP Processor");
    public static final int EM_FX66         = nm(66, "fx66", "Siemens FX66 microcontroller");
    public static final int EM_ST9PLUS      = nm(67, "st9+", "STMicroelectronics ST9+ 8/16 bit microcontroller");
    public static final int EM_ST7          = nm(68, "st7", "STMicroelectronics ST7 8-bit microcontroller");
    public static final int EM_68HC16       = nm(69, "m68hc16", "Motorola MC68HC16 Microcontroller");
    public static final int EM_68HC11       = nm(70, "m68hc11", "Motorola MC68HC11 Microcontroller");
    public static final int EM_68HC08       = nm(71, "m68hc08", "Motorola MC68HC08 Microcontroller");
    public static final int EM_68HC05       = nm(72, "m68hc05", "Motorola MC68HC05 Microcontroller");
    public static final int EM_SVX          = nm(73, "svx", "Silicon Graphics SVx");
    public static final int EM_ST19         = nm(74, "st19", "STMicroelectronics ST19 8-bit microcontroller");
    public static final int EM_VAX          = nm(75, "vax", "Digital VAX");
    public static final int EM_CRIS         = nm(76, "cris", "Axis Communications 32-bit embedded processor");
    public static final int EM_JAVELIN      = nm(77, "javelin", "Infineon Technologies 32-bit embedded processor");
    public static final int EM_FIREPATH     = nm(78, "firepath", "Element 14 64-bit DSP Processor");
    public static final int EM_ZSP          = nm(79, "zsp", "LSI Logic 16-bit DSP Processor");
    public static final int EM_MMIX         = nm(80, "mmix", "Donald Knuth's educational 64-bit processor");
    public static final int EM_HUANY        = nm(81, "huany", "Harvard University machine-independent object files");
    public static final int EM_PRISM        = nm(82, "prism", "SiTera Prism");
    public static final int EM_AVR          = nm(83, "avr", "Atmel AVR 8-bit microcontroller");
    public static final int EM_FR30         = nm(84, "fr30", "Fujitsu FR30");
    public static final int EM_D10V         = nm(85, "d10v", "Mitsubishi D10V");
    public static final int EM_D30V         = nm(86, "d30v", "Mitsubishi D30V");
    public static final int EM_V850         = nm(87, "v850", "NEC v850");
    public static final int EM_M32R         = nm(88, "m32r", "Mitsubishi M32R");
    public static final int EM_MN10300      = nm(89, "mn10300", "Matsushita MN10300");
    public static final int EM_MN10200      = nm(90, "mn10200", "Matsushita MN10200");
    public static final int EM_PJ           = nm(91, "pj", "picoJava");
    public static final int EM_OPENRISC     = nm(92, "openrisc", "OpenRISC 32-bit embedded processor");
    public static final int EM_ARC_A5       = nm(93, "arc-a5", "ARC Cores Tangent-A5");
    public static final int EM_XTENSA       = nm(94, "xtensa", "Tensilica Xtensa Architecture");
    public static final int EM_VIDEOCORE    = nm(95, "videocore", "Alphamosaic VideoCore processor");
    public static final int EM_TMM_GPP      = nm(96, "tmm-gpp", "Thompson Multimedia General Purpose Processor");
    public static final int EM_NS32K        = nm(97, "ns32000", "National Semiconductor 32000 series");
    public static final int EM_TPC          = nm(98, "tpc", "Tenor Network TPC processor");
    public static final int EM_SNP1K        = nm(99, "snp1000", "Trebia SNP 1000 processor");
    public static final int EM_ST200        = nm(100, "st200", "STMicroelectronics (www.st.com) ST200 microcontroller");
    public static final int EM_IP2K         = nm(101, "ip2000", "Ubicom IP2xxx microcontroller family");
    public static final int EM_MAX          = nm(102, "max", "MAX Processor");
    public static final int EM_CR           = nm(103, "cr", "National Semiconductor CompactRISC microprocessor");
    public static final int EM_F2MC16       = nm(104, "f2mc16", "Fujitsu F2MC16");
    public static final int EM_MSP430       = nm(105, "msp430", "Texas Instruments embedded microcontroller msp430");
    public static final int EM_BLACKFIN     = nm(106, "blackfin", "Analog Devices Blackfin (DSP) processor");
    public static final int EM_SE_C33       = nm(107, "s1c33", "S1C33 Family of Seiko Epson processors");
    public static final int EM_SEP          = nm(108, "sep", "Sharp embedded microprocessor");
    public static final int EM_ARCA         = nm(109, "arca", "Arca RISC Microprocessor");
    public static final int EM_UNICORE      = nm(110, "unicore", "Microprocessor series from PKU-Unity Ltd. and MPRC of Peking University");

    static int nm(int id, String name, String help) {
        EM_names[id] = name;
        EM_help[id] = help;
        return id;
    }

    /**
     * The <code>getArchitecture()</code> method attempts to get a string representation of the given
     * machine number. If the number corresponds to a known machine number, this method will return
     * a short string representing the canonical handle for that architecture.
     * @param mach the machine number from an ELF identification section
     * @return a string name representing the architecture of the ELF file if the machine number is
     * recognized; null otherwise
     */
    public static String getArchitecture(int mach) {
        if (mach < EM_names.length) {
            return EM_names[mach];
        }
        return null;
    }

    /**
     * The <code>getDescription()</code> method attempts to get a string representation that contains
     * more information about the specified machine. If the machine is valid, then this method
     * returns a string that contains a longer description string of the specified machine.
     * @param mach the machine number for which to get a description
     * @return a string containing a longer description of this machine, if it exists; null otherwise
     */
    public static String getDescription(int mach) {
        if (mach < EM_help.length) {
            return EM_help[mach];
        }
        return null;
    }
}
