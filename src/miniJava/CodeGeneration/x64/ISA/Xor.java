package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RByte;
import miniJava.AbstractSyntaxTrees.AST;

public class Xor extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.XOR;
	}
	
	public Xor(RByte modrmsib, AST source) {
		super(modrmsib,source);
	}

	public Xor(RByte modrmsib, int imm, AST source) {
		super(modrmsib,imm,source);
	}
}
