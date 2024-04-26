package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;


public class Inc extends Instruction {
    /**
     * M mode - R/M or memory location. R register gets overwritten
     * @param mod
     */
    public Inc(RByte mod, AST source){
        super(source);
        //opcode is FF /0; write FF, set RegR to 0
        opcodeBytes.write(0xFF);
        mod.SetRegR(x64.mod543ToReg(0));
        byte[] rmsib = mod.getBytes();
		importREX(mod);
		x64.writeBytes(immBytes,rmsib);
    }
    
}
