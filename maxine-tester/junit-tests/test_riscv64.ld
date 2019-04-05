OUTPUT_ARCH( "riscv" )
ENTRY(_Reset)
SECTIONS
{
  . = 0x80000000;
  .text.init : { *(.text.init) }
  .tohost ALIGN(0x1000) : { *(.tohost) }
  .text ALIGN(0x1000) : { *(.text) }
  .data ALIGN(0x1000) : { *(.data) }
  .bss : { *(.bss) }
  .note.gnu.build-id : {}
  . = ALIGN(8);
  . = . + 0x8000; /* 32kB of stack memory */
  stack_top = .;
  _end = .;
}
