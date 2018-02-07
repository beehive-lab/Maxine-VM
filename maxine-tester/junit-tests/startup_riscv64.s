# Bare metal boot-strapping based on
# https://github.com/riscv/riscv-tests/blob/master/benchmarks/common/crt.S

.global _Reset
_Reset:
  # Reset stack pointer
  la sp, stack_top
  # enable FPU and accelerator if present
  li t0, 0x00006000 | 0x00018000
  csrs mstatus, t0
  # initialize trap vector
  la t0, failure
  csrw mtvec, t0
  # Jump to our code
  jal c_entry
success:
  j .
failure:
  j . # We got an exception, go debug
