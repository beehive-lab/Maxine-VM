#include "./codebuffer.c"

void (*pf)(int) = (void (*))(code);
(*pf)(1);

}
