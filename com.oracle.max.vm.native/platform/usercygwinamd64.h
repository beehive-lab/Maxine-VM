struct user_fpregs_struct
{
  unsigned short int	cwd;
  unsigned short int	swd;
  unsigned short int	ftw;
  unsigned short int	fop;
  __extension__ unsigned long long int rip;
  __extension__ unsigned long long int rdp;
  unsigned int		mxcsr;
  unsigned int		mxcr_mask;
  unsigned int		st_space[32];   /* 8*16 bytes for each FP-reg = 128 bytes */
  unsigned int		xmm_space[64];  /* 16*16 bytes for each XMM-reg = 256 bytes */
  unsigned int		padding[24];
};

struct user_regs_struct
{
  __extension__ unsigned long long int r15;
  __extension__ unsigned long long int r14;
  __extension__ unsigned long long int r13;
  __extension__ unsigned long long int r12;
  __extension__ unsigned long long int rbp;
  __extension__ unsigned long long int rbx;
  __extension__ unsigned long long int r11;
  __extension__ unsigned long long int r10;
  __extension__ unsigned long long int r9;
  __extension__ unsigned long long int r8;
  __extension__ unsigned long long int rax;
  __extension__ unsigned long long int rcx;
  __extension__ unsigned long long int rdx;
  __extension__ unsigned long long int rsi;
  __extension__ unsigned long long int rdi;
  __extension__ unsigned long long int orig_rax;
  __extension__ unsigned long long int rip;
  __extension__ unsigned long long int cs;
  __extension__ unsigned long long int eflags;
  __extension__ unsigned long long int rsp;
  __extension__ unsigned long long int ss;
  __extension__ unsigned long long int fs_base;
  __extension__ unsigned long long int gs_base;
  __extension__ unsigned long long int ds;
  __extension__ unsigned long long int es;
  __extension__ unsigned long long int fs;
  __extension__ unsigned long long int gs;
};
