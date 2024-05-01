package miniJava.CodeGeneration.x64.ISA;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.x64;
import miniJava.CodeGeneration.x64.Reg.RegSize;

public class StoreImmediate extends Instruction {
	// Mov_i_to_rm
	// rm,imm32 variants
	public StoreImmediate(RByte modrmsib, int imm, AST source) {
		super(source);
		modrmsib.SetRegR(x64.mod543ToReg(0));
		byte[] modrmsibBytes = modrmsib.getBytes();
		importREX(modrmsib);
		
		if( x64.isOneByte(imm) && modrmsib.getSize() == RegSize.m8 ) {
			// mov rm8, imm8
			opcodeBytes.write(0xC6);
			x64.writeBytes(immBytes,modrmsibBytes);
			immBytes.write(imm);
			return;
		}
		
		// mov rm64, imm32
		opcodeBytes.write(0xC7);
		x64.writeBytes(immBytes,modrmsibBytes);
		x64.writeInt(immBytes,imm);
	}
}
