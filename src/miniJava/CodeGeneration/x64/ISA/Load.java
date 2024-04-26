package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;

//Mov_RM_to_R
/**
 * Moves RM or [RM] to register R
 */
public class Load extends Instruction {
	// r,rm variants
	/**
	 * Moves RM or [RM] to register R
	 */
	public Load(RByte modrmsib, AST source) {
		super(source);
		byte[] modrmsibBytes = modrmsib.getBytes();
		importREX(modrmsib);
		opcodeBytes.write(0x8B);
		x64.writeBytes(immBytes,modrmsibBytes);
	}
}
