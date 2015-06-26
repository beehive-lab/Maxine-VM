set breakpoint pending on
set confirm on
set logging redirect on
set logging overwrite on
set logging on
break maxine.c:407
run
set logging overwrite on
info proc mappings
shell echo set \$lower=$(grep maxine.vm gdb.txt | tail -1f | awk '{print $1}') >/tmp/foo.gdb
source /tmp/foo.gdb
shell echo set \$upper=$(grep maxine.vm gdb.txt | tail -1f | awk '{print $1}') >/tmp/foo.gdb
source /tmp/foo.gdb
shell echo set \$target=$(../junit-tests/findCodeSequence LeastSignificantBit.test | awk '{print $3}')
find /g $lower,$upper,$target