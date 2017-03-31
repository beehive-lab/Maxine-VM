#set confirm off
set breakpoint pending on
set logging overwrite on
set logging on
break maxine.c:545
run > temp.txt
shell echo set \$x=$(grep "Main method" temp.txt | cut -d' ' -f4) >/tmp/foo.gdb
source /tmp/foo.gdb
break *$x
continue
set logging overwrite on
continue
set $upper=$pc-0x800
set $low=$pc+0x40
disas $upper,$low 
p $pc
shell maxine-tester/junit-tests/method_finder

