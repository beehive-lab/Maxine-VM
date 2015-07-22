.global _Reset
_Reset:
 ADR x0,stack_top
 MOV sp,x0
 BL VFP_enable
 BL c_entry
 B .

.EQU VFPVAL, 0x30
VFP_enable:
	MOVZ x1,#VFPVAL,LSL #16
	MSR CPACR_EL1, x1
        BR x30
