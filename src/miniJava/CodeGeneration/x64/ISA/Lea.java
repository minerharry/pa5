package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;


public class Lea extends Instruction {
	/**
	 * execute [Rm] operation, store result in R
	 * @param modrmsib
	 */
	public Lea(RByte modrmsib, AST source) {
		super(source);
		opcodeBytes.write(0x8D);
		byte[] rmsib = modrmsib.getBytes();
		importREX(modrmsib);
		x64.writeBytes(immBytes,rmsib);
	}
}
