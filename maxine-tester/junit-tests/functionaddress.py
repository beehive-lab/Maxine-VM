# find /g 0x39571ed8, 0x39571f0c, 16375862305113558263

import sys;
import os;
def movwHelper(imm16): 
	instruction = 0x03000000
	condition = 0xe
	instruction |= (condition) << 28;
	instruction |= (imm16 >> 12) << 16;
	instruction |= (12) << 12;
	instruction |= imm16 & 0xfff;
	# print instruction
	return instruction;

def movtHelper(imm16):
	instruction = 0x03400000
	condition = 0xe
	instruction |= (condition) << 28;
	instruction |= (imm16 >> 12) << 16;
	instruction |= (12) << 12;
	instruction |= imm16 & 0xfff;
	# print instruction
	return instruction;

argc = len(sys.argv)
if argc == 3:
	y = int(sys.argv[1],16)
	z = int (sys.argv[2],16)
    # print len(sys.argv)
if argc == 2:
	x = int(sys.argv[1]);
	y = x >> 16;
	z = x & 0xffff


w = (y << 16) + z
movtNum = movtHelper(y)
movwNum = movwHelper(z)
theExpr = movtNum << 32;
theExpr = theExpr + movwNum
print 'find /g ', hex(theExpr)
# print w
os.execv('/bin/grep',['/bin/grep',str(w) , './debug_methods']);
# print w

