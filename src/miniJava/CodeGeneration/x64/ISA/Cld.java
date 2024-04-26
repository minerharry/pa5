package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.AbstractSyntaxTrees.AST;

public class Cld extends Instruction {
    public Cld(AST source){
        super(source);
        opcodeBytes.write(0xFC);
        //that might literally be it
    }
    
}
