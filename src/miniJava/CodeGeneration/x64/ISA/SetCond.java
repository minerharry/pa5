package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Condition;
import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.Reg8;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;

public class SetCond extends Instruction {
	public SetCond(Condition cond, Reg8 dest, AST source) {
		super(source);
		opcodeBytes.write(0x0F);
		switch( cond ) {
		case E: opcodeBytes.write(0x94); break;
		case NE: opcodeBytes.write(0x95); break;
		case LT: opcodeBytes.write(0x9C); break;
		case GTE: opcodeBytes.write(0x9D); break;
		case LTE: opcodeBytes.write(0x9E); break;
		case GT: opcodeBytes.write(0x9F); break;
		};
		
		immBytes.write(0xC0 + x64.getIdx(dest));
		if (dest.idx > 7){
			rexB = true;
		}
	}
}
