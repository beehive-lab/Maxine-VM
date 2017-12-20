.global _Reset
_Reset:
 LDR sp, =stack_top
 BL Setup_Undef_Stack
 BL VFP_enable
 BL c_entry
success:
failure:
 B .

.EQU Mode_UNDEF, 0x1B
.EQU UNDEF_Stack, 0x4000;
Setup_Undef_Stack:
	MRS r0,CPSR
	MOV r1,r0
	BIC r1,r1,#0x1f
	ORR r1,r1,#Mode_UNDEF
	MSR CPSR_c, r1
	LDR SP,=UNDEF_Stack
	MSR CPSR_c,r0
	BX LR


.EQU VFPVAL, 0x40000000
VFP_enable:
	LDR r0, =(0xF << 20)
	MCR p15, 0, r0, c1, c0, 2
	MOV r1,#VFPVAL
	VMSR FPEXC, r1
	BX LR
