ENTRY(_Reset)
SECTIONS
{
 . = 0x40000000;
 .startup . : { startup_aarch64.o(.text) }
 .text : { startup_aarch64.o(.text) prelude.o(.text) method.o(.text) adapter.o(.text) }
 .data : { *(.data) }
 .bss : { *(.bss COMMON) }
 . = ALIGN(8);
 . = . + 0x1000; /* 4kB of stack memory */
 stack_top = .;
}
