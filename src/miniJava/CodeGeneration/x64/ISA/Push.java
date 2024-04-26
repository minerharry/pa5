package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.Reg64;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;


public class Push extends Instruction {
	public Push(int imm,AST source) {
		super(source);
		// TODO: how can we do a push imm32?
		opcodeBytes.write(0x68);
		x64.writeInt(immBytes,imm); //4 bytes
		
	}
	
	public Push(Reg64 reg, AST source) {
		super(source);
		// no need to set rexW, push is always r64 (cannot access ecx/r9d)
		if( reg.getIdx() > 7 )
			rexB = true;
		opcodeBytes.write(0x50 + x64.getIdx(reg));
	}
	
	public Push(RByte modrmsib, AST source) {
		super(source);
		// no need to set rexW, push is always r64 (cannot access ecx/r9d)
		opcodeBytes.write(0xFF);
		
		modrmsib.SetRegR(x64.mod543ToReg(6));
		byte[] rmsib = modrmsib.getBytes();
		importREX(modrmsib);
		x64.writeBytes(immBytes,rmsib);
	}
}
