.global _Reset
_Reset:
 ADR x0,stack_top
 MOV sp,x0
 BL c_entry
 B .
