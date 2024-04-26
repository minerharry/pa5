package miniJava.ContextualAnalysis;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Stack;

import miniJava.CompilerError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.ClassMemberDecl.ClassType;
import miniJava.AbstractSyntaxTrees.MemberDecl.MemberType;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;
import miniJava.Compiler;

public class TypeChecker implements Visitor<TypeContext,TypeResult> {

    public static class TypeError extends CompilerError{
        public TypeError(String message) {
            super(message);
        }}


    public void checkCompatible(TypeResult expected, TypeResult received, SourcePosition posn){
        if (received == null) return; //is fine
        if (!received.canBecome(expected)){
            throw (new TypeError("Type mismatch; Expected " + expected.repr() +" and received " + received.repr() + " at position " + posn));
        }
    }

    public void checkNumeric(TypeResult type, SourcePosition posn){
        TypeResult[] numerics = {TypeResult.FLOAT, TypeResult.DOUBLE, TypeResult.INT};
        for (TypeResult t : numerics){
            try {
                checkCompatible(t, type, posn);
                return;
            } catch (TypeError e){
                continue;
            }
        }
        throw new TypeError("Type mismatch; Type " + type.repr() + " is not numeric");
    }

    public ArrayType checkArrayType(TypeResult type, SourcePosition posn){
        if (!type.getType().isArrayType()){
            throw new TypeError("Type mismatch: Type " + type.repr() + " is not an array; at position " + posn);
        }
        return type.getType().asArrayType();
    }

    
    @Override
    public TypeResult visitPackage(Package prog, TypeContext arg) {
        visitList(prog.classes, arg);
        return null; //package has no type :))
    }

    @Override
    public TypeResult visitFileHeader(FileHeader fileHeader, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFileHeader'");
    }

    @Override
    public TypeResult visitPackageDecl(PackageDecl packageDecl, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitPackageDecl'");
    }

    @Override
    public TypeResult visitImportStatement(ImportStatement importStatement, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitImportStatement'");
    }

    @Override
    public TypeResult visitDeclKeywords(DeclKeywords declParams, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDeclKeywords'");
    }

    @Override
    public TypeResult visitModifier(Modifier modifier, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitModifier'");
    }

    @Override
    public TypeResult visitProtection(Protection protection, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitProtection'");
    }

    @Override
    public TypeResult visitGenericVar(GenericVar genericVar, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitGenericVar'");
    }

    public void checkMainMethod(ClassMemberDecl cmd){
        //find the main method if it exists
        for (MethodDecl md : cmd.methods){
            
            if (md.name.equals(Identifier.makeDummy("main")) &&
                    md.isStatic() &&
                    !md.isPrivate() &&
                    md.type.equals(TypeResult.VOID.getType()) &&
                    md.parameters.size() == 1 &&
                    md.parameters.get(0).type.equals(TypeResult.STRINGARR.getType())){
                if (mainMethod != null)
                {
                    throw new TypeError("Multiple main methods in one file!");
                }
                mainMethod = md;
            }
        }
    }

    @Override
    public TypeResult visitClassDecl(ClassDecl cd, TypeContext arg) {
        arg.addScope(cd);
        visitList(cd.classmembers,arg);
        visitList(cd.constructors, arg);
        visitList(cd.fields, arg);
        visitList(cd.generics, arg);
        visitList(cd.methods, arg);
        arg.popScope();
        checkMainMethod(cd);
        return null;
    }

    @Override
    public TypeResult visitAnnotationDecl(AnnotationDecl ad, TypeContext arg) {
        arg.addScope(ad);
        visitList(ad.classmembers,arg);
        visitList(ad.constructors, arg);
        visitList(ad.fields, arg);
        visitList(ad.generics, arg);
        visitList(ad.methods, arg);
        arg.popScope();
        checkMainMethod(ad);
        return null;
    }

    @Override
    public TypeResult visitEnumDecl(EnumDecl ed, TypeContext arg) {
        arg.addScope(ed);
        visitList(ed.classmembers,arg);
        visitList(ed.constructors, arg);
        visitList(ed.fields, arg);
        visitList(ed.generics, arg);
        visitList(ed.methods, arg);
        arg.popScope();
        checkMainMethod(ed);
        return null;
    }

    @Override
    public TypeResult visitInterfaceDecl(InterfaceDecl id, TypeContext arg) {
        arg.addScope(id);
        visitList(id.classmembers,arg);
        visitList(id.constructors, arg);
        visitList(id.fields, arg);
        visitList(id.generics, arg);
        visitList(id.methods, arg);
        arg.popScope();
        checkMainMethod(id);
        return null;
    }

    @Override
    public TypeResult visitEnumElement(EnumElement el, TypeContext arg) {
        //each enum element is a constructor call! use same logic as newobjectexpr, extract to method perhaps
        //TODO:
        return null;
    }

    @Override
    public TypeResult visitFieldDecl(FieldDecl fd, TypeContext arg) {
        TypeResult fieldType = fd.type.visit(this,arg);
        arg.memberKeywords = fd.keywords;
        if (fd.initializer != null)
            checkCompatible(fieldType,fd.initializer.visit(this,arg),fd.posn);
        arg.memberKeywords = null;
        return fieldType;
    }

    @Override
    public TypeResult visitConstructorDecl(ConstructorDecl cd, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitConstructorDecl'");
    }

    @Override
    public TypeResult visitMethodDecl(MethodDecl md, TypeContext arg) {
        TypeResult retType = md.type.visit(this,arg);
        List<TypeResult> throwTypes = visitList(md.throwsList, arg);
        visitList(md.parameters,arg); //initializer validation
        arg.returnType = retType;
        arg.throwTypes = throwTypes;
        arg.memberKeywords = md.keywords;
        visitList(md.statementList, arg);
        arg.returnType = null;
        arg.throwTypes = null;
        arg.memberKeywords = null;

        //Check last return statement; if missing, create if void and throw error if not
		if (md.statementList.size() == 0 || !(md.statementList.get(md.statementList.size()-1) instanceof ReturnStmt)){
			if (md.type.equals(TypeResult.VOID.getType())){ //create dummy return
				ReturnStmt ret = new ReturnStmt(null, null);
				md.statementList.add(ret);
			} else {
                throw new TypeError("Non-void method " + md.repr() + " must return object of type " + md.type.repr());
            }
		}
        return retType;
    }

    @Override
    public TypeResult visitEmptyMethodDecl(EmptyMethodDecl emptyMethodDecl, TypeContext arg) {
        visitList(emptyMethodDecl.parameters, arg); //we don't need the types, but we should evaluate them anyway (also default parameters)
        return emptyMethodDecl.type.visit(this,arg);
    }

    @Override
    public TypeResult visitParameterDecl(ParameterDecl pd, TypeContext arg) {
        TypeResult paramType = pd.type.visit(this,arg);
        if (pd.initializer != null)
            checkCompatible(paramType,pd.initializer.visit(this,arg),pd.posn);
        return paramType;
    }

    @Override
    public TypeResult visitLocalDecl(LocalDecl decl, TypeContext arg) {
        TypeResult varType = decl.type.visit(this,arg);
        if (decl.initializer != null)
            checkCompatible(varType,decl.initializer.visit(this,arg),decl.posn);
        return varType;
    }

    @Override
    public TypeResult visitInitializer(Kwarg initializer, TypeContext arg) {
        if (initializer.initExpression != null){
            return initializer.initExpression.visit(this,arg);
        }
        return null;
    }

    @Override
    public TypeResult visitPrimitiveType(PrimitiveType type, TypeContext arg) {
        return new TypeResult(type);
    }

    @Override
    public TypeResult visitIdentifierType(IdentifierType type, TypeContext arg) {
        try {
            type.typeDeclaration = type.className.refDecl.asMemberDecl().asClassLike(); //as populated by scoped identification
        } catch (InvalidCastError e){
            //type was not a class! bad
            throw new TypeError("IdentifierType " + type + " must be classlike to be a type, but " + type.className.repr() + " was " + type.className.refDecl + " instead");
        }
        return new TypeResult(type);
    }

    @Override
    public TypeResult visitArrayType(ArrayType type, TypeContext arg) {
        type.eltType.visit(this,arg);
        return new TypeResult(type);
    }

    @Override
    public TypeResult visitEllipsisType(EllipsisType ellipsisType, TypeContext arg) {
        ellipsisType.base.visit(this,arg);
        return new TypeResult(ellipsisType);
    }

    @Override
    public TypeResult visitQualType(QualType qualType, TypeContext arg) {
        //finally, qualified types
        qualType.baseType.visit(this,arg);
        
        //because qualified types have to come from a class type, we can assume that we just populated the typedeclaration
        ClassMemberDecl baseDecl = qualType.baseType.typeDeclaration;
        MemberDecl newDecl = baseDecl.findMemberThrow(qualType.id,true); //only look for subtypes in types!
        //check accessibility (just private for now)
        if (newDecl.asDeclaration().isPrivate()){
            if (!arg.within(baseDecl)){
                throw new TypeError("Cannot access private member " + qualType.id.repr() + " from outside class " + baseDecl);
            }
        }
        if (!(newDecl instanceof ClassMemberDecl)){
            throw new TypeError("Type " + qualType + " cannot be resolved to a type; got " + newDecl + " instead");
        }
        qualType.typeDeclaration = newDecl.asClassLike();
        return new TypeResult(qualType);
    }

    @Override
    public TypeResult visitGenericType(GenericType genericType, TypeContext arg) {
        genericType.baseType.visit(this,arg);
        //since generic types are really the same kind of thing as the underlying type, it's important the declaration be carried over
        genericType.typeDeclaration = genericType.baseType.typeDeclaration;
        //TODO: do I need to do anything weird with generic instance types?
        return new TypeResult(genericType);
    }

    @Override
    public TypeResult visitAssignStmt(AssignStmt stmt, TypeContext arg) {
        TypeResult varType = stmt.expRef.visit(this,arg);
        TypeResult expType = stmt.val.visit(this,arg);
        checkCompatible(varType, expType, stmt.posn);
        return varType;
    }

    @Override
    public TypeResult visitReturnStmt(ReturnStmt stmt, TypeContext arg) {
        if (stmt.returnExpr == null){ //void
            if (!arg.returnType.equals(TypeResult.VOID)){
                throw new TypeError("Non-void methods must return a value; at " + stmt.posn);
            }
            return TypeResult.VOID;
        }
        else {
            TypeResult rtype = stmt.returnExpr.visit(this,arg);
            checkCompatible(arg.returnType,rtype, stmt.posn);
            return rtype;
        }
    }

    @Override
    public TypeResult visitBreakStmt(BreakStmt breakStmt, TypeContext arg) {
        return null;
    }

    @Override
    public TypeResult visitLocalDeclStmt(LocalDeclStmt localDeclStmt, TypeContext arg) {
        TypeResult res = null;
        for (LocalDecl dec : localDeclStmt.decls){
            res = dec.visit(this,arg);
        }
        return res; //initialize and add declarations
    }

    @Override
    public TypeResult visitThrowStmt(ThrowStmt throwStmt, TypeContext arg) {
        TypeResult throwType = throwStmt.exp.visit(this,arg);
        if (!(arg.throwTypes.contains(throwType))){
            throw new TypeError("Error type " + throwType + " not declared in method signature");
        }
        return throwType;
    }

    @Override
    public TypeResult visitContinueStmt(ContinueStmt continueStmt, TypeContext arg) {
        return null;
    }

    @Override
    public TypeResult visitExprStatement(ExprStmt exprStatement, TypeContext arg) {
        return exprStatement.baseExpr.visit(this,arg);
    }

    @Override
    public TypeResult visitBlockStmt(BlockStmt stmt, TypeContext arg) {
        visitList(stmt.sl,arg);
        return null;
    }

    @Override
    public TypeResult visitIfStmt(IfStmt stmt, TypeContext arg) {
        checkCompatible(TypeResult.BOOLEAN,stmt.cond.visit(this,arg),stmt.posn);
        stmt.thenStmt.visit(this,arg);
        if (stmt.elseStmt != null){
            stmt.elseStmt.visit(this,arg);
        }
        return null;
    }

    @Override
    public TypeResult visitWhileStmt(WhileStmt stmt, TypeContext arg) {
        checkCompatible(TypeResult.BOOLEAN,stmt.cond.visit(this,arg),stmt.posn);
        stmt.body.visit(this,arg);
        return null;
    }

    @Override
    public TypeResult visitDoWhileStmt(DoWhileStmt stmt, TypeContext arg) {
        stmt.body.visit(this,arg);
        checkCompatible(TypeResult.BOOLEAN,stmt.cond.visit(this,arg),stmt.posn);
        return null;
    }

    @Override
    public TypeResult visitForStmt(ForStmt stmt, TypeContext arg) {
        stmt.initStmt.visit(this,arg);
        checkCompatible(TypeResult.BOOLEAN,stmt.compExp.visit(this,arg),stmt.posn);
        stmt.incStmt.visit(this, arg);

        stmt.body.visit(this,arg);
        return null;
    }

    @Override
    public TypeResult visitForEachStmt(ForEachStmt stmt, TypeContext arg) {
        checkCompatible(stmt.iterDecl.visit(this,arg), stmt.iterExpression.visit(this,arg),stmt.posn);
        stmt.body.visit(this,arg);
        return null;
    }

    @Override
    public TypeResult visitTryCatchFinallyStmt(TryCatchFinallyStmt stmt, TypeContext arg) {
        stmt.tryBlock.visit(this,arg);
        visitList(stmt.catchBlocks,arg);
        stmt.finallyBlock.visit(this,arg);
        return null;
    }

    @Override
    public TypeResult visitCatchBlock(CatchBlock block, TypeContext arg) {
        block.exception.visit(this,arg);
        block.statement.visit(this, arg);
        return null;
    }

    @Override
    public TypeResult visitSwitchStmt(SwitchStmt stmt, TypeContext arg) {
        TypeResult switchType = stmt.target.visit(this,arg);

        //TODO: Type validation on what kind of types can appear here?
        if (!(switchType.getType() instanceof PrimitiveType 
                || switchType.getType().typeDeclaration.getClassType() == ClassType.ENUM 
                || switchType.equals(TypeResult.STRING))){
            throw new TypeError("Switch targets must be primitive, enum, or String!");
        }

        //Remember: switch case statements use qualified types for enums! need to include the type in the typecontext
        //but since return / throw type still matters, need to make a copy
        TypeContext switchCtx = arg.clone();
        switchCtx.switchType = switchType;

        stmt.firstCase.visit(this,switchCtx);

        return switchType;
    }

    @Override
    public TypeResult visitCaseBlock(CaseBlock block, TypeContext arg) {
        
        TypeResult switchType = arg.switchType;
        if (switchType.getType().typeDeclaration.getClassType() == ClassType.ENUM){
            if (!(block.literal instanceof Identifier)){
                throw new TypeError("Invalid terminal " + block.literal + " in case block with enum target; use unqualified enum constant names");
            }
            //enum type, need to use identifier as qualified wrt classtype
            switchType = new QualType(switchType.getType(), block.literal.asIdentifier()).visit(this,arg); 
            //we don't really need the result, but visiting to ensure the enum constant is a member, for example
        } else {
            //parse like a regular type
            checkCompatible(switchType,block.literal.visit(this,arg),block.posn);
        }

        visitList(block.caseBody, arg);

        if (block.nextBlock != null){
            block.nextBlock.visit(this, arg);
        }

        return null;
    }

    @Override
    public TypeResult visitUnaryExpr(UnaryExpr expr, TypeContext arg) {
        TypeResult inner = expr.expr.visit(this,arg);
        switch (expr.operator.spelling){
            case "+":
            case "-":
                checkNumeric(inner,expr.posn);
                break;
            case "!":
                checkCompatible(TypeResult.BOOLEAN,inner,expr.posn);
                break;
            default:
                throw new UnsupportedOperationException("Unexpected unary operator: " + expr.operator);
        }
        return expr.setType(inner); //unary operators do not change type of expression
    }

    @Override
    public TypeResult visitIncExpr(IncExpr incExpr, TypeContext arg) {
        TypeResult inner = incExpr.incExp.visit(this,arg);
        checkNumeric(inner,incExpr.posn); //++, -- can work on float types; still just add 1 / sub 1
        return incExpr.setType(inner); //incrementing does not change numeric type
    }

    @Override
    public TypeResult visitBinaryExpr(BinaryExpr expr, TypeContext arg) {
        TypeResult v1 = expr.left.visit(this,arg);
        TypeResult v2 = expr.right.visit(this,arg);
        TypeResult res;
        //THIS IS THE HARD ONE
        switch (expr.operator.spelling){
            case "&&": //TODO: this list was accelerated for PA3, it is incomplete
            case "||":
                checkCompatible(TypeResult.BOOLEAN, v1,expr.posn);
                checkCompatible(TypeResult.BOOLEAN, v2,expr.posn);
                res = TypeResult.BOOLEAN;
                break;
            case "<":
            case "<=":
            case ">":
            case ">=":
                checkNumeric(v1,expr.posn);
                checkNumeric(v2,expr.posn);
                res = TypeResult.BOOLEAN;
                break;
            case "+":
            case "-":
                checkNumeric(v1,expr.posn);
                checkNumeric(v2,expr.posn);
                res = TypeResult.INT; //TODO WRONG
                break;
            case "*":
            case "/":
                checkNumeric(v1,expr.posn);
                checkNumeric(v2,expr.posn);
                res = TypeResult.INT; //TODO WRONG
                break;
            case "==":
            case "!=":
                //comparing objects
                try{
                    checkCompatible(v1, v2, expr.posn); //a instanceof b
                } catch (TypeError e){
                    try {
                        checkCompatible(v2, v1, expr.posn); //b instanceof a
                    } catch (TypeError f){
                        throw new TypeError("Incompatible operands: " + v1 + " and " + v2);
                    }
                } 
                res = TypeResult.BOOLEAN;
                break;
            default:
                throw new TypeError("Unexpected operator: " + expr.operator);
        }

        expr.returnType = res.getType();
        return res;

    }

    @Override
    public TypeResult visitIxExpr(IxExpr expr, TypeContext arg) {
        TypeResult arrType = expr.exRef.visit(this,arg);
        ArrayType t = checkArrayType(arrType,expr.posn); //must be array to do a[]
        TypeResult res = new TypeResult(t.eltType);
        
        TypeResult idxType = expr.ixExpr.visit(this,arg);
        checkCompatible(TypeResult.INT,idxType,expr.posn); //can only index with int

        expr.returnType = res.getType();
        return res; //a: int[] -> a[3]: int
    }


    // //finally, qualified types
    // qualType.baseType.visit(this,arg);
        
    // //because qualified types have to come from a class type, we can assume that we just populated the typedeclaration
    // ClassMemberDecl baseDecl = qualType.baseType.typeDeclaration;
    // MemberDecl newDecl = baseDecl.findMemberThrow(qualType.id);
    // if (!(newDecl instanceof ClassMemberDecl)){
    //     throw new TypeError("Type " + qualType + " cannot be resolved to a type; got " + newDecl + " instead");
    // }
    // qualType.typeDeclaration = newDecl.asClassLike();
    // return new TypeResult(qualType);

    @Override
    public TypeResult visitCallExpr(CallExpr expr, TypeContext arg) {
        expr.context = arg.scopes.peek();
        MemberDecl methDecl;
        if (expr.baseExp != null){
            //qualified method call
            TypeResult base = expr.baseExp.visit(this,arg);
            methDecl = base.getMember(expr.methodName);

            //qualified: check access!
            if (methDecl.asDeclaration().isPrivate()){
                if (!arg.within(methDecl)){
                    throw new TypeError("Cannot access private member " + expr.methodName.repr() + " from outside class " + base.repr());
                }
            }

        } else {
            Declaration dec = expr.methodName.refDecl;
            if (dec.isMember()){
                methDecl = dec.asMemberDecl();
            } else {
                throw new TypeError("Identifier " + expr.methodName + " is not callable");
            }
        }

        
        MethodDecl method;
        if (methDecl.getMemberType() == MemberType.METHOD){
            method = methDecl.asMethod();
        } else {
            throw new TypeError("Member " + expr.methodName.refDecl + " is not a method");
        }

        expr.methodName.refDecl = method;

        //verify parameters match!
        List<TypeResult> paramTypes = visitList(method.parameters,arg);
        List<TypeResult> argTypes = visitList(expr.argList,arg);

        if (argTypes.size() > paramTypes.size()){
            throw new TypeError("Too many arguments for method " + method + "; expected " + paramTypes.size() + ", got " + argTypes.size());
        }
        for (int i = 0; i < paramTypes.size(); i++){
            if (i >= argTypes.size()){
                if (method.parameters.get(i).isDefault()){
                    //default parameter, doesn't need argument
                    //realistically, this could be break, since all parameters after should be default,
                    // but making sure *every* parameter is default is good for my sanity and is easy enough
                    continue; 
                } else {
                    throw new TypeError("Missing non-default parameter " + method.parameters.get(i));
                }
            }
            checkCompatible(paramTypes.get(i),argTypes.get(i),expr.posn);
        }
        
        return expr.setType(method.type.visit(this,arg)); //this can potentially visit the same method's return type multiple time but like oh well
    }

    @Override
    public TypeResult visitLiteralExpr(LiteralExpr expr, TypeContext arg) {
        return expr.setType(expr.lit.visit(this,arg));
    }

    @Override
    public TypeResult visitNewObjectExpr(NewObjectExpr expr, TypeContext arg) {
        TypeResult objType = expr.type.visit(this,arg);
        if (objType.getType().typeDeclaration == null){
            throw new TypeError("Member " + expr.type + " is not an object and cannot be instantiated");
        }

        ClassMemberDecl decl;
        try {
            decl = expr.type.typeDeclaration.asClassLike();
        } catch (InvalidCastError e){
            throw new TypeError("Member " + expr.type + " is not an object and cannot be instantiated");
        }

        if (Compiler.IS_MINI){
            //only one default constructor
            if (expr.args.size() > 0){
                throw new TypeError("Constructors cannot take arguments in miniJava");
            }
            return expr.setType(objType);
        }
        for (ConstructorDecl c : decl.constructors){
            //pass
        }
        return null; //TODO: figure out method overloading (also applies to methods!)

    }

    @Override
    public TypeResult visitNewArrayExpr(NewArrayExpr expr, TypeContext arg) {
        TypeResult base = expr.eltType.visit(this,arg);
        TypeDenoter res = base.getType();
        for (Expression exp : expr.sizeExprs){
            checkCompatible(TypeResult.INT,exp.visit(this,arg),expr.posn);
            res = new ArrayType(res, null);
        }
        return expr.setType(new TypeResult(res));
    }
    

    @Override
    public TypeResult visitDotExpr(DotExpr dotExpr, TypeContext arg) {
        TypeResult base = dotExpr.exp.visit(this,arg);

        MemberDecl md = base.getMember(dotExpr.name); //if base is static, will only return the static members
        dotExpr.name.refDecl = md.asDeclaration();
        if (md.asDeclaration().isPrivate()){
            if (!arg.within(md)){
                throw new TypeError("Cannot access private member " + dotExpr.name.repr() + " from outside class " + base.repr());
            }
        }
        switch (md.getMemberType()){
            case FIELD:
                return dotExpr.setType(md.asField().type.visit(this, arg));
            case CLASSLIKE:
                // TypeDenoter type = new QualType(base.getType(), dotExpr.name); //I don't *think* it needs to be a qualtype??????
                // type.typeDeclaration = md.asClassLike();
                return dotExpr.setType(new TypeResult(StaticType.fromClass(md.asClassLike())));
            default:
                throw new TypeError("Cannot access member " + dotExpr.name.repr() + " of type " + base.repr());
        }
    }



    @Override
    public TypeResult visitMethodRefExpr(MethodRefExpr methodRefExpr, TypeContext arg) {
        // TODO: needs new custom type I think
        throw new TypeError("Unimplemented method 'visitMethodRefExpr'");
    }

    @Override
    public TypeResult visitArrayLiteralExpr(ArrayLiteralExpr arrayLiteralExpr, TypeContext arg) {
        // TODO: implement type hinting solves this problem
        throw new TypeError("Unimplemented method 'visitArrayLiteralExpr'");
    }


    @Override
    public TypeResult visitTernaryExpr(TernaryExpr ternaryExpr, TypeContext arg) {
        checkCompatible(TypeResult.BOOLEAN,ternaryExpr.conditional.visit(this,arg),ternaryExpr.posn);
        // TODO: the result can be one of multiple types... how?

        throw new UnsupportedOperationException("Unimplemented method 'visitTernaryExpr'");
    }

    @Override
    public TypeResult visitThisRef(ThisRef ref, TypeContext arg) {
        if (arg.memberKeywords.isStatic()){
            throw new TypeError("Cannot access this keyword: " + ref.repr() + " from a static context");
        }
        TypeDenoter thisType = IdentifierType.makeClassType(arg.scopes.peek());
        return new TypeResult(thisType);
    }

    @Override
    public TypeResult visitIdRef(IdRef ref, TypeContext arg) {
        //expression types must always be visited; the only nodes of the tree are types 
        
        //ok so static variables. Until we hit the static field/method, the types are actually classes,
        //so we need to account for that when passing them back up. 
        Declaration refDecl = ref.id.refDecl;
        ref.context = arg.scopes.peek();
        
        if (refDecl.isMember()){ //members are hard and annoying; locals are easy
            MemberDecl memDecl = refDecl.asMemberDecl();

            //static reference, essentially just a type for now. will become expression later (hopefully)
            if (memDecl.getMemberType() == MemberType.CLASSLIKE){
                return ref.setType(new TypeResult(StaticType.fromClass(memDecl.asClassLike())));
            }

            //this could have referred to a method, which is a type error (needs to be caught explicitly because its "type" is its return type)
            if (memDecl.getMemberType() == MemberType.METHOD){
                throw new TypeError("Cannot interpret method-referential identifier " + ref.id.repr() + " as variable");
            }

            //finally, check valid staticness
            boolean staticLevel = arg.memberKeywords.isStatic();
            boolean found = false;
            for (ClassMemberDecl source : arg.scopes){
                if (memDecl.enclosingDecl().equals(source)){
                    //if the route to the self is static, it can only access static members at that level
                    if (staticLevel && !memDecl.asDeclaration().isStatic()){
                        throw new TypeError("Cannot access instance member " + memDecl.asDeclaration().repr() + " from static context " + arg.scopes.peek());
                    }
                    //however, if the route to the self is instance, it can access any members.
                    found = true;
                    break;
                }
                //update whether the route to the self is static or not
                staticLevel = source.isStatic();
            }
            if (!found){
                throw new UnsupportedOperationException("Could not find enclosing class for member " + memDecl.asDeclaration().repr() + "; this is an error in identification");
            }
        }

        //but if the id is not itself a class
        return ref.setType(refDecl.type.visit(this,arg));
    }

    @Override
    public TypeResult visitIdentifier(Identifier id, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIdentifier'"); //should never visit an identifier!!
    }

    @Override
    public TypeResult visitOperator(Operator op, TypeContext arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOperator'");
    }

    @Override
    public TypeResult visitIntLiteral(IntLiteral num, TypeContext arg) {
        return TypeResult.INT;
    }

    @Override
    public TypeResult visitBooleanLiteral(BooleanLiteral bool, TypeContext arg) {
        return TypeResult.BOOLEAN;
    }

    @Override
    public TypeResult visitStringLiteral(StringLiteral stringLiteral, TypeContext arg) {
        return TypeResult.STRING;
    }

    @Override
    public TypeResult visitFloatLiteral(FloatLiteral floatLiteral, TypeContext arg) {
        return TypeResult.FLOAT;
    }

    @Override
    public TypeResult visitCharLiteral(CharLiteral charLiteral, TypeContext arg) {
        return TypeResult.CHAR;
    }

    @Override
    public TypeResult visitNullLiteral(NullLiteral nullLiteral, TypeContext arg) {
        return TypeResult.NULL;
    }

    public MethodDecl mainMethod;

    public TypeResult typeCheck(Package prog) {
        TypeContext arg = new TypeContext(null, null, new Stack<>());
        mainMethod = null;

        //we identified the global classes in scopedidentifier, but we still need to match the types
        TypeResult.STRING.getType().visit(this,arg);
        TypeResult.STRINGARR.getType().visit(this,arg);

        TypeResult res =  prog.visit(this, arg);
        prog.mainMethod = mainMethod;
        if (mainMethod == null){
            throw new TypeError("Main method not found!");
        }
        return res;
    }

    @Override
    public TypeResult visitSyscallStmt(SyscallStmt syscallStmt, TypeContext arg) {
        //syscall-type-specific identification 
        switch (syscallStmt.type) {
            default:
                visitList(syscallStmt.args, arg);
                break;
        }
        return null; //statements don't have a return type
    }
    
}
