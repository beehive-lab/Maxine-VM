set breakpoint pending on
set confirm on
set logging redirect on
set logging overwrite on
set logging on
break maxine.c:515
run
set logging overwrite on
info proc mappings
shell echo set \$l=$(grep maxine.vm gdb.txt | tail -1f | awk '{print $1}') >/tmp/foo.gdb1
source /tmp/foo.gdb1
shell echo set \$u=$(grep maxine.vm gdb.txt | tail -1f | awk '{print $2}') >/tmp/foo.gdb2
source /tmp/foo.gdb2
shell echo set \$t=$(../junit-tests/findCodeSequence Field_get02.test | awk '{print $3}') > /tmp/foo.gdb
source /tmp/foo.gdb
set logging off
find /g $l,$u,$t
