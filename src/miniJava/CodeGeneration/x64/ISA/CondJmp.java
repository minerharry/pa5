package miniJava.CodeGeneration.x64.ISA;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.CodeGeneration.x64.Condition;
import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;

public class CondJmp extends JmpBase {
	public Condition cond;
	public CondJmp(Condition cond, byte rel8, AST source) {
		super(rel8,source);
		opcodeBytes.write(getImm32Opcode(cond) - 0x10);
	}
	
	public CondJmp(Condition cond, int rel32, AST source) {
		super(rel32,source);
		opcodeBytes.write(0x0F);
		opcodeBytes.write(getImm32Opcode(cond));
	}
	
	public CondJmp(Condition cond, int curAddr, int destAddr, boolean asByte, AST source) {
		super(curAddr, destAddr, asByte, source);
		if( asByte ) {
			opcodeBytes.write( getImm32Opcode(cond) - 0x10 );
			return;
		}
		opcodeBytes.write(0x0F);
		opcodeBytes.write(getImm32Opcode(cond));
	}

	public CondJmp(Condition cond, Instruction other, boolean asByte, boolean after, AST source){
		super(other, asByte, after,source);
		if (asByte){
			opcodeBytes.write( getImm32Opcode(cond) - 0x10 );
			immBytes.write(0);
		} else {
			opcodeBytes.write(0x0F);
			opcodeBytes.write(getImm32Opcode(cond));
			x64.writeInt(immBytes,0);
		}

		this.cond = cond;
	}

	public CondJmp(Condition cond, MethodDecl other, boolean asByte, AST source){
		super(other, asByte, source);
		if (asByte){
			opcodeBytes.write( getImm32Opcode(cond) - 0x10 );
			immBytes.write(0);
		} else {
			opcodeBytes.write(0x0F);
			opcodeBytes.write(getImm32Opcode(cond));
			x64.writeInt(immBytes,0);
		}

		this.cond = cond;
	}

	
	// imm32
	// 84, 85: jz, jnz   / je, jne
	// 8C, 8D: jl, jnl   / jnge, jge
	// 8E, 8F: jle, jnle / jng, jg
	// imm8: subtract above by 0x10
	private int getImm32Opcode(Condition cond) {
		switch(cond) {
		case E: return 0x84;
		case NE: return 0x85;
		case LT: return 0x8C;
		case GTE: return 0x8D;
		case LTE: return 0x8E;
		case GT: return 0x8F;
		}
		
		throw new IllegalArgumentException("Illegal operator: " + cond);
	}

}
