package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;

//Mov_R_to_RM
public class Store extends Instruction {
	/**
	 * Moves register R to RM or [RM]
	 */
	public Store(RByte modrmsib, AST Source) {
		super(Source);
		byte[] modrmsibBytes = modrmsib.getBytes();
		importREX(modrmsib);
		opcodeBytes.write(0x89);
		x64.writeBytes(immBytes,modrmsibBytes);
	}
}
