package miniJava.ContextualAnalysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import miniJava.Compiler;
import miniJava.CompilerError;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.Annotation;
import miniJava.AbstractSyntaxTrees.AnnotationDecl;
import miniJava.AbstractSyntaxTrees.ArrayLiteralExpr;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.BreakStmt;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CaseBlock;
import miniJava.AbstractSyntaxTrees.CatchBlock;
import miniJava.AbstractSyntaxTrees.CharLiteral;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassMemberDecl;
import miniJava.AbstractSyntaxTrees.ConstructorDecl;
import miniJava.AbstractSyntaxTrees.ContinueStmt;
import miniJava.AbstractSyntaxTrees.DeclKeywords;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.DoWhileStmt;
import miniJava.AbstractSyntaxTrees.DotExpr;
import miniJava.AbstractSyntaxTrees.EllipsisType;
import miniJava.AbstractSyntaxTrees.EmptyMethodDecl;
import miniJava.AbstractSyntaxTrees.EnumDecl;
import miniJava.AbstractSyntaxTrees.EnumElement;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.ExprStmt;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FileHeader;
import miniJava.AbstractSyntaxTrees.FloatLiteral;
import miniJava.AbstractSyntaxTrees.ForEachStmt;
import miniJava.AbstractSyntaxTrees.ForStmt;
import miniJava.AbstractSyntaxTrees.GenericType;
import miniJava.AbstractSyntaxTrees.GenericVar;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IdentifierType;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.ImportStatement;
import miniJava.AbstractSyntaxTrees.IncExpr;
import miniJava.AbstractSyntaxTrees.Kwarg;
import miniJava.AbstractSyntaxTrees.KwargList;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.InterfaceDecl;
import miniJava.AbstractSyntaxTrees.IxExpr;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.LocalDecl;
import miniJava.AbstractSyntaxTrees.LocalDeclStmt;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodRefExpr;
import miniJava.AbstractSyntaxTrees.Modifier;
import miniJava.AbstractSyntaxTrees.ModifierList;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.PackageDecl;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.PrimitiveType;
import miniJava.AbstractSyntaxTrees.Protection;
import miniJava.AbstractSyntaxTrees.QualType;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.StringLiteral;
import miniJava.AbstractSyntaxTrees.SwitchStmt;
import miniJava.AbstractSyntaxTrees.SyscallStmt;
import miniJava.AbstractSyntaxTrees.TernaryExpr;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.ThrowStmt;
import miniJava.AbstractSyntaxTrees.TryCatchFinallyStmt;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.AbstractSyntaxTrees.MemberDecl.MemberType;
import miniJava.AbstractSyntaxTrees.SyscallStmt.SyscallType;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;


public class ScopedIdentifier implements Visitor<IdentificationContext,Object> {

    //NOTE: make sure to be careful with nested types; the miniJava guide doesn't allow
    //class defs within class defs. qualified types need to be parsed just as well as qualified refs
    public IDTable table;

    public static class IdentificationError extends CompilerError {
        public IdentificationError(String message,SourcePosition posn) {
            super(message + "; at position " + posn);
        }
    }

    public Scope openScope(boolean global){
        Scope newScope = new Scope(global); //references in global scopes can be overshadowed; those in local scopes cannot
        table.addLast(newScope);
        return newScope;
    }

    public Scope closeScope(){
        return table.removeLast();
    }

    public Scope currentScope(){
        return table.peekLast();
    }

    public void addDeclaration(Identifier name, Declaration decl){
        DeclarationHit s = findDeclaration(name,decl.isTypeMember());
        if (s != null){
            if (s.declScope.global && (s.declScope != table.peekLast()))
            {
                //local variable overriding global variable
            } else {
                throw new IdentificationError("local variable " + name.repr() + " already exists",name.posn);
            }
            
        }
        table.peekLast().put(name.spelling, decl);
    }

    

    private class DeclarationHit {
        public Declaration declaration;
        public Scope declScope;
        public int scopeLevel;

        public DeclarationHit(Declaration decl, Scope scope, int level){
            declaration = decl;
            declScope = scope;
            scopeLevel = level;
        }
    }

    public DeclarationHit findDeclaration(Identifier id, boolean allow_type){ //
        Declaration result = null;
        Iterator<Scope> it = table.descendingIterator();
        int level = table.size();
        while (it.hasNext()){
            level--;
            Scope scope = it.next();
            result = scope.get(id.spelling, allow_type);
            if (result != null){
                return new DeclarationHit(result, scope,level);
            }
        }
        return null;
    }

    public DeclarationHit findDeclarationThrow(Identifier id, boolean allow_type) throws IdentificationError {
        DeclarationHit res = findDeclaration(id, allow_type);
        if (res == null) throw new IdentificationError(id.repr() + " could not be resolved to a " + (allow_type ? "type " : "") + "declaration",id.posn);
        return res;
    }

    public void identifyProgram(AST prog){
        table = new IDTable();
        prog.visit(this, new IdentificationContext(null));
    }

    public void addBuiltins(Package prog){
        //add default imports
        //String class
        ClassDecl strDecl = 
        new ClassDecl(
            new DeclKeywords(), 
            new Identifier(new Token(TokenType.id,"String",null)), 
            null, 
            new ArrayList<FieldDecl>(), 
            new ArrayList<MethodDecl>(),
            new ArrayList<ConstructorDecl>(),
            new ArrayList<ClassMemberDecl>(), 
            null,
            new ArrayList<TypeDenoter>(),
                null);
        addDeclaration(strDecl.name, strDecl);
        
        //System.out.println
        //making: 
        //class _PrintStream 
        //  void println(int wow){
        //      SyscallStmt(type=PRINTLN);
        //    }
        //Class System
        //  static Out out = new _PrintStream();

        List<ParameterDecl> printParams = new ArrayList<>();
        printParams.add(new ParameterDecl(
            new DeclKeywords(),
            new PrimitiveType(TypeKind.INT, null), //TODO: non-minijava version? method overloading?
            Identifier.makeDummy("obj"),
            null, 
            null));


        List<Statement> printBody = new ArrayList<>();
        printBody.add(new SyscallStmt(SyscallType.PRINTLN,new IdRef(Identifier.makeDummy("obj")),null)); //syscall to print 'obj' to stdout
        MethodDecl printDecl = new MethodDecl(
            new DeclKeywords(),
            new ArrayList<GenericVar>(),
            new PrimitiveType(TypeKind.VOID, null), 
            Identifier.makeDummy("println"),
            printParams,
            printBody,
            new ArrayList<TypeDenoter>(),
            null
            );
            
        List<MethodDecl> methods = new ArrayList<>();
        methods.add(printDecl);
        ClassDecl streamDecl = new ClassDecl(
            new DeclKeywords(), 
            Identifier.makeDummy("_PrintStream"), 
            new ArrayList<>(), 
            new ArrayList<>(), 
            methods, new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), null);
        
        List<FieldDecl> fieldDecls = new ArrayList<>();
        Identifier printRef = Identifier.makeDummy("_PrintStream");
        IdentifierType printType = new IdentifierType(printRef);
        printRef.refDecl = streamDecl;
        DeclKeywords outKeywords = new DeclKeywords();
        outKeywords.modifiers.add(new Modifier(new Token(TokenType.modifier, "static", null))); //make out static
        fieldDecls.add(new FieldDecl(
            outKeywords, 
            printType, 
            Identifier.makeDummy("out"),        
            new NewObjectExpr(printType, new ExprList(), null), // out = new _PrintStream();
            null));
        ClassDecl sysDecl = new ClassDecl(
            new DeclKeywords(), 
            Identifier.makeDummy("System"),
            new ArrayList<>(), 
            fieldDecls, new ArrayList<>(),
            new ArrayList<>(), 
            new ArrayList<>(), 
            null, 
            new ArrayList<>(), 
            null);

        prog.classes.add(sysDecl);
        prog.classes.add(streamDecl);

    }



    @Override
    public Object visitPackage(Package prog, IdentificationContext arg) {
        openScope(true);
        addBuiltins(prog);

        //make sure the global types are identified to the right place
        TypeResult.STRING.getType().visit(this,arg);
        TypeResult.STRINGARR.getType().visit(this,arg);

        if (prog.header != null){
            prog.header.visit(this,arg); //add import declarations
        }
        
        //complete level 1: fill the Last level with *all* of the classdecls in the package
        for (ClassMemberDecl cmd : prog.classes){
            addDeclaration(cmd.name, cmd);
        }

        //visit classes
        for (ClassMemberDecl cmd : prog.classes){
            cmd.visit(this, arg);
        }
        closeScope();
        return null;
    }

    @Override
    public Object visitFileHeader(FileHeader fileHeader, IdentificationContext arg) {
        visitList(fileHeader.imports,arg);
        return null;
    }

    @Override
    public Object visitPackageDecl(PackageDecl packageDecl, IdentificationContext arg) {
        //TODO: implement file linking?
        throw new UnsupportedOperationException("Unimplemented method 'visitPackageDecl'");
    }

    @Override
    public Object visitImportStatement(ImportStatement is, IdentificationContext arg) {
        if (is.importAll){
            //file linking not supported so... do nothing I guess. Would need to go to the package and add every thing
        } else {
            Identifier importFinal = is.importRef.get(is.importRef.size()-1);
            //file linking not supported yet so I guess just add a dummy declaration?
            ClassDecl dummy = new ClassDecl(null, importFinal, null, null, null, null, null, null, null, null);
            addDeclaration(importFinal, dummy);
        }
        return null;
    }


    public void addClassMembers(Iterable<ClassMemberDecl> cmds){
        for (ClassMemberDecl cmd : cmds) addDeclaration(cmd.name, cmd);
    }

    public void addMethods(Iterable<MethodDecl> methods){
        for (MethodDecl md : methods) addDeclaration(md.name, md);
    }

    public void addConstructors(Iterable<ConstructorDecl> constructors){
        for (ConstructorDecl cdc : constructors) addDeclaration(cdc.name, cdc);
    }

    public void addGenericTypes(Iterable<GenericVar> generics){
        for (GenericVar gv : generics){
            addDeclaration(gv.name, gv);
        }
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, IdentificationContext arg) {
        cd.keywords.visit(this,arg);
        if (cd.superclass != null) cd.superclass.visit(this, arg);
        visitList(cd.superinterfaces,arg);

        cd.setEnclosingDecl(arg.thisDecl);
        ClassMemberDecl enclosingType = arg.thisDecl;
        openScope(true);
        arg.thisDecl = cd;

        //complete level: fill with fields, methods, classmembers, etc.
        addGenericTypes(cd.generics);
        addClassMembers(cd.classmembers);
        addConstructors(cd.constructors);
        addMethods(cd.methods);
        
        visitList(cd.fields,arg);

        visitList(cd.classmembers, arg);
        visitList(cd.constructors,arg);
        visitList(cd.methods, arg);

        closeScope();
        arg.thisDecl = enclosingType;
        
        return null;
    }

    @Override
    public Object visitEnumDecl(EnumDecl ed, IdentificationContext arg) {
        ed.keywords.visit(this,arg);
        visitList(ed.superinterfaces,arg);

        //complete level: fill with fields, methods, classmembers, etc.
        ed.setEnclosingDecl(arg.thisDecl);
        ClassMemberDecl enclosingType = arg.thisDecl;
        openScope(true);
        arg.thisDecl = ed;
        
        addClassMembers(ed.classmembers);
        addConstructors(ed.constructors);
        addMethods(ed.methods);
        
        visitList(ed.elements,arg); //enum elements work the same way as fields, are parsed and identified in order
        visitList(ed.fields,arg);

        visitList(ed.classmembers, arg);
        visitList(ed.constructors,arg);
        visitList(ed.methods, arg);

        closeScope();
        arg.thisDecl = enclosingType;
        
        return null;
    }

    @Override
    public Object visitInterfaceDecl(InterfaceDecl id, IdentificationContext arg) {
        id.keywords.visit(this,arg);
        if (id.superclass != null) id.superclass.visit(this, arg);
        visitList(id.superinterfaces,arg);
        
        id.setEnclosingDecl(arg.thisDecl);
        ClassMemberDecl enclosingType = arg.thisDecl;
        openScope(true);
        arg.thisDecl = id;

        //complete level: fill with fields, methods, classmembers, etc
        addGenericTypes(id.generics);
        addClassMembers(id.classmembers);
        addConstructors(id.constructors);
        addMethods(id.methods);
        
        visitList(id.fields,arg);

        visitList(id.classmembers, arg);
        visitList(id.constructors,arg);
        visitList(id.methods, arg);

        closeScope();
        arg.thisDecl = enclosingType;
        
        return null;
    }

    @Override
    public Object visitAnnotationDecl(AnnotationDecl ad, IdentificationContext arg) {
        ad.keywords.visit(this,arg);
        if (ad.superclass != null) ad.superclass.visit(this, arg);
        visitList(ad.superinterfaces,arg);
        ad.setEnclosingDecl(arg.thisDecl);
        ClassMemberDecl enclosingType = arg.thisDecl;
        openScope(true);
        arg.thisDecl = ad;

        //complete level: fill with fields, methods, classmembers, etc
        addGenericTypes(ad.generics);
        addClassMembers(ad.classmembers);
        addConstructors(ad.constructors);
        addMethods(ad.methods);
        
        visitList(ad.fields,arg);
        
        visitList(ad.classmembers, arg);
        visitList(ad.constructors,arg);
        visitList(ad.methods, arg);
        

        closeScope();
        arg.thisDecl = enclosingType;
        
        return null;
    }


    @Override
    public Object visitMethodDecl(MethodDecl md, IdentificationContext arg) {
        md.keywords.visit(this,arg);
        arg.isStatic = md.keywords.isStatic(); //since keywords are visited before every declaration, this **should** work...
        arg.staticLevel = table.currentLevel();
        md.type.visit(this, arg); //identify return type
        
        md.setEnclosingDecl(arg.thisDecl);
        openScope(false);
        if (md.genericTypes != null) addGenericTypes(md.genericTypes);
        visitList(md.parameters, arg);
        visitList(md.statementList,arg);
        closeScope();
        
        return null;
    }

    @Override
    public Object visitConstructorDecl(ConstructorDecl cd, IdentificationContext arg) {
        cd.keywords.visit(this,arg);
        cd.type.visit(this, arg);

        cd.setEnclosingDecl(arg.thisDecl);
        openScope(false);

        visitList(cd.parameters, arg);
        visitList(cd.statementList,arg);

        closeScope();
        
        return null;
    }
    @Override
    public Object visitEmptyMethodDecl(EmptyMethodDecl emd, IdentificationContext arg) {
        arg.isStatic = emd.keywords.isStatic();
        arg.staticLevel = table.currentLevel();
        emd.keywords.visit(this,arg);
        emd.setEnclosingDecl(arg.thisDecl);
        openScope(false);
        visitList(emd.parameters, arg);
        //no statements
        closeScope();
        
        return null;
    }
    
    @Override
    public Object visitFieldDecl(FieldDecl fd, IdentificationContext arg) {
        arg.isStatic = fd.keywords.isStatic();
        arg.staticLevel = table.currentLevel();
        fd.keywords.visit(this,arg);
        fd.type.visit(this,arg);
        fd.setEnclosingDecl(arg.thisDecl);
        if (fd.initializer != null)
            fd.initializer.visit(this, arg); // evaluate initializer before adding declarations
        addDeclaration(fd.name, fd);

        return null;
    }

    @Override
    public Object visitEnumElement(EnumElement enumElement, IdentificationContext arg) {
        enumElement.keywords.visit(this,arg);
        visitList(enumElement.args,arg);
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, IdentificationContext arg) {
        pd.keywords.visit(this,arg);
        pd.type.visit(this, arg);
        if (pd.initializer != null)
            pd.initializer.visit(this,arg);
        addDeclaration(pd.name, pd);
        return null;
    }

    @Override
    public Object visitLocalDecl(LocalDecl decl, IdentificationContext arg) {
        decl.keywords.visit(this,arg);
        decl.type.visit(this,arg);
        if (decl.initializer != null)
            arg.currentName = decl.name;
            decl.initializer.visit(this, arg);
        addDeclaration(decl.name, decl);
        arg.currentName = null;
        return null;
    }

    @Override
    public Object visitPrimitiveType(PrimitiveType type, IdentificationContext arg) {
        return null; //primitive type knows what it is, because it knows what it isn't
    }

    @Override
    public Object visitIdentifierType(IdentifierType type, IdentificationContext arg) {
        arg.allow_type = true;
        return type.className.visit(this,arg);
    }

    @Override
    public Object visitArrayType(ArrayType type, IdentificationContext arg) {
        return type.eltType.visit(this, arg);
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, IdentificationContext arg) {
        openScope(false);
        visitList(stmt.sl, arg);
        closeScope();
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, IdentificationContext arg) {
        stmt.val.visit(this, arg);
        stmt.expRef.visit(this, arg);
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, IdentificationContext arg) {
        if (stmt.returnExpr != null) // void check!
            return stmt.returnExpr.visit(this, arg);
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, IdentificationContext arg) {
        stmt.cond.visit(this, arg);
        if (stmt.thenStmt instanceof LocalDeclStmt){ //imo this is a silly check
            throw new IdentificationError("Cannot have a declaration on its own as if body", stmt.thenStmt.posn);
        }
        stmt.thenStmt.visit(this,arg);
        if (stmt.elseStmt != null) {
            if (stmt.elseStmt instanceof LocalDeclStmt){
                throw new IdentificationError("Cannot have a declaration on its own as else body", stmt.thenStmt.posn);
            }
            stmt.elseStmt.visit(this,arg);
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, IdentificationContext arg) {
        stmt.cond.visit(this,arg);
        if (stmt.body instanceof LocalDeclStmt){
            throw new IdentificationError("Cannot have a declaration on its own as while body", stmt.body.posn);
        }
        stmt.body.visit(this,arg);
        return null;
    }

    @Override
    public Object visitDoWhileStmt(DoWhileStmt stmt, IdentificationContext arg) {
        if (stmt.body instanceof LocalDeclStmt){
            throw new IdentificationError("Cannot have a declaration on its own as while body", stmt.body.posn);
        }
        stmt.body.visit(this,arg);
        stmt.cond.visit(this,arg);
        return null;
    }

    @Override
    public Object visitForEachStmt(ForEachStmt stmt, IdentificationContext arg) {
        stmt.iterExpression.visit(this,arg);
        openScope(false);
        stmt.iterDecl.visit(this,arg); //add local decl

        if (stmt.body instanceof LocalDeclStmt){
            throw new IdentificationError("Cannot have a declaration on its own as for body", stmt.body.posn);
        }
        stmt.body.visit(this,arg);
        closeScope();

        return null;
    }

    @Override
    public Object visitForStmt(ForStmt stmt, IdentificationContext arg) {
        openScope(false);
        stmt.initStmt.visit(this,arg);
        stmt.initStmt.visit(this,arg);
        stmt.incStmt.visit(this,arg);

        if (stmt.body instanceof LocalDeclStmt){
            throw new IdentificationError("Cannot have a declaration on its own as for body", stmt.body.posn);
        }
        stmt.body.visit(this,arg);
        closeScope();


        return null;
    }

    @Override
    public Object visitTryCatchFinallyStmt(TryCatchFinallyStmt stmt, IdentificationContext arg) {
        stmt.tryBlock.visit(this, arg);
        visitList(stmt.catchBlocks,arg);
        if (stmt.finallyBlock != null){
            stmt.finallyBlock.visit(this,arg);
        }
        return null;
    }

    @Override
    public Object visitCatchBlock(CatchBlock catchBlock, IdentificationContext arg) {
        openScope(false);
        catchBlock.exception.visit(this,arg);
        catchBlock.statement.visit(this,arg);
        closeScope();
        return null;
    }


    @Override
    public Object visitLocalDeclStmt(LocalDeclStmt localDeclStmt, IdentificationContext arg) {
        for (LocalDecl dec : localDeclStmt.decls){
            dec.visit(this,arg);
        }
        return null; //initialize and add declarations
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, IdentificationContext arg) {
        return expr.expr.visit(this, arg);
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, IdentificationContext arg) {
        expr.left.visit(this,arg);
        expr.right.visit(this,arg);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, IdentificationContext arg) {
        expr.exRef.visit(this, arg);
        expr.ixExpr.visit(this,arg);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, IdentificationContext arg) {
        //the scoped identifier as I have implemented it is incapable of doing qualified identification
        //this is because it *needs* type information to do so
        //the second pass will be done with the QualifiedIdentifier
        if (expr.baseExp != null) { //could be method on its own
            expr.baseExp.visit(this,arg); //now left should have been populated
        } else {
            arg.allow_type = false;
            expr.methodName.visit(this,arg); //since it's just a lone identifier, need to parse that
            if (!(expr.methodName.refDecl.isMember() && expr.methodName.refDecl.asMemberDecl().getMemberType() == MemberType.METHOD)){
                throw new IdentificationError("Cannot call non-method identifier " + expr.methodName.repr() + " with declaration " + expr.methodName.refDecl + " as method", expr.methodName.posn);
            }
        }
        visitList(expr.argList,arg);

        return null;
    }

    @Override
    public Object visitMethodRefExpr(MethodRefExpr methodRefExpr, IdentificationContext arg) {
        //Qualified expression, but can't do it now - need QualifiedIdentifier for that
        return methodRefExpr.exp.visit(this,arg);
    }

    @Override
    public Object visitQualType(QualType qualType, IdentificationContext arg) {
        //Qualified type, but can't do it now - need QualifiedIdentifier for that
        return qualType.baseType.visit(this,arg);
    }

    @Override
    public Object visitGenericType(GenericType genericType, IdentificationContext arg) {
        genericType.baseType.visit(this,arg);
        visitList(genericType.genericTypes,arg);
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, IdentificationContext arg) {
        return null; //literals don't need identification
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, IdentificationContext arg) {
        expr.type.visit(this, arg);
        visitList(expr.args,arg);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, IdentificationContext arg) {
        expr.eltType.visit(this,arg);
        visitList(expr.sizeExprs,arg);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, IdentificationContext arg) {
        if (arg.isStatic){
            throw new IdentificationError("keyword 'this' cannot be used in a static context", ref.posn);
        }
        ref.thisDecl = arg.thisDecl;
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, IdentificationContext arg) {
        arg.allow_type = false;
        ref.id.visit(this, arg);
        return null;
    }


    @Override
    public Object visitIdentifier(Identifier id, IdentificationContext arg) {
        DeclarationHit hit = findDeclarationThrow(id,arg.allow_type);
        if (arg.currentName != null && id.equals(arg.currentName)){
            //can't declare a variable using another of the same name
            throw new IdentificationError("Local variable " + id.spelling + " referenced before declaration", id.posn);
        }

        


        //currently in static, decl outside of current static body, decl not static AND decl not in level 0 (automatically "static")
        if (arg.isStatic  && hit.scopeLevel < arg.staticLevel && !hit.declaration.isStatic() && hit.scopeLevel != 0){ //TODO: this logic is wrong
            throw new IdentificationError("Referencing non-static variable " + id.repr() + " from static context", id.posn);
        }
        id.refDecl = (hit.declaration);
        return null;
    }

    @Override
    public Object visitOperator(Operator op, IdentificationContext arg) {
        throw new IdentificationError("Operators should not be visited during identification",op.posn);
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, IdentificationContext arg) {
        throw new IdentificationError("Literals should not be visited during identification",num.posn);
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, IdentificationContext arg) {
        throw new IdentificationError("Literals should not be visited during identification",bool.posn);
    }

    @Override
    public Object visitStringLiteral(StringLiteral stringLiteral, IdentificationContext arg) {
        throw new IdentificationError("Literals should not be visited during identification",stringLiteral.posn);
    }

    @Override
    public Object visitFloatLiteral(FloatLiteral floatLiteral, IdentificationContext arg) {
        throw new IdentificationError("Literals should not be visited during identification",floatLiteral.posn);
    }

    @Override
    public Object visitCharLiteral(CharLiteral charLiteral, IdentificationContext arg) {
        throw new IdentificationError("Literals should not be visited during identification",charLiteral.posn);
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, IdentificationContext arg) {
        throw new IdentificationError("Literals should not be visited during identification",nullLiteral.posn);
    }

    @Override
    public Object visitDeclKeywords(DeclKeywords declParams, IdentificationContext arg) {
        //modifiers and protection is find, but annotations are scopedid
        visitList(declParams.annotations,arg);
        return null;
    }

    @Override
    public Object visitEllipsisType(EllipsisType ellipsisType, IdentificationContext arg) {
        return ellipsisType.base.visit(this,arg);   
    }

    @Override
    public Object visitThrowStmt(ThrowStmt throwStmt, IdentificationContext arg) {
        return throwStmt.exp.visit(this,arg);
    }

    @Override
    public Object visitContinueStmt(ContinueStmt continueStmt, IdentificationContext arg) {
        return null;
    }

    @Override
    public Object visitDotExpr(DotExpr dotExpr, IdentificationContext arg) {
        //Qualified expression, but can't do it now - need QualifiedIdentifier for that
        return dotExpr.exp.visit(this,arg);
    }

    @Override
    public Object visitArrayLiteralExpr(ArrayLiteralExpr arrayLiteralExpr, IdentificationContext arg) {
        visitList(arrayLiteralExpr.elements,arg);
        return null;
    }

    @Override
    public Object visitIncExpr(IncExpr incExpr, IdentificationContext arg) {
        return incExpr.incExp.visit(this,arg);
    }

    @Override
    public Object visitTernaryExpr(TernaryExpr ternaryExpr, IdentificationContext arg) {
        ternaryExpr.conditional.visit(this,arg);
        ternaryExpr.value1.visit(this, arg);
        ternaryExpr.value2.visit(this, arg);
        return null;
    }

    @Override
    public Object visitExprStatement(ExprStmt exprStatement, IdentificationContext arg) {
        return exprStatement.baseExpr.visit(this,arg);
    }

    @Override
    public Object visitBreakStmt(BreakStmt breakStmt, IdentificationContext arg) {
        return null;
    }

    @Override
    public Object visitInitializer(Kwarg initializer, IdentificationContext arg) {
        //the declarations are added separately; this just parses the initalizing expression if it exists
        if (initializer.initExpression != null){
            return initializer.initExpression.visit(this,arg);
        }
        return null;
    }

    

    @Override
    public Object visitModifier(Modifier modifier, IdentificationContext arg) {
        return null; //modifiers are identifierless keywords
    }

    @Override
    public Object visitProtection(Protection protection, IdentificationContext arg) {
        return null; //protectionss are identifierless keywords
    }

    @Override
    public Object visitGenericVar(GenericVar genericVar, IdentificationContext arg) {
        throw new IdentificationError("Generic variables should not be visited during identification, just added as declarations", genericVar.posn);
    }

    @Override
    public Object visitSwitchStmt(SwitchStmt stmt, IdentificationContext arg) {
        openScope(false);
        stmt.target.visit(this,arg);
        stmt.firstCase.visit(this,arg); //visited on a linked-list basis
        closeScope();
        return null;
    }

    @Override
    public Object visitCaseBlock(CaseBlock caseBlock, IdentificationContext arg) {
        //interesting case: for enums, the caseblock literal is actually a qualified reference, so we need to cover this in QualifiedIdentifier
        visitList(caseBlock.caseBody,arg);
        if (caseBlock.nextBlock != null){
            caseBlock.nextBlock.visit(this,arg);
        }
        return null;
    }

    @Override
    public Object visitSyscallStmt(SyscallStmt syscallStmt, IdentificationContext arg) {
        //syscall-type-specific identification 
        switch (syscallStmt.type) {
            default:
                visitList(syscallStmt.args, arg);
                break;
        }
        return null;
    }

    

    

    
}
