/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of the Visitor interface provides a method visitX
 * for each non-abstract AST class X.  
 */
public interface Visitor<ArgType,ResultType> {

    public default List<ResultType> visitList(Iterable<? extends AST> l,ArgType arg){
      ArrayList<ResultType> rs = new ArrayList<>();
      for (AST a : l){
        rs.add(a.visit(this, arg));
      }
      return rs;
    }

  // Program
    public ResultType visitProgram(Program program, ArgType arg);

  // Package
    public ResultType visitPackage(Package prog, ArgType arg);
    public ResultType visitFileHeader(FileHeader fileHeader, ArgType arg);
    public ResultType visitPackageDecl(PackageReference packageDecl, ArgType arg);
    public ResultType visitImportStatement(ImportStatement importStatement, ArgType arg);

  // Declarations
    public ResultType visitDeclKeywords(DeclKeywords declParams, ArgType arg);
    public ResultType visitModifier(Modifier modifier, ArgType arg);
    public ResultType visitProtection(Protection protection, ArgType arg);
    public ResultType visitGenericVar(GenericVar genericVar, ArgType arg);

    public ResultType visitClassDecl(ClassDecl cd, ArgType arg);
    public ResultType visitAnnotationDecl(AnnotationDecl annotationDecl, ArgType arg);
    public ResultType visitEnumDecl(EnumDecl enumDecl, ArgType arg);
    public ResultType visitInterfaceDecl(InterfaceDecl interfaceDecl, ArgType arg);
    
    public ResultType visitEnumElement(EnumElement enumElement, ArgType arg);
    public ResultType visitFieldDecl(FieldDecl fd, ArgType arg);
    public ResultType visitConstructorDecl(ConstructorDecl constructorDecl, ArgType arg);
    public ResultType visitMethodDecl(MethodDecl md, ArgType arg);
    public ResultType visitEmptyMethodDecl(EmptyMethodDecl emptyMethodDecl, ArgType arg);
    
    public ResultType visitParameterDecl(ParameterDecl pd, ArgType arg);
    public ResultType visitLocalDecl(LocalDecl decl, ArgType arg);
    public ResultType visitInitializer(Kwarg initializer, ArgType arg);
 
  // Types
    public ResultType visitPrimitiveType(PrimitiveType type, ArgType arg);
    public ResultType visitIdentifierType(IdentifierType type, ArgType arg);
    public ResultType visitArrayType(ArrayType type, ArgType arg);
    public ResultType visitEllipsisType(EllipsisType ellipsisType, ArgType arg);
    public ResultType visitQualType(QualType qualType, ArgType arg);
    public ResultType visitGenericType(GenericType genericType, ArgType arg);
    
  // Statements
    public ResultType visitAssignStmt(AssignStmt stmt, ArgType arg);
    public ResultType visitReturnStmt(ReturnStmt stmt, ArgType arg);
    public ResultType visitBreakStmt(BreakStmt breakStmt, ArgType arg);
    public ResultType visitLocalDeclStmt(LocalDeclStmt localDeclStmt, ArgType arg);
    public ResultType visitThrowStmt(ThrowStmt throwStmt, ArgType arg);
    public ResultType visitContinueStmt(ContinueStmt continueStmt, ArgType arg);
    public ResultType visitExprStatement(ExprStmt exprStatement, ArgType arg);

    public ResultType visitBlockStmt(BlockStmt stmt, ArgType arg);
    public ResultType visitIfStmt(IfStmt stmt, ArgType arg);
    public ResultType visitWhileStmt(WhileStmt stmt, ArgType arg);
    public ResultType visitDoWhileStmt(DoWhileStmt stmt, ArgType arg);
    public ResultType visitForStmt(ForStmt stmt, ArgType arg);  
    public ResultType visitForEachStmt(ForEachStmt stmt, ArgType arg);
    public ResultType visitTryCatchFinallyStmt(TryCatchFinallyStmt stmt, ArgType arg);
    public ResultType visitCatchBlock(CatchBlock block, ArgType arg);
    public ResultType visitSwitchStmt(SwitchStmt stmt, ArgType arg);
    public ResultType visitCaseBlock(CaseBlock block, ArgType arg);
    
  // Expressions
    public ResultType visitUnaryExpr(UnaryExpr expr, ArgType arg);
    public ResultType visitBinaryExpr(BinaryExpr expr, ArgType arg);
    public ResultType visitIxExpr(IxExpr expr, ArgType arg);
    public ResultType visitCallExpr(CallExpr expr, ArgType arg);
    public ResultType visitLiteralExpr(LiteralExpr expr, ArgType arg);
    public ResultType visitNewObjectExpr(NewObjectExpr expr, ArgType arg);
    public ResultType visitNewArrayExpr(NewArrayExpr expr, ArgType arg);
    public ResultType visitDotExpr(DotExpr expr, ArgType arg);
    public ResultType visitMethodRefExpr(MethodRefExpr expr, ArgType arg);
    public ResultType visitArrayLiteralExpr(ArrayLiteralExpr expr, ArgType arg);
    public ResultType visitIncExpr(IncExpr expr, ArgType arg);
    public ResultType visitTernaryExpr(TernaryExpr expr, ArgType arg);

    public ResultType visitThisRef(ThisRef ref, ArgType arg);
    public ResultType visitIdRef(IdRef ref, ArgType arg);

  // Terminals
    public ResultType visitIdentifier(Identifier id, ArgType arg);
    public ResultType visitOperator(Operator op, ArgType arg);
    public ResultType visitIntLiteral(IntLiteral num, ArgType arg);
    public ResultType visitBooleanLiteral(BooleanLiteral bool, ArgType arg);
    public ResultType visitStringLiteral(StringLiteral literal, ArgType arg);
    public ResultType visitFloatLiteral(FloatLiteral literal, ArgType arg);
    public ResultType visitCharLiteral(CharLiteral literal, ArgType arg);
    public ResultType visitNullLiteral(NullLiteral literal, ArgType arg);

    public ResultType visitSyscallStmt(SyscallStmt syscallStmt, ArgType arg);

    public ResultType visitOverloadedMethod(OverloadedMethod overloadedMethod, ArgType arg);

    public ResultType visitInstanceOf(InstanceOfExpression instanceOfExpression, ArgType arg);
    
}
