package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.Reg64;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;


public class Pop extends Instruction {
	public Pop(Reg64 r, AST source) {
		super(source);
		// /TODO: first, check if the Reg64 is R8-R15, if it is, set one of rexB,rexW,rexR,rexX to true (which one?)
		//why does this have to be B???? whatever
		rexB = (r.getIdx() > 7);

		// /TODO: second, find the opcode for pop r, where r is a plain 64-bit register
		// NOTE: x64.getIdx(r) will return a 0-7 index, whereas r.getIdx() returns an index from 0-15
		opcodeBytes.write( 0x58 + x64.getIdx(r)); //opcode
	}
	
	public Pop(RByte modrmsib, AST source) {
		super(source);
		opcodeBytes.write(0x8F);
		modrmsib.SetRegR(x64.mod543ToReg(0));
		byte[] rmsib = modrmsib.getBytes();
		importREX(modrmsib);
		x64.writeBytes(immBytes,rmsib);
	}
}
