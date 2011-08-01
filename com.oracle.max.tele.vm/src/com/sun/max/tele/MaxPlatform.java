/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.tele;


/**
 * Descriptors for various platforms on which the VM may be running.
 */
public interface MaxPlatform {

    /**
    * Instruction Set Architecture moniker.
    */
    public enum ISA {
        AMD64,
        ARM,
        IA32,
        PPC,
        SPARC;

        public static ISA fromName(String name) {
            return ISA.valueOf(name.toUpperCase());
        }
    }

    /**
     * Specific processor model.
     */
    public enum CPU {
        AMD64,
        ARM32,
        IA32,
        PPC,
        SPARC,
        SPARCV9;

        public static CPU fromName(String name) {
            return CPU.valueOf(name.toUpperCase());
        }
    }

    /**
     * Specific operating system.
     */
    public enum OS {
        DARWIN("Darwin"),
        LINUX("Linux"),
        SOLARIS("Solaris"),
        WINDOWS("Windows"),
        MAXVE("MaxVE");

        private final String name;

        private OS(String name) {
            this.name = name;
        }

        public static OS fromName(String name) {
            return OS.valueOf(name.toUpperCase());
        }

        @Override
        public final String toString() {
            return name;
        }
    }

    /**
     * @return Instruction Set Architecture information for the VM's platform
     */
    ISA getISA();

    /**
     * @return CPU information for the VM's platform
     */
    CPU getCPU();

    /**
     * @return OS information for the VM's platform
     */
    OS getOS();

    /**
     * @return size of a word in the VM.
     */
    int nBytesInWord();

    /**
     * @return size a memory page in the VM
     */
    int nBytesInPage();

}
