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
 * @author Bernd Mathiske
 * @author Laurent Daynes
 * @author Doug Simon
 */

#include "c.h"
#include "threads.h"
#include "virtualMemory.h"
#include "log.h"
#include "jni.h"
#include "os.h"
#include "isa.h"
#include "image.h"
#include "trap.h"

#if os_SOLARIS && isa_SPARC
    /* Get STACK_BIAS definition for Solaris / SPARC */
#      include <sys/stack.h>
       typedef struct rwindow * RegisterWindow;
#endif

/*
 * Important: The values defined here must correspond to those of the same name
 *            defined in the TrapNumber class in Trap.java.
 */
#define MEMORY_FAULT 0
#define STACK_FAULT 1
#define STACK_FATAL 2
#define ILLEGAL_INSTRUCTION 3
#define ARITHMETIC_EXCEPTION 4
#define ASYNC_INTERRUPT 5

static Address theJavaTrapStub;
static boolean traceTraps = false;

#if !os_MAXVE

/**
 * All signals.
 */
static sigset_t allSignals;

/**
 * The signals directly handled by the VM.
 */
static sigset_t vmSignals;

/**
 * The signals directly handled by the VM as well as those
 * that can be dispatched by SignalDispatcher.java.
 */
static sigset_t vmAndDefaultSignals;

#endif

int getTrapNumber(int signal) {
    switch (signal) {
    case SIGSEGV:
#if !os_MAXVE
    case SIGBUS:
#endif
        return MEMORY_FAULT;
    case SIGILL:
        return ILLEGAL_INSTRUCTION;
    case SIGFPE:
        return ARITHMETIC_EXCEPTION;
#if !os_MAXVE
    case SIGUSR1:
        return ASYNC_INTERRUPT;
#endif
    }
    return -signal;
}

#if os_SOLARIS
#include <thread.h>
#define thread_setSignalMask thr_sigsetmask
#elif os_DARWIN || os_LINUX
#define thread_setSignalMask pthread_sigmask
#endif

void setCurrentThreadSignalMask(boolean isVmOperationThread) {
#if !os_MAXVE
    if (isVmOperationThread) {
        thread_setSignalMask(SIG_SETMASK, &vmAndDefaultSignals, NULL);
    } else {
        thread_setSignalMask(SIG_BLOCK, &allSignals, NULL);
        thread_setSignalMask(SIG_UNBLOCK, &vmSignals, NULL);
    }
#endif
}

void* setSignalHandler(int signal, SignalHandlerFunction handler) {
#if os_MAXVE
	maxve_register_fault_handler(signal, handler);
	return NULL;
#else

    struct sigaction newSigaction;
    struct sigaction oldSigaction;

    memset((char *) &newSigaction, 0, sizeof(newSigaction));
    sigemptyset(&newSigaction.sa_mask);
    newSigaction.sa_flags = SA_SIGINFO | SA_RESTART | SA_ONSTACK;
#if os_SOLARIS || os_LINUX || os_DARWIN
    if (signal == SIGUSR1) {
        newSigaction.sa_flags = SA_SIGINFO | SA_ONSTACK;
    }
#endif
    newSigaction.sa_sigaction = handler;

    if (sigaction(signal, &newSigaction, &oldSigaction) != 0) {
        log_exit(1, "sigaction failed");
    }

    if (traceTraps || log_TRAP) {
        log_lock();
        log_print("Registered handler %p [", handler);
        log_print_symbol((Address) handler);
        log_print("] for signal %d", signal);
        if (oldSigaction.sa_handler != NULL) {
            log_print(" replacing handler ");
            log_print_symbol((Address) oldSigaction.sa_handler);
        }
        log_print_newline();
        log_unlock();
    }
    return (void *) oldSigaction.sa_handler;
#endif

}

static Address getInstructionPointer(UContext *ucontext) {
#if os_SOLARIS
    return ucontext->uc_mcontext.gregs[REG_PC];
#elif os_LINUX
#   if isa_AMD64
    return ucontext->uc_mcontext.gregs[REG_RIP];
#   elif isa_IA32
    return ucontext->uc_mcontext.gregs[REG_EIP];
#   endif
#elif os_DARWIN
    return ucontext->uc_mcontext->__ss.__rip;
#elif os_MAXVE
        return ucontext->rip;
#else
        c_UNIMPLEMENTED();
#endif
}

static void setInstructionPointer(UContext *ucontext, Address stub) {
#if os_SOLARIS
#   if isa_SPARC
    ucontext->uc_mcontext.gregs[REG_nPC] = (greg_t) (stub + 4);
#   endif
    ucontext->uc_mcontext.gregs[REG_PC] = (greg_t) stub;
#elif os_DARWIN
    ucontext->uc_mcontext->__ss.__rip = stub;
#elif os_LINUX
#   if isa_AMD64
     ucontext->uc_mcontext.gregs[REG_RIP] = (greg_t) stub;
#   elif isa_IA32
     ucontext->uc_mcontext.gregs[REG_EIP] = (greg_t) stub;
#   endif
#elif os_MAXVE
    ucontext->rip = (unsigned long) stub;
#else
    c_UNIMPLEMENTED();
#endif
}

static Address getFaultAddress(SigInfo * sigInfo, UContext *ucontext) {
#if (os_DARWIN || os_SOLARIS || os_LINUX )
    return (Address) sigInfo->si_addr;
#elif (os_MAXVE)
    return (Address) sigInfo;
#endif
}

/**
 * Gets the name of a given signal if it is a signal handled
 * directly by the VM otherwise return NULL.
 */
char *vmSignalName(int signal) {
    switch (signal) {
    case SIGSEGV: return "SIGSEGV";
    case SIGFPE: return "SIGFPE";
    case SIGILL: return "SIGILL";
#if !os_MAXVE
    case SIGUSR1: return "SIGUSR1";
    case SIGBUS: return "SIGBUS";
#endif
    }
    return NULL;
}

static void blueZoneTrap(NativeThreadLocals ntl) {
#if os_MAXVE
	maxve_blue_zone_trap(ntl);
#endif
}

#if isa_AMD64
/**
 * According to the JVM specification for the IDIV and LDIV instructions:
 *
 *    There is one special case that does not satisfy this rule: if the dividend is the
 *    negative integer of largest possible magnitude for the int/long type, and the divisor
 *    is -1, then overflow occurs, and the result is equal to the dividend. Despite
 *    the overflow, no exception is thrown in this case.
 *
 * On AMD64 (and x86), the CPU traps in this case. This function determines
 * if the trap is due to Integer.MIN_VALUE / -1 and if so, sets the result to
 * the dividend and advances the instruction pointer in the given trap context
 * to the next instruction.
 */
static boolean handleDivideOverflow(UContext *ucontext) {
    unsigned char *rip = (unsigned char *) getInstructionPointer(ucontext);

    boolean is64Bit = false;

    if ((rip[0] & 0xf0) == 0x40) {
        /* Decode REX byte */
        unsigned char rex = rip[0] & 0x0f;
        is64Bit = (rex & 0x08) != 0;
        rip++;
    }

    if (rip[0] == 0xf7) {
#if os_SOLARIS
        Address dividend = ucontext->uc_mcontext.gregs[REG_RAX];
#elif os_LINUX
        Address dividend = ucontext->uc_mcontext.gregs[REG_RAX];
#elif os_DARWIN
        Address dividend = ucontext->uc_mcontext->__ss.__rax;
#elif os_MAXVE
        Address dividend = ucontext->rax;
#else
        c_UNIMPLEMENTED();
#endif
        boolean isDividendMinValue = false;
        unsigned char modrm = rip[1];

        if (((modrm >> 3) & 7) == 7) {
            if (is64Bit) {
                isDividendMinValue = (dividend == 0x8000000000000000L);
            } else {
                isDividendMinValue = ((dividend & 0xffffffff) == 0x80000000);
            }
        }

        if (isDividendMinValue) {
            unsigned char rm = modrm & 7;
            /* Set the remainder to 0. */
#if os_SOLARIS
            ucontext->uc_mcontext.gregs[REG_RDX] = 0;
#elif os_LINUX
            ucontext->uc_mcontext.gregs[REG_RDX] = 0;
#elif os_DARWIN
            ucontext->uc_mcontext->__ss.__rdx = 0;
#elif os_MAXVE
            ucontext->rdx = 0;
#else
            c_UNIMPLEMENTED();
#endif
            switch (modrm >> 6) {
                case 0:  /* register indirect */
                    if (rm == 5) {  /* 32-bit displacement */
                        rip += 4;
                    }
                    if (rm == 4) { /* A SIB byte follows the ModR/M byte */
                        rip += 1;
                    }
                    break;
                case 1:  /* register indirect + 8-bit displacement */
                    rip += 1;
                    if (rm == 4) { /* A SIB byte follows the ModR/M byte */
                        rip += 1;
                    }
                    break;
                case 2:  /* register indirect + 32-bit displacement */
                    rip += 4;
                    if (rm == 4) { /* A SIB byte follows the ModR/M byte */
                        rip += 1;
                    }
                    break;
                case 3:
                    break;
            }
            rip += 2;
            setInstructionPointer(ucontext, (Address) rip);
            return true;
        }
    }
    return false;
}
#endif

static void logTrap(int signal, Address ip, Address fault, TLA dtla) {
    char *sigName = vmSignalName(signal);
    if (sigName == NULL) {
        sigName = "<unknown>";
    }
    log_lock();
    log_println("SIGNAL: %0d [%s]", signal, sigName);
    log_println("  Instruction Pointer = %p", ip);
    log_println("  Fault address       = %p", fault);
    log_println("  Trap number         = %d", getTrapNumber(signal));
    log_println("  Thread handle       = %p", thread_self());
    if (dtla != 0) {
        log_println("  Thread ID           = %d", tla_load(int, dtla, ID));
        log_println("  Safepoint latch     = %p", tla_load(Address, dtla, TRAP_LATCH_REGISTER));
    }
    log_unlock();
}

/**
 * The handler for signals dealt with by Stubs.trapStub.
 */
static void vmSignalHandler(int signal, SigInfo *signalInfo, UContext *ucontext) {
    int primordial = 0;
    int trapNumber = getTrapNumber(signal);
    Address ip = getInstructionPointer(ucontext);
    Address faultAddress = getFaultAddress(signalInfo, ucontext);

    /* Only VM signals should get here. */
    if (trapNumber < 0) {
        logTrap(signal, ip, faultAddress, 0);
        log_exit(-22, "Non VM signal %d should be handled by the Java signal handler", signal);
    }

#if isa_AMD64
    if (signal == SIGFPE && handleDivideOverflow(ucontext)) {
        // TODO: Determine if trap occurred in Java code; should be fatal otherwise
        if (traceTraps || log_TRAP) {
            log_println("SIGNAL: Handled Integer.MIN_VALUE / -1");
        }
        return;
    }
#endif

    TLA tla = tla_current();
    NativeThreadLocals ntl = nativeThreadLocals_current();
    if (ntl == 0) {
        logTrap(signal, ip, faultAddress, 0);
        log_exit(-22, "could not find native thread locals in trap handler");
    }
    TLA dtla = tla_load(TLA, tla, DTLA);

    boolean trapLogged = false;
    if (traceTraps || log_TRAP) {
        logTrap(signal, ip, faultAddress, dtla);
        trapLogged = true;
    }

    if (tla_load(int, tla, ID) == 0) {
        log_println("Trap taken on primordial thread (this is usually bad)!");
        if (!trapLogged) {
	        logTrap(signal, ip, faultAddress, dtla);
	        trapLogged = true;
	    }
        primordial = 1;
    }

    if (dtla == 0) {
        log_exit(-21, "could not find DTLA in trap handler");
    }

    if (faultAddress >= ntl->stackRedZone && faultAddress < ntl->stackBase + ntl->stackSize && !primordial) {
        if (faultAddress < ntl->stackYellowZone) {
            /* The faultAddress is in the red zone; we shouldn't be alive */
            if (ntl->stackRedZoneIsProtectedByVM) {
                // Only unprotect the red guard zone if the VM (and not the thread library) protected it
                virtualMemory_unprotectPages(ntl->stackRedZone, STACK_RED_ZONE_PAGES);
                trapNumber = STACK_FATAL;
            } else {
                // If VM cannot unprotect the red guard zone page(s), it's not possible
                // to call the Java trap stub (which calls other compiled methods that will
                // bang the stack); just exit now without a stack trace
                if (!trapLogged) {
                    logTrap(signal, ip, faultAddress, dtla);
                    trapLogged = true;
                }
                log_exit(1, "fatal stack fault in red zone");
            }
        } else if (faultAddress < ntl->stackYellowZone + virtualMemory_getPageSize()) {
            /* the faultAddress is in the yellow zone; assume this is a stack fault. */
            virtualMemory_unprotectPages(ntl->stackYellowZone, STACK_YELLOW_ZONE_PAGES);
            trapNumber = STACK_FAULT;
        } else {
            blueZoneTrap(ntl);
            return;
        }
    }

    /* save the trap information in the thread locals */
    tla_store3(dtla, TRAP_NUMBER, trapNumber);
    tla_store3(dtla, TRAP_INSTRUCTION_POINTER, getInstructionPointer(ucontext));
    tla_store3(dtla, TRAP_FAULT_ADDRESS, faultAddress);

#if os_SOLARIS && isa_SPARC
	/* save the value of the safepoint latch at the trapped instruction */
    tla_store3(dtla, TRAP_LATCH_REGISTER, ucontext->uc_mcontext.gregs[REG_G2]);
    /* set the safepoint latch register of the trapped frame to the disabled state */
    ucontext->uc_mcontext.gregs[REG_G2] = (Address) dtla;
#elif isa_AMD64 && (os_SOLARIS || os_LINUX)
    tla_store3(dtla, TRAP_LATCH_REGISTER, ucontext->uc_mcontext.gregs[REG_R14]);
    ucontext->uc_mcontext.gregs[REG_R14] = (Address) dtla;
#elif isa_AMD64 && os_DARWIN
    tla_store3(dtla, TRAP_LATCH_REGISTER, ucontext->uc_mcontext->__ss.__r14);
    ucontext->uc_mcontext->__ss.__r14 = (Address) dtla;
#elif isa_AMD64 && os_MAXVE
    tla_store3(dtla, TRAP_LATCH_REGISTER, ucontext->r14);
    ucontext->r14 = (Address) dtla;
#else
    c_UNIMPLEMENTED();
#endif

    setInstructionPointer(ucontext, theJavaTrapStub);
}

/**
 * The handler for signals handled by SignalDispatcher.java.
 */
static void userSignalHandlerDef(int signal, SigInfo *signalInfo, UContext *ucontext) {
    void postSignal(int signal);
    postSignal(signal);
}

/* Defined global declared in trap.h */
SignalHandlerFunction userSignalHandler = (SignalHandlerFunction) userSignalHandlerDef;

/**
 * Implementation of com.sun.max.vm.runtime.Trap.nativeInitialize().
 */
void nativeTrapInitialize(Address javaTrapStub) {
    /* This function must be called on the primordial thread. */
    c_ASSERT(tla_load(int, tla_current(), ID) == 0);

    theJavaTrapStub = javaTrapStub;
    setSignalHandler(SIGSEGV, (SignalHandlerFunction) vmSignalHandler);
    setSignalHandler(SIGILL, (SignalHandlerFunction) vmSignalHandler);
    setSignalHandler(SIGFPE, (SignalHandlerFunction) vmSignalHandler);

#if !os_MAXVE
    setSignalHandler(SIGBUS, (SignalHandlerFunction) vmSignalHandler);
    setSignalHandler(SIGUSR1, (SignalHandlerFunction) vmSignalHandler);

    sigfillset(&allSignals);

    /* Save the current signal mask to apply it to the VM operation thread. */
    thread_setSignalMask(0, NULL, &vmAndDefaultSignals);

    /* Define the VM signals mask. */
    sigemptyset(&vmSignals);
    sigaddset(&vmSignals, SIGSEGV);
    sigaddset(&vmSignals, SIGBUS);
    sigaddset(&vmSignals, SIGILL);
    sigaddset(&vmSignals, SIGFPE);
    sigaddset(&vmSignals, SIGUSR1);

    /* Let all threads be stopped by a debugger. */
    sigaddset(&vmSignals, SIGTRAP);

    /* Apply the normal thread mask to the primordial thread. */
    thread_setSignalMask(SIG_BLOCK, &allSignals, NULL);
    thread_setSignalMask(SIG_UNBLOCK, &vmSignals, NULL);
#endif
}

/**
 * Implementation of com.sun.max.vm.runtime.Trap.nativeSetTracing().
 */
void nativeSetTrapTracing(boolean flag) {
    traceTraps = flag;
}
