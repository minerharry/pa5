package miniJava.CodeGeneration.x64.ISA;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;

public class Jmp extends JmpBase {

	// jmp (some register combination)
	public Jmp(RByte modrmsib, AST source) {
		super(modrmsib,source);
		opcodeBytes.write(0xFF);
	}
	
	// jmp imm32 (offset from next instruction)
	public Jmp(int offset, AST source) {
		super(offset,source);
		opcodeBytes.write(0xE9);
	}
	
	// jmp imm8 (offset from next instruction)
	public Jmp(byte offset, AST source) {
		super(offset,source);
		opcodeBytes.write(0xEB);
	}
	
	// jmp imm8/32 (offset calculated)
	public Jmp(int curAddr, int destAddr, boolean asByte, AST source) {
		super(curAddr,destAddr,asByte,source);
		if( asByte ) {
			opcodeBytes.write(0xEB);
			return;
		}
		opcodeBytes.write(0xE9);
	}

	public Jmp(Instruction other,boolean asByte, boolean after, AST source){
		super(other, asByte, after, source);
		if (asByte){
			opcodeBytes.write(0xEB);
			immBytes.write(0);
		} else {
			opcodeBytes.write(0xE9);
			x64.writeInt(immBytes,0);
		}
	}

	public Jmp(MethodDecl other, boolean asByte, AST source){
		super(other,asByte,source);
		if (asByte){
			opcodeBytes.write(0xEB);
			immBytes.write(0);
		} else {
			opcodeBytes.write(0xE9);
			x64.writeInt(immBytes,0);
		}
	}
}
