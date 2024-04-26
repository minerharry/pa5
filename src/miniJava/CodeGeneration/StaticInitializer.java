package miniJava.CodeGeneration;
import java.util.ArrayList;

import miniJava.CompilerError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.InstructionList;

import miniJava.CodeGeneration.x64.RByte;
import miniJava.CodeGeneration.x64.Reg64;
import miniJava.CodeGeneration.x64.ISA.*;
import miniJava.ContextualAnalysis.TypeResult;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

// class InstructionList {
//     public InstructionList code;
//     public int staticOffset;
//     public InstructionList(){
//         code = new InstructionList();
//     }
//     public InstructionList(InstructionList c){
//         code = c;
//     }
//     public void add(Instruction i){
//         code.add(i);
//     }


// }

/**
 * Key to static stuff: all top-level classes are considered to be members of a single larger "class" body, the package. The package body
 * is the first section of the heap, and really is just a list of pointers to each class definition. Each outermost classdecl listed has 
 * its offset set to its offset relative to the package location, which for now is just the start of the heap. It should be possible to
 * change that first address, should only be referenced in one place in the code, but I think it should be fine?
 */
public class StaticInitializer implements Visitor<Object,InstructionList> {

    public MethodDecl mainMethod;

    public InstructionList parse(Package prog){
        Object arg = new Object();
        return prog.visit(this, arg);
    }

    @Override
    public InstructionList visitPackage(Package prog, Object arg) {
        InstructionList res = new InstructionList();
        // res.addAll(CodeGenerator.makeMalloc());
        // res.addAll(new Store)
        //TODO: INITIALIZE ON THE HEAP, OR THE STACK?

        int classOffset = 0;
        for (ClassMemberDecl cmd : prog.classes){
            cmd.basePointerOffset = classOffset;
            InstructionList cres = cmd.visit(this,arg);
            res.addAll(cres);
            classOffset += cmd.staticSize;
        }

        //lets do the stack I guess; just make a whole bunch of space
        res.add(new Store(new RByte(Reg64.R15,Reg64.RSP), prog));
        res.add(new Add(new RByte(Reg64.RSP,true),classOffset, prog));

        return res;
    }

    @Override
    public InstructionList visitClassDecl(ClassDecl cd, Object arg) {
        InstructionList code = new InstructionList();

        //now we need to make space for (and instantiate!) all the static fields and classmembers. 
		//I don't think static methods will have data to be stored? I suppose we could store the pointers but that seems unnecessary
        //Register static class location
    
        //Allocate **STATIC** fields, classmember pointers
        int offset = 0;
		for (FieldDecl fd : cd.fields){
			if (!fd.isStatic()){ continue; }
            fd.basePointerOffset = offset;
            offset += fd.type.getTypeSize();
		}
        for (ClassMemberDecl cmd : cd.classmembers){
            if (!cmd.isStatic()){ continue; }
            cmd.basePointerOffset = offset;
            cmd.visit(this,arg);
            offset += cmd.staticSize;
        }
        cd.staticSize = offset;


        //NOTE: THIS DOES NOT INITIALIZE ANY MEMORY. 
        //Instead, this is establishing the *TEMPLATE* for these classes

        //TODO: Initializers? Might need **another** traversal...
        return code;
    }

    @Override
    public InstructionList visitFileHeader(FileHeader fileHeader, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFileHeader'");
    }

    @Override
    public InstructionList visitPackageDecl(PackageDecl packageDecl, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitPackageDecl'");
    }

    @Override
    public InstructionList visitImportStatement(ImportStatement importStatement, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitImportStatement'");
    }

    @Override
    public InstructionList visitDeclKeywords(DeclKeywords declParams, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDeclKeywords'");
    }

    @Override
    public InstructionList visitModifier(Modifier modifier, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitModifier'");
    }

    @Override
    public InstructionList visitProtection(Protection protection, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitProtection'");
    }

    @Override
    public InstructionList visitGenericVar(GenericVar genericVar, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitGenericVar'");
    }

    @Override
    public InstructionList visitAnnotationDecl(AnnotationDecl annotationDecl, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAnnotationDecl'");
    }

    @Override
    public InstructionList visitEnumDecl(EnumDecl enumDecl, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitEnumDecl'");
    }

    @Override
    public InstructionList visitInterfaceDecl(InterfaceDecl interfaceDecl, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitInterfaceDecl'");
    }

    @Override
    public InstructionList visitEnumElement(EnumElement enumElement, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitEnumElement'");
    }

    @Override
    public InstructionList visitFieldDecl(FieldDecl fd, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFieldDecl'");
    }

    @Override
    public InstructionList visitConstructorDecl(ConstructorDecl constructorDecl, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitConstructorDecl'");
    }

    @Override
    public InstructionList visitMethodDecl(MethodDecl md, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMethodDecl'");
    }

    @Override
    public InstructionList visitEmptyMethodDecl(EmptyMethodDecl emptyMethodDecl, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitEmptyMethodDecl'");
    }

    @Override
    public InstructionList visitParameterDecl(ParameterDecl pd, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitParameterDecl'");
    }

    @Override
    public InstructionList visitLocalDecl(LocalDecl decl, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitLocalDecl'");
    }

    @Override
    public InstructionList visitInitializer(Kwarg initializer, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitInitializer'");
    }

    @Override
    public InstructionList visitPrimitiveType(PrimitiveType type, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitPrimitiveType'");
    }

    @Override
    public InstructionList visitIdentifierType(IdentifierType type, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIdentifierType'");
    }

    @Override
    public InstructionList visitArrayType(ArrayType type, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitArrayType'");
    }

    @Override
    public InstructionList visitEllipsisType(EllipsisType ellipsisType, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitEllipsisType'");
    }

    @Override
    public InstructionList visitQualType(QualType qualType, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitQualType'");
    }

    @Override
    public InstructionList visitGenericType(GenericType genericType, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitGenericType'");
    }

    @Override
    public InstructionList visitAssignStmt(AssignStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAssignStmt'");
    }

    @Override
    public InstructionList visitReturnStmt(ReturnStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitReturnStmt'");
    }

    @Override
    public InstructionList visitBreakStmt(BreakStmt breakStmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBreakStmt'");
    }

    @Override
    public InstructionList visitLocalDeclStmt(LocalDeclStmt localDeclStmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitLocalDeclStmt'");
    }

    @Override
    public InstructionList visitThrowStmt(ThrowStmt throwStmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitThrowStmt'");
    }

    @Override
    public InstructionList visitContinueStmt(ContinueStmt continueStmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitContinueStmt'");
    }

    @Override
    public InstructionList visitExprStatement(ExprStmt exprStatement, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitExprStatement'");
    }

    @Override
    public InstructionList visitBlockStmt(BlockStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBlockStmt'");
    }

    @Override
    public InstructionList visitIfStmt(IfStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIfStmt'");
    }

    @Override
    public InstructionList visitWhileStmt(WhileStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitWhileStmt'");
    }

    @Override
    public InstructionList visitDoWhileStmt(DoWhileStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDoWhileStmt'");
    }

    @Override
    public InstructionList visitForStmt(ForStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitForStmt'");
    }

    @Override
    public InstructionList visitForEachStmt(ForEachStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitForEachStmt'");
    }

    @Override
    public InstructionList visitTryCatchFinallyStmt(TryCatchFinallyStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTryCatchFinallyStmt'");
    }

    @Override
    public InstructionList visitCatchBlock(CatchBlock block, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCatchBlock'");
    }

    @Override
    public InstructionList visitSwitchStmt(SwitchStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitSwitchStmt'");
    }

    @Override
    public InstructionList visitCaseBlock(CaseBlock block, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCaseBlock'");
    }

    @Override
    public InstructionList visitUnaryExpr(UnaryExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitUnaryExpr'");
    }

    @Override
    public InstructionList visitBinaryExpr(BinaryExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBinaryExpr'");
    }

    @Override
    public InstructionList visitIxExpr(IxExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIxExpr'");
    }

    @Override
    public InstructionList visitCallExpr(CallExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCallExpr'");
    }

    @Override
    public InstructionList visitLiteralExpr(LiteralExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitLiteralExpr'");
    }

    @Override
    public InstructionList visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNewArrayExpr'");
    }

    @Override
    public InstructionList visitDotExpr(DotExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDotExpr'");
    }

    @Override
    public InstructionList visitMethodRefExpr(MethodRefExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMethodRefExpr'");
    }

    @Override
    public InstructionList visitArrayLiteralExpr(ArrayLiteralExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitArrayLiteralExpr'");
    }

    @Override
    public InstructionList visitIncExpr(IncExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIncExpr'");
    }

    @Override
    public InstructionList visitTernaryExpr(TernaryExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTernaryExpr'");
    }

    @Override
    public InstructionList visitThisRef(ThisRef ref, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitThisRef'");
    }

    @Override
    public InstructionList visitIdRef(IdRef ref, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIdRef'");
    }

    @Override
    public InstructionList visitIdentifier(Identifier id, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIdentifier'");
    }

    @Override
    public InstructionList visitOperator(Operator op, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOperator'");
    }

    @Override
    public InstructionList visitIntLiteral(IntLiteral num, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIntLiteral'");
    }

    @Override
    public InstructionList visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBooleanLiteral'");
    }

    @Override
    public InstructionList visitStringLiteral(StringLiteral literal, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitStringLiteral'");
    }

    @Override
    public InstructionList visitFloatLiteral(FloatLiteral literal, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFloatLiteral'");
    }

    @Override
    public InstructionList visitCharLiteral(CharLiteral literal, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCharLiteral'");
    }

    @Override
    public InstructionList visitNullLiteral(NullLiteral literal, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNullLiteral'");
    }

    @Override
    public InstructionList visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNewObjectExpr'");
    }

    @Override
    public InstructionList visitSyscallStmt(SyscallStmt syscallStmt, Object arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitSyscallStmt'");
    }
    
}

// class Object {
//     private int nextOffset = 0;
//     public int getOffset(TypeDenoter objType){ //kinda dumb but I love it
//         int res = nextOffset;
//         nextOffset += objType.getTypeSize();
//         return res;
//     }
// }