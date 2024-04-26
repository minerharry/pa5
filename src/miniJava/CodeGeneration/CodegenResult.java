package miniJava.CodeGeneration;

import java.util.ArrayList;
import java.util.List;

import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.InstructionList;

class MethodTuple {
    public InstructionList body;
    public MethodDecl method;
    public MethodTuple(InstructionList body, MethodDecl method) {
        this.body = body;
        this.method = method;
    }
}

public class CodegenResult {
    public InstructionList code; //any code necessary for initialization
    public List<MethodTuple> methods;
    
    public CodegenResult(){
        code = new InstructionList();
        methods = new ArrayList<>();
    }
    
    public CodegenResult(InstructionList instructions, List<MethodTuple> methods){
        code = instructions;
        this.methods = methods;
    }
    
    public CodegenResult(Iterable<CodegenResult> results){
        code = new InstructionList();
        methods = new ArrayList<>();
        addAll(results);
    }
    
    public CodegenResult(CodegenResult... results){
        code = new InstructionList();
        methods = new ArrayList<>();
        addAll(results);
    }

    public void addAll(Iterable<CodegenResult> results){
        for (CodegenResult res : results){
            code.addAll(res.code);
            methods.addAll(res.methods);
        }
    }    
    
    public void addAll(CodegenResult... results){
        for (CodegenResult res : results){
            code.addAll(res.code);
            methods.addAll(res.methods);
        }
    }
}
