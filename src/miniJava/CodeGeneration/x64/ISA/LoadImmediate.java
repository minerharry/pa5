package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.Reg64;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;

//Mov_i64_to_R
public class LoadImmediate extends Instruction {
	// mov r64,imm64 variant
	public LoadImmediate(Reg64 reg, long imm64, AST source) {
		super(source);
		rexW = true; // operand is 64bit
		// TODO: first, check if the Reg64 is R8-R15, if it is, set one of rexB,rexW,rexR,rexX to true (which one?)
		if (reg.regIdx > 7){
			rexB = true; //no idea why RexB tbh but it's the one that works
		}
		// TODO: second, find the opcode for pop r, where r is a plain 64-bit register
		// NOTE: x64.getIdx(r) will return a 0-7 index, whereas r.getIdx() returns an index from 0-15
		opcodeBytes.write( 0xB8 + x64.getIdx(reg)); //opcode gets shifted by reg#!
		x64.writeLong(immBytes,imm64);
	}
}
