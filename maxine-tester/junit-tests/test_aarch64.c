volatile unsigned int * const UART0DR = (unsigned int *)0x09000000;

void print_uart0(const char *s) {
    while(*s != '\0') {
        *UART0DR = (unsigned int)(*s);
        s++;
    }
}

#include "./codebuffer.c"

// Add a main method to trick gcc to consider c_entry as a normal
// function and save LR register
int main(int argc, char *argv[argc])
{
    c_entry();
    return 0;
}
