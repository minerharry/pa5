package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.Reg.RegSize;
import miniJava.CodeGeneration.x64.Reg64;
import miniJava.CodeGeneration.x64.x64;
import miniJava.AbstractSyntaxTrees.AST;


public class RepStos extends Instruction {
    /**
     * Instruction takes no operands! Just different flags and opcode based on memory size (how many big of memory to copy at a time)
     * (R)AX - source data register
     * RDI - start address
     * RCX - total number of operations (total bytes writen will be memory size * start value of RCX)
     */
    public RepStos(RegSize rsize, AST source){
        super(source);
        switch (rsize) {
            case m16:
                rexW = true;
            case m8:
                opcodeBytes.write(0xAA);
                break;
            
            case m64:
                rexW = true;
            case m32:
                opcodeBytes.write(0xAB);
                break;
        }
    }

    @Override
    protected Byte getPrefix() {
        return (byte) 0xF3; //REP prefix
    }


    
}
