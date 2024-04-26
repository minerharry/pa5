package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RByte;
import miniJava.AbstractSyntaxTrees.AST;

public class Sub extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.SUB;
	}
	
	public Sub(RByte modrmsib, AST source) {
		super(modrmsib,source);
	}

	public Sub(RByte modrmsib, int imm, AST source) {
		super(modrmsib,imm,source);
	}
}
