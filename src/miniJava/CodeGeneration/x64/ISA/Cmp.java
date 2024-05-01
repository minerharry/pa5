package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.Reg8;
import miniJava.AbstractSyntaxTrees.AST;

/**MR mode - computes second (RegR) minus first (RM) and stores flags like SUB does
 * Flags are stored in Condition! Very useful
 */
public class Cmp extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.CMP;
	}

	public Cmp(RByte modrmsib, AST source) {
		super(modrmsib,source);
	}

	public Cmp(RByte modrmsib, int imm, AST source) {
		super(modrmsib,imm, source);
	}
}
