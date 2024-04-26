package miniJava.CodeGeneration.x64.ISA;

import java.io.ByteArrayOutputStream;

import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.InstructionList;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;


public class Call extends Instruction {
	public MethodDecl ref;

	public Call(int offset, AST source) {
		super(source);
		opcodeBytes.write(0xE8);
		x64.writeInt(immBytes,offset);
	}
	
	public Call(int curAddr, int destAddr, AST source) {
		super(source);
		opcodeBytes.write(0xE8);
		x64.writeInt(immBytes, destAddr - curAddr - 5);
	}
	
	public Call(RByte modrmsib, AST source) {
		super(source);
		opcodeBytes.write(0xFF);
		
		modrmsib.SetRegR(x64.mod543ToReg(2));
		byte[] rmsib = modrmsib.getBytes();
		importREX(modrmsib);
		x64.writeBytes(immBytes,rmsib);
	}

	public Call(MethodDecl md, AST source){ //indirect call, will be instantiated
		super(source);
		opcodeBytes.write(0xE8); //still gonna be an immediate call, so write
		x64.writeInt(immBytes, 0);
		ref = md;
	}

	@Override
	public void final_patch(InstructionList asm){ //TODO: How do I do this properly?
		//TODO: Like jump
		int destIdx = ref.basePointerOffset; 
		int offset = asm.get(destIdx).startAddress - this.startAddress - 5;
		immBytes = new ByteArrayOutputStream(); //reset immbytes
		x64.writeInt(immBytes, offset);
	}	
}
