// TODO: check this is actually working.
// This file has been adapted from https://github.com/riscv/riscv-glibc/blob/riscv-glibc-2.26/sysdeps/unix/sysv/linux/aarch64/sys/user.h

#define STRINGIFY(s) XSTRINGIFY(s)
#define XSTRINGIFY(s) #s

// #pragma message ("_SYS_USER_H=" STRINGIFY(_SYS_USER_H))

#ifndef _SYS_USER_H
#define _SYS_USER_H	1
#endif

#ifndef CREATED_REG_DESCRIPTION
#define CREATED_REG_DESCRIPTION 1

// #pragma message ("INSIDEEEEEE")

struct user_regs_struct
{
  unsigned long long regs[31];
  // unsigned long long sp;
  unsigned long long pc;
  // unsigned long long pstate;
};

struct user_fpsimd_struct
{
  __uint128_t  vregs[32];
  // unsigned int fpsr;
  // unsigned int fpcr;
};

#endif