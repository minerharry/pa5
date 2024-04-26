package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RByte;
import miniJava.AbstractSyntaxTrees.AST;


public class And extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.AND;
	}

	public And(RByte modrmsib, AST source) {
		super(modrmsib,source);
	}

	public And(RByte modrmsib, int imm, AST source) {
		super(modrmsib,imm,source);
	}
	
	public And(RByte modrmsib, int imm, boolean signExtend, AST source) {
		super(modrmsib,imm,signExtend,source);
	}
}
