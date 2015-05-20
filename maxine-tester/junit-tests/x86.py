# find /g 0x39571ed8, 0x39571f0c, 16375862305113558263

import sys;
import os;
#mov    $0x200011e4,%r11d

def movHelper(imm32): 
        instruction = 0x11e4bb41 << 32
	instruction |= imm32
	print instruction
        return instruction;


argc = len(sys.argv)
if argc == 3:
	y = int(sys.argv[1],16)
	z = int (sys.argv[2],16)
	y = (y << 16) + z
print len(sys.argv)

movNum = movHelper(y)
print 'find /w ', hex(movNum)
os.execv('/bin/grep',['/bin/grep',str(y) , './debug_methods']);
print y

