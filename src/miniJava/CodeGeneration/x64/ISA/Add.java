package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RByte;
import miniJava.AbstractSyntaxTrees.AST;


/**
 * Adds RM and R; stores in R
 */
public class Add extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.ADD;
	}
	
	public Add(RByte modrmsib, AST source) {
		super(modrmsib, source);
	}

	public Add(RByte modrmsib, int imm, AST source) {
		super(modrmsib,imm, source);
	}
}
