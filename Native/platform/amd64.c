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
/*
 * @author Bernd Mathiske
 */
#include "amd64.h"
#include "log.h"

void amd64_decanonicalizeSignalIntegerRegisters(amd64_CanonicalIntegerRegisters c, amd64_OsSignalIntegerRegisters os) {
#if os_SOLARIS || os_LINUX
    os[REG_RAX] = (greg_t) c->rax;
    os[REG_RCX] = (greg_t) c->rcx;
    os[REG_RDX] = (greg_t) c->rdx;
    os[REG_RBX] = (greg_t) c->rbx;
    os[REG_RSP] = (greg_t) c->rsp;
    os[REG_RBP] = (greg_t) c->rbp;
    os[REG_RSI] = (greg_t) c->rsi;
    os[REG_RDI] = (greg_t) c->rdi;
    os[REG_R8] = (greg_t) c->r8;
    os[REG_R9] = (greg_t) c->r9;
    os[REG_R10] = (greg_t) c->r10;
    os[REG_R11] = (greg_t) c->r11;
    os[REG_R12] = (greg_t) c->r12;
    os[REG_R13] = (greg_t) c->r13;
    os[REG_R14] = (greg_t) c->r14;
    os[REG_R15] = (greg_t) c->r15;
#elif os_GUESTVMXEN
    os->rax = (unsigned long) c->rax;
    os->rcx = (unsigned long) c->rcx;
    os->rdx = (unsigned long) c->rdx;
    os->rbx = (unsigned long) c->rbx;
    os->rsp = (unsigned long) c->rsp;
    os->rbp = (unsigned long) c->rbp;
    os->rsi = (unsigned long) c->rsi;
    os->rdi = (unsigned long) c->rdi;
    os->r8 = (unsigned long) c->r8;
    os->r9 = (unsigned long) c->r9;
    os->r10 = (unsigned long) c->r10;
    os->r11 = (unsigned long) c->r11;
    os->r12 = (unsigned long) c->r12;
    os->r13 = (unsigned long) c->r13;
    os->r14 = (unsigned long) c->r14;
    os->r15 = (unsigned long) c->r15;
#elif os_DARWIN
    os->__rax = (Address) c->rax;
    os->__rcx = (Address) c->rcx;
    os->__rdx = (Address) c->rdx;
    os->__rbx = (Address) c->rbx;
    os->__rsp = (Address) c->rsp;
    os->__rbp = (Address) c->rbp;
    os->__rsi = (Address) c->rsi;
    os->__rdi = (Address) c->rdi;
    os->__r8 = (Address) c->r8;
    os->__r9 = (Address) c->r9;
    os->__r10 = (Address) c->r10;
    os->__r11 = (Address) c->r11;
    os->__r12 = (Address) c->r12;
    os->__r13 = (Address) c->r13;
    os->__r14 = (Address) c->r14;
    os->__r15 = (Address) c->r15;
#else
    c_UNIMPLEMENTED();
#endif
}

void amd64_canonicalizeSignalIntegerRegisters(amd64_OsSignalIntegerRegisters os, amd64_CanonicalIntegerRegisters c) {
#if os_LINUX || os_SOLARIS
    c->rax = (Word) os[REG_RAX];
    c->rcx = (Word) os[REG_RCX];
    c->rdx = (Word) os[REG_RDX];
    c->rbx = (Word) os[REG_RBX];
    c->rsp = (Word) os[REG_RSP];
    c->rbp = (Word) os[REG_RBP];
    c->rsi = (Word) os[REG_RSI];
    c->rdi = (Word) os[REG_RDI];
    c->r8 =  (Word) os[REG_R8];
    c->r9 =  (Word) os[REG_R9];
    c->r10 = (Word) os[REG_R10];
    c->r11 = (Word) os[REG_R11];
    c->r12 = (Word) os[REG_R12];
    c->r13 = (Word) os[REG_R13];
    c->r14 = (Word) os[REG_R14];
    c->r15 = (Word) os[REG_R15];
#elif os_GUESTVMXEN
    c->rax = (Word) os->rax;
    c->rcx = (Word) os->rcx;
    c->rdx = (Word) os->rdx;
    c->rbx = (Word) os->rbx;
    c->rsp = (Word) os->rsp;
    c->rbp = (Word) os->rbp;
    c->rsi = (Word) os->rsi;
    c->rdi = (Word) os->rdi;
    c->r8 = (Word) os->r8;
    c->r9 = (Word) os->r9;
    c->r10 = (Word) os->r10;
    c->r11 = (Word) os->r11;
    c->r12 = (Word) os->r12;
    c->r13 = (Word) os->r13;
    c->r14 = (Word) os->r14;
    c->r15 = (Word) os->r15;
#elif os_DARWIN
    c->rax = (Word) os->__rax;
    c->rcx = (Word) os->__rcx;
    c->rdx = (Word) os->__rdx;
    c->rbx = (Word) os->__rbx;
    c->rsp = (Word) os->__rsp;
    c->rbp = (Word) os->__rbp;
    c->rsi = (Word) os->__rsi;
    c->rdi = (Word) os->__rdi;
    c->r8 = (Word) os->__r8;
    c->r9 = (Word) os->__r9;
    c->r10 = (Word) os->__r10;
    c->r11 = (Word) os->__r11;
    c->r12 = (Word) os->__r12;
    c->r13 = (Word) os->__r13;
    c->r14 = (Word) os->__r14;
    c->r15 = (Word) os->__r15;
#else
    c_UNIMPLEMENTED();
#endif
}

void amd64_printCanonicalIntegerRegisters(amd64_CanonicalIntegerRegisters c) {
#   define PRINT_REG(name, field) log_println(name " = 0x%016lx [%ld]", c->field, c->field)
    PRINT_REG("RAX", rax);
    PRINT_REG("RCX", rcx);
    PRINT_REG("RDX", rdx);
    PRINT_REG("RBX", rbx);
    PRINT_REG("RSP", rsp);
    PRINT_REG("RDI", rdi);
    PRINT_REG("R8 ", r8);
    PRINT_REG("R9 ", r9);
    PRINT_REG("R10", r10);
    PRINT_REG("R11", r11);
    PRINT_REG("R12", r12);
    PRINT_REG("R13", r13);
    PRINT_REG("R14", r14);
    PRINT_REG("R15", r15);
}

void amd64_canonicalizeTeleIntegerRegisters(amd64_OsTeleIntegerRegisters os, amd64_CanonicalIntegerRegisters c) {
#if os_DARWIN
    c->rax = (Word) os->__rax;
    c->rcx = (Word) os->__rcx;
    c->rdx = (Word) os->__rdx;
    c->rbx = (Word) os->__rbx;
    c->rsp = (Word) os->__rsp;
    c->rbp = (Word) os->__rbp;
    c->rsi = (Word) os->__rsi;
    c->rdi = (Word) os->__rdi;
    c->r8 = (Word) os->__r8;
    c->r9 = (Word) os->__r9;
    c->r10 = (Word) os->__r10;
    c->r11 = (Word) os->__r11;
    c->r12 = (Word) os->__r12;
    c->r13 = (Word) os->__r13;
    c->r14 = (Word) os->__r14;
    c->r15 = (Word) os->__r15;
#elif os_LINUX || os_GUESTVMXEN
    c->rax = (Word) os->rax;
    c->rcx = (Word) os->rcx;
    c->rdx = (Word) os->rdx;
    c->rbx = (Word) os->rbx;
    c->rsp = (Word) os->rsp;
    c->rbp = (Word) os->rbp;
    c->rsi = (Word) os->rsi;
    c->rdi = (Word) os->rdi;
    c->r8 = (Word) os->r8;
    c->r9 = (Word) os->r9;
    c->r10 = (Word) os->r10;
    c->r11 = (Word) os->r11;
    c->r12 = (Word) os->r12;
    c->r13 = (Word) os->r13;
    c->r14 = (Word) os->r14;
    c->r15 = (Word) os->r15;
#elif os_SOLARIS
    c->rax = os[REG_RAX];
    c->rcx = os[REG_RCX];
    c->rdx = os[REG_RDX];
    c->rbx = os[REG_RBX];
    c->rsp = os[REG_RSP];
    c->rbp = os[REG_RBP];
    c->rsi = os[REG_RSI];
    c->rdi = os[REG_RDI];
    c->r8 = os[REG_R8];
    c->r9 = os[REG_R9];
    c->r10 = os[REG_R10];
    c->r11 = os[REG_R11];
    c->r12 = os[REG_R12];
    c->r13 = os[REG_R13];
    c->r14 = os[REG_R14];
    c->r15 = os[REG_R15];
#else
    c_UNIMPLEMENTED();
#endif
}

#if os_DARWIN
#   define GET_DARWIN_FP_REGISTER(n) (*((Word *) (&os->__fpu_xmm##n)))
#elif os_LINUX
    static Word getLinuxSignalFpRegister(amd64_OsSignalFloatingPointRegisters os, int index) {
        Word *osRegister = (Word *) &os->_xmm[index];
        /* we are only interested in the lower 64-bits: */
        return *osRegister;
    }
#elif os_SOLARIS
	static Word getSolarisFpRegister(amd64_OsTeleFloatingPointRegisters os, int index) {
		Word *osRegister = (Word *) &os->fp_reg_set.fpchip_state.xmm[index];
		/* we are only interested in the lower 64-bits: */
		return *osRegister;
	}
#endif



void amd64_canonicalizeTeleFloatingPointRegisters(amd64_OsTeleFloatingPointRegisters os, amd64_CanonicalFloatingPointRegisters c) {
#if os_DARWIN
    c->xmm0 = GET_DARWIN_FP_REGISTER(0);
    c->xmm1 = GET_DARWIN_FP_REGISTER(1);
    c->xmm2 = GET_DARWIN_FP_REGISTER(2);
    c->xmm3 = GET_DARWIN_FP_REGISTER(3);
    c->xmm4 = GET_DARWIN_FP_REGISTER(4);
    c->xmm5 = GET_DARWIN_FP_REGISTER(5);
    c->xmm6 = GET_DARWIN_FP_REGISTER(6);
    c->xmm7 = GET_DARWIN_FP_REGISTER(7);
    c->xmm8 = GET_DARWIN_FP_REGISTER(8);
    c->xmm9 = GET_DARWIN_FP_REGISTER(9);
    c->xmm10 = GET_DARWIN_FP_REGISTER(10);
    c->xmm11 = GET_DARWIN_FP_REGISTER(11);
    c->xmm12 = GET_DARWIN_FP_REGISTER(12);
    c->xmm13 = GET_DARWIN_FP_REGISTER(13);
    c->xmm14 = GET_DARWIN_FP_REGISTER(14);
    c->xmm15 = GET_DARWIN_FP_REGISTER(15);
#elif os_LINUX
    // TODO
#elif os_SOLARIS
    c->xmm0 = getSolarisFpRegister(os, 0);
    c->xmm1 = getSolarisFpRegister(os, 1);
    c->xmm2 = getSolarisFpRegister(os, 2);
    c->xmm3 = getSolarisFpRegister(os, 3);
    c->xmm4 = getSolarisFpRegister(os, 4);
    c->xmm5 = getSolarisFpRegister(os, 5);
    c->xmm6 = getSolarisFpRegister(os, 6);
    c->xmm7 = getSolarisFpRegister(os, 7);
    c->xmm8 = getSolarisFpRegister(os, 8);
    c->xmm9 = getSolarisFpRegister(os, 9);
    c->xmm10 = getSolarisFpRegister(os, 10);
    c->xmm11 = getSolarisFpRegister(os, 11);
    c->xmm12 = getSolarisFpRegister(os, 12);
    c->xmm13 = getSolarisFpRegister(os, 13);
    c->xmm14 = getSolarisFpRegister(os, 14);
    c->xmm15 = getSolarisFpRegister(os, 15);
#elif os_GUESTVMXEN
    // TODO
#else
    c_UNIMPLEMENTED();
#endif
}

void amd64_canonicalizeSignalFloatingPointRegisters(amd64_OsSignalFloatingPointRegisters os, amd64_CanonicalFloatingPointRegisters c) {
#if os_DARWIN
#   define GET_XMM(n) (*((Word *) (&os->__fpu_xmm##n)))
    c->xmm0 = GET_XMM(0);
    c->xmm1 = GET_XMM(1);
    c->xmm2 = GET_XMM(2);
    c->xmm3 = GET_XMM(3);
    c->xmm4 = GET_XMM(4);
    c->xmm5 = GET_XMM(5);
    c->xmm6 = GET_XMM(6);
    c->xmm7 = GET_XMM(7);
    c->xmm8 = GET_XMM(8);
    c->xmm9 = GET_XMM(9);
    c->xmm10 = GET_XMM(10);
    c->xmm11 = GET_XMM(11);
    c->xmm12 = GET_XMM(12);
    c->xmm13 = GET_XMM(13);
    c->xmm14 = GET_XMM(14);
    c->xmm15 = GET_XMM(15);
#elif os_LINUX
    c->xmm0 = getLinuxSignalFpRegister(os, 0);
    c->xmm1 = getLinuxSignalFpRegister(os, 1);
    c->xmm2 = getLinuxSignalFpRegister(os, 2);
    c->xmm3 = getLinuxSignalFpRegister(os, 3);
    c->xmm4 = getLinuxSignalFpRegister(os, 4);
    c->xmm5 = getLinuxSignalFpRegister(os, 5);
    c->xmm6 = getLinuxSignalFpRegister(os, 6);
    c->xmm7 = getLinuxSignalFpRegister(os, 7);
    c->xmm8 = getLinuxSignalFpRegister(os, 8);
    c->xmm9 = getLinuxSignalFpRegister(os, 9);
    c->xmm10 = getLinuxSignalFpRegister(os, 10);
    c->xmm11 = getLinuxSignalFpRegister(os, 11);
    c->xmm12 = getLinuxSignalFpRegister(os, 12);
    c->xmm13 = getLinuxSignalFpRegister(os, 13);
    c->xmm14 = getLinuxSignalFpRegister(os, 14);
    c->xmm15 = getLinuxSignalFpRegister(os, 15);
#elif os_SOLARIS
    c->xmm0 = getSolarisFpRegister(os, 0);
    c->xmm1 = getSolarisFpRegister(os, 1);
    c->xmm2 = getSolarisFpRegister(os, 2);
    c->xmm3 = getSolarisFpRegister(os, 3);
    c->xmm4 = getSolarisFpRegister(os, 4);
    c->xmm5 = getSolarisFpRegister(os, 5);
    c->xmm6 = getSolarisFpRegister(os, 6);
    c->xmm7 = getSolarisFpRegister(os, 7);
    c->xmm8 = getSolarisFpRegister(os, 8);
    c->xmm9 = getSolarisFpRegister(os, 9);
    c->xmm10 = getSolarisFpRegister(os, 10);
    c->xmm11 = getSolarisFpRegister(os, 11);
    c->xmm12 = getSolarisFpRegister(os, 12);
    c->xmm13 = getSolarisFpRegister(os, 13);
    c->xmm14 = getSolarisFpRegister(os, 14);
    c->xmm15 = getSolarisFpRegister(os, 15);
#elif os_GUESTVMXEN
    // TODO
#elif os_DARWIN
    c->xmm0 = GET_DARWIN_FP_REGISTER(0);
    c->xmm1 = GET_DARWIN_FP_REGISTER(1);
    c->xmm2 = GET_DARWIN_FP_REGISTER(2);
    c->xmm3 = GET_DARWIN_FP_REGISTER(3);
    c->xmm4 = GET_DARWIN_FP_REGISTER(4);
    c->xmm5 = GET_DARWIN_FP_REGISTER(5);
    c->xmm6 = GET_DARWIN_FP_REGISTER(6);
    c->xmm7 = GET_DARWIN_FP_REGISTER(7);
    c->xmm8 = GET_DARWIN_FP_REGISTER(8);
    c->xmm9 = GET_DARWIN_FP_REGISTER(9);
    c->xmm10 = GET_DARWIN_FP_REGISTER(10);
    c->xmm11 = GET_DARWIN_FP_REGISTER(11);
    c->xmm12 = GET_DARWIN_FP_REGISTER(12);
    c->xmm13 = GET_DARWIN_FP_REGISTER(13);
    c->xmm14 = GET_DARWIN_FP_REGISTER(14);
    c->xmm15 = GET_DARWIN_FP_REGISTER(15);
#else
    c_UNIMPLEMENTED();
#endif
}

void amd64_canonicalizeTeleStateRegisters(amd64_OsTeleStateRegisters os, amd64_CanonicalStateRegisters c) {
#if os_DARWIN
    c->rip = (Word) os->__rip;
    c->flags = (Word) os->__rflags;
#elif os_LINUX
    // TODO
#elif os_SOLARIS
    c->rip = os[REG_RIP];
    c->flags = os[REG_RFL];
#elif os_GUESTVMXEN
    c->rip = (Word) os->rip;
    c->flags = (Word) 0xdeadbeef; // TODO
#else
    c_UNIMPLEMENTED();
#endif
}

void amd64_printCanonicalFloatingPointRegisters(amd64_CanonicalFloatingPointRegisters c) {
#   define PRINT_XMM(id) log_println("XMM%-2d = 0x%016lx [%lf]", id, CONCATENATE(c->xmm, id), CONCATENATE(c->xmm, id))
    PRINT_XMM(0);
    PRINT_XMM(1);
    PRINT_XMM(2);
    PRINT_XMM(3);
    PRINT_XMM(4);
    PRINT_XMM(5);
    PRINT_XMM(6);
    PRINT_XMM(7);
    PRINT_XMM(8);
    PRINT_XMM(9);
    PRINT_XMM(10);
    PRINT_XMM(11);
    PRINT_XMM(12);
    PRINT_XMM(13);
    PRINT_XMM(14);
    PRINT_XMM(15);
}

