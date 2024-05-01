package miniJava.CodeGeneration.x64.ISA;

import java.util.Map;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.Reg64;
import miniJava.CodeGeneration.x64.Reg8;
import miniJava.CodeGeneration.x64.x64;
import miniJava.CodeGeneration.x64.Reg.RegSize;

import java.util.Collections;
import java.util.HashMap;
import miniJava.AbstractSyntaxTrees.AST;


public abstract class SimpleMathInstruction extends Instruction {
	abstract protected SimpleMathOp _thisOp();
	
	// rm,r variants
	public SimpleMathInstruction(RByte modrmsib, AST source) {
		super(source);
		byte[] modrmsibBytes = modrmsib.getBytes();
		importREX(modrmsib);
		opcodeBytes.write(_RegRegOpcode.get(_thisOp()));
		x64.writeBytes(immBytes,modrmsibBytes);
	}
	
	// do we have an immediate afterwards?
	public SimpleMathInstruction(RByte modrmsib, int imm, AST source) {
		super(source);
		//rexW = true;
		modrmsib.SetRegR(x64.mod543ToReg(_thisOp().idx));
		byte[] modrmsibBytes = modrmsib.getBytes();
		importREX(modrmsib);
		if( x64.isOneByte(imm) ) {
			opcodeBytes.write(0x83);
			x64.writeBytes(immBytes,modrmsibBytes);
			immBytes.write(imm);
		} else if (modrmsib.getSize() == RegSize.m8){
			//1 byte instruction
			opcodeBytes.write(0x80);
			x64.writeBytes(immBytes, modrmsibBytes);
			immBytes.write(imm);
		} else {
			opcodeBytes.write(0x81);
			x64.writeBytes(immBytes,modrmsibBytes);
			x64.writeInt(immBytes,imm);
		}
	}

	
	// manually specify rex
	public SimpleMathInstruction(RByte modrmsib, int imm, boolean rexW, AST source) {
		this(modrmsib,imm,source);
		this.rexW = rexW;
	}
	
	protected enum SimpleMathOp {
		ADD(0), OR(1), ADC(2), SBB(3), AND(4), SUB(5), XOR(6), CMP(7);
		public int idx;
		private SimpleMathOp(int idx) {
			this.idx = idx;
		}
	}
	
	// this can be computed in the constructor, but this is easier to explain
	// ALL MATH OPERATIONS ARE IN "MR" MODE IN FELIX CLOUTIER
	private static final Map<SimpleMathOp,Integer> _RegRegOpcode;
	static {
		Map<SimpleMathOp,Integer> rrOp = new HashMap<SimpleMathOp,Integer>();
		// 0x01, 0x09, 0x11, 0x19, 0x21, 0x29, 0x31, 0x39
		// respectively are ADD, OR, ADC, SBB, AND, SUB, XOR, CMP reg,reg
		rrOp.put(SimpleMathOp.ADD,0x01 + (8 * 0));
		rrOp.put(SimpleMathOp.OR,0x01 + (8 * 1));
		rrOp.put(SimpleMathOp.ADC,0x01 + (8 * 2));
		rrOp.put(SimpleMathOp.SBB,0x01 + (8 * 3));
		rrOp.put(SimpleMathOp.AND,0x01 + (8 * 4));
		rrOp.put(SimpleMathOp.SUB,0x01 + (8 * 5));
		rrOp.put(SimpleMathOp.XOR,0x01 + (8 * 6));
		rrOp.put(SimpleMathOp.CMP,0x01 + (8 * 7));
		_RegRegOpcode = Collections.unmodifiableMap(rrOp);
	}
}
