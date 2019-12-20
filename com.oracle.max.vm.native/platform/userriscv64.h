// This file has been adapted from https://github.com/riscv/riscv-glibc/blob/riscv-glibc-2.26/sysdeps/unix/sysv/linux/aarch64/sys/user.h

#ifndef _SYS_USER_H
#define _SYS_USER_H	1
#endif

#ifndef CREATED_REG_DESCRIPTION
#define CREATED_REG_DESCRIPTION 1

struct user_regs_struct
{
  unsigned long long regs[31];
  unsigned long long pc;
};

struct user_fpsimd_struct
{
  __uint128_t  vregs[32];
};

#endif
