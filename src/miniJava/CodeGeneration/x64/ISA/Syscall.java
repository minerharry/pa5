package miniJava.CodeGeneration.x64.ISA;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.CodeGeneration.x64.Instruction;

public class Syscall extends Instruction {
	public Syscall(AST source) {
		super(source);
		// TODO: syscall is two bytes
		opcodeBytes.write(0x0f);
		opcodeBytes.write(0x05);
	}
}
