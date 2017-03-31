    .text
    .global asm_entry
asm_entry: 
    movw r0,#0xffff
    movt r0,#4
    movt r1,0x1001
    movt r2,0xffff
    movw r3,#0xf0
    mov r4,#12
    add r5,r1,r0
donowt: b donowt 
    b donowt
