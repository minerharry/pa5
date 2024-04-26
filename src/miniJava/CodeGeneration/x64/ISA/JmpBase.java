package miniJava.CodeGeneration.x64.ISA;

import java.io.ByteArrayOutputStream;

import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.InstructionList;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;


public abstract class JmpBase extends Instruction {
    public Instruction dest = null;
	public MethodDecl method = null;
	public boolean asByte;
	/** whether to jump *to* the specified instruction, or *after* the specified instruction */
	public boolean after;


    // jmp (some register combination)
	public JmpBase(RByte modrmsib, AST source) {
		super(source);
		modrmsib.SetRegR(x64.mod543ToReg(4));
		byte[] rmsib = modrmsib.getBytes();
		importREX(modrmsib);
		x64.writeBytes(immBytes,rmsib);
	}
	
	// jmp imm32 (offset from next instruction)
	public JmpBase(int offset, AST source) {
		super(source);
		x64.writeInt(immBytes,offset);
	}
	
	// jmp imm8 (offset from next instruction)
	public JmpBase(byte offset, AST source) {
		super(source);
		immBytes.write(offset);
	}
	
	// jmp imm8/32 (offset calculated)
	public JmpBase(int curAddr, int destAddr, boolean asByte, AST source) {
		super(source);
		if( asByte ) {
			immBytes.write( destAddr - curAddr - 2 );
			return;
		}
		x64.writeInt(immBytes, destAddr - curAddr - 5);
	}

	public JmpBase(Instruction other, boolean asByte, boolean after, AST source){
		super(source);
		dest = other;
		this.asByte = asByte;
		this.after = after;
	}

	public JmpBase(MethodDecl other, boolean asByte, AST source){
		super(source);
		method = other;
		this.asByte = asByte;
		this.after = false;
	}

	
	@Override
	public void final_patch(InstructionList asm){
		if (method != null){
			dest = asm.get(method.basePointerOffset); //method points to first instruction when its added to the codebase
		}
		if (dest == null){ return; }
		int offset = dest.startAddress - this.startAddress;
		if (after){
			offset += dest.size();
		}
		offset -= this.size();
		immBytes = new ByteArrayOutputStream();
		if( asByte ) {	
			immBytes.write( offset ); 
			return;
		}
		x64.writeInt(immBytes,offset);
	}
    
}
