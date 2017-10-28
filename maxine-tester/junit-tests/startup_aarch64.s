// Bare metal boot-strapping based on ARM DAI 0527A:
// "Bare-metal Boot Code for ARMv8-A Processors"

.global _Reset
_Reset:
 // Reset stack pointer
 ADR x0,stack_top
 MOV sp,x0
 // Initialize Vector Base Address registers for exception handling
 LDR x1, = vector_table_el
 MSR VBAR_EL1, x1
 // Disable access trapping in EL1 and EL0.
 MOV X1, #(0x3 << 20) // FPEN disables trapping to EL1.
 MSR CPACR_EL1, X1
 ISB
 // Jump to generated code
 BL c_entry
success:
 B .
failure:
 B . // We got an exception, go debug

// Typical exception vector table code.
.balign 0x800
vector_table_el:
curr_el_sp0_sync:
// The exception handler for the synchronous
// exception from the current EL using SP0.
 B failure
.balign 0x80
curr_el_sp0_irq:
// The exception handler for the IRQ exception
// from the current EL using SP0.
 B failure
.balign 0x80
curr_el_sp0_fiq:
// The exception handler for the FIQ exception
// from the current EL using SP0.
 B failure
.balign 0x80
curr_el_sp0_serror:
// The exception handler for the system error
// exception from the current EL using SP0.
 B failure
.balign 0x80
curr_el_spx_sync:
// The exception handler for the synchronous
// exception from the current EL using the
// current SP.
 B failure
.balign 0x80
curr_el_spx_irq:
// The exception handler for IRQ exception
// from the current EL using the current SP.
 B failure
.balign 0x80
curr_el_spx_fiq:
// The exception handler for the FIQ exception
// from the current EL using the current SP.
 B failure
.balign 0x80
curr_el_spx_serror:
// The exception handler for the system error
// exception from the current EL using the
// current SP.
 B failure
.balign 0x80
lower_el_aarch64_sync:
// The exception handler for the synchronous
// exception from a lower EL (AArch64).
 B failure
.balign 0x80
lower_el_aarch64_irq:
// The exception handler for the IRQ exception
// from a lower EL (AArch64).
 B failure
.balign 0x80
lower_el_aarch64_fiq:
// The exception handler for the FIQ exception
// from a lower EL (AArch64).
 B failure
.balign 0x80
lower_el_aarch64_serror: // The exception handler for the system error
// exception from a lower EL(AArch64).
 B failure
.balign 0x80
lower_el_aarch32_sync:
// The exception handler for the synchronous
// exception from a lower EL(AArch32).
 B failure
.balign 0x80
lower_el_aarch32_irq:
// The exception handler for the IRQ exception
// from a lower EL (AArch32).
 B failure
.balign 0x80
lower_el_aarch32_fiq:
// The exception handler for the FIQ exception
// from a lower EL (AArch32).
 B failure
.balign 0x80
lower_el_aarch32_serror: // The exception handler for the system error
// exception from a lower EL(AArch32).
 B failure
