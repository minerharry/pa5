package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RByte;
import miniJava.AbstractSyntaxTrees.AST;


public class Or extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.OR;
	}
	
	public Or(RByte modrmsib,AST source) {
		super(modrmsib,source);
	}

	public Or(RByte modrmsib, int imm, AST source) {
		super(modrmsib,imm,source);
	}
}
