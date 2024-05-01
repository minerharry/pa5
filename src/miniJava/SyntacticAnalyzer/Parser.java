package miniJava.SyntacticAnalyzer;

import java.lang.reflect.Member;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;


import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ClassMemberDecl.ClassType;
import miniJava.AbstractSyntaxTrees.MemberDecl.MemberType;
import miniJava.AbstractSyntaxTrees.TypeOrExpression.TypeClassification;
import miniJava.Compiler;
import miniJava.CompilerError;
import miniJava.ErrorReporter;

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}
	
	public static class SyntaxError extends CompilerError {
		public SyntaxError(String message, SourcePosition posn) {
			super(message + "; at " + posn);
		}

		public SyntaxError(String message, Token sourceT) {
			super(message + "; Token `" + sourceT.getTokenText() + "` at " + sourceT.getTokenPosition());
		}

		// private static final long serialVersionUID = -6461942006097999362L;
	}
	
	public Package parse() {
		try {
			// The first thing we need to parse is the Program symbol
			return parsePackage();
		} catch( SyntaxError e ) { 
			this._errors.reportError(e);
			return null;
		}
	}
	
	// Program ::= (ClassDeclaration)* eot
	private Package parsePackage() throws SyntaxError {
		SourcePosition startpos = _currentToken.getTokenPosition();
		FileHeader head = null;
		if (!Compiler.IS_MINI){
			head = parseHeader();
		}
		List<ClassMemberDecl> l = new ArrayList<ClassMemberDecl>();
		
		while (acceptOptional(TokenType.EOT) == null){
			l.add(parseGlobalDeclaration());
		}
		return new Package(head,l,startpos);
	}

	private FileHeader parseHeader() throws SyntaxError {
		
		//parse package statement
		Token packageToken = accept(TokenType.packageKeyword);
		ArrayList<Identifier> packIds = new ArrayList<>();
		packIds.add(new Identifier(accept(TokenType.id)));
		while (acceptOptional(TokenType.dot) != null){
			packIds.add(new Identifier(accept(TokenType.id)));
		}
		PackageReference pack = new PackageReference(packIds,packageToken.getTokenPosition());
		
		accept(TokenType.semicolon);
		
		ArrayList<ImportStatement> imports = new ArrayList<>();
		//parse import statements
		Token importToken;
		while ((importToken = acceptOptional(TokenType.importKeyword)) != null){
			List<Identifier> ids = new ArrayList<Identifier>();
			ids.add(new Identifier(accept(TokenType.id)));
			boolean isStar = false;
			while (acceptOptional(TokenType.dot) != null){
				Token star = acceptOptional(TokenType.binOp);
				if (star != null && star.getTokenText().equals("*")){
					isStar = true;

					break;
				} else if (star != null){
					throw err(new SyntaxError("Unexpected token: " + star,star));
				}
				ids.add(new Identifier(accept(TokenType.id)));
			}
			if (isStar){
				throw new SyntaxError("Star syntax not allowed in my java it's very annoying", importToken);
			}

			imports.add(new ImportStatement(ids, importToken.getTokenPosition()));
			accept(TokenType.semicolon);
		}
		return new FileHeader(pack, imports);
	}

	public enum MemberScope {
		GLOBAL,
		CLASSLIKE, //further specified by a ClassType into class, interface, etc
		LOCAL,
		PARAMETER,
		TYPEPARAMETER;
	}
	
	public class ClassContext {
		public MemberScope outerscope;
		public ClassMemberDecl.ClassType ct = null;
		public String classname;
		public boolean isAbstract = false;
		public ClassContext(ClassMemberDecl.ClassType specifier, String classname, boolean isAbstract){
			outerscope = MemberScope.CLASSLIKE;
			ct = specifier;
			this.classname = classname;
			this.isAbstract = isAbstract;
		}

		public ClassContext(MemberScope scope){
			outerscope = scope;
			ct = null;
			this.classname = null;
		}

	}

	private class InterfaceAnnotationFound extends Throwable {
		public DeclKeywords keywords;
		public Token atsign;
		public Token interfaceId;
		public InterfaceAnnotationFound(DeclKeywords keywords, Token atsign, Token interfaceId){
			this.keywords = keywords;
			this.atsign = atsign;
			this.interfaceId = interfaceId;

		}
	}

	private DeclKeywords parseDeclarationKeywords() throws SyntaxError, InterfaceAnnotationFound {
		SourcePosition startpos = null;
		Protection protection = null;
		ModifierList modifiers = new ModifierList();
		List<Annotation> annotations = new ArrayList<Annotation>();
		while (true) {
			Token ptoken = acceptOptional(TokenType.protection);
			if (ptoken != null){
				if (protection != null){
					throw err(new SyntaxError("Cannot have more than one protection level; " + protection, ptoken));
				}
				protection = new Protection(ptoken);
				if (startpos == null) {
					startpos = ptoken.getTokenPosition();
				}
				continue;
			}
			Token mtoken = acceptOptional(TokenType.modifier);
			if (mtoken != null){
				if (startpos == null){
					startpos = mtoken.getTokenPosition();
				}
				Modifier mod = new Modifier(mtoken);
				modifiers.add(mod);
				continue;
			}
			Token atoken = acceptOptional(TokenType.atsign);
			if (atoken != null){
				Token itoken;
				if ((itoken = acceptOptional(TokenType.interfaceKeyword)) != null){ //@interface annotation, interface declaration
					throw new InterfaceAnnotationFound(
						new DeclKeywords(protection, modifiers, annotations, startpos), atoken, itoken);
				}
				Annotation ann = parseAnnotation(atoken);
				if (startpos == null){
					startpos = ann.posn;
				}
				annotations.add(ann);
				continue;
			}
			break;
		}
		
		return new DeclKeywords(protection, modifiers, annotations, startpos);
	}
	
	private Annotation parseAnnotation(Token atsign) { //NOTE
		SourcePosition posn = atsign.getTokenPosition();
		TypeDenoter base = parseType();
		if (acceptOptional(TokenType.lparen) != null){
			KwargList il = new KwargList();
			while (acceptOptional(TokenType.rparen) == null){
				Identifier name = new Identifier(accept(TokenType.id));
				accept(TokenType.assignment);
				Expression init = parseExpression();
				il.add(new Kwarg(name, init, posn));
			}
			return new Annotation(base,il,posn);
		}
		return new Annotation(base, posn);
	}

	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private ClassMemberDecl parseGlobalDeclaration() throws SyntaxError {
		// TODO: Take in a "class" token (check by the TokenType)
		//  What should be done if the first token isn't "class"?
		
		ClassMemberDecl.ClassType ct = null;

		DeclKeywords keywords;
		SourcePosition startpos;
		try { 
			keywords = parseDeclarationKeywords();
			startpos = keywords.posn != null ? keywords.posn : null;
		} catch (InterfaceAnnotationFound i){
			keywords = i.keywords;
			ct = ClassMemberDecl.ClassType.ANNOTATION;
			startpos = keywords.posn != null ? keywords.posn 
				: i.atsign.getTokenPosition();
		}

		if (ct == null){
			Token classToken = acceptAny(TokenType.classKeyword,TokenType.interfaceKeyword,TokenType.enumKeyword);
			if (startpos == null){
				startpos = classToken.getTokenPosition();
			}
			switch (classToken.getTokenType()) {
				case classKeyword:
					ct = ClassMemberDecl.ClassType.CLASS;
					break;
				case enumKeyword:
					ct = ClassMemberDecl.ClassType.ENUM;
					break;
				case interfaceKeyword:
					ct = ClassMemberDecl.ClassType.INTERFACE;
					break;
				default:
					throw new IllegalStateException("Unexpected class token: " + classToken);
			}
		}
		
		ClassMemberDecl res = parseClassLike(MemberScope.GLOBAL,startpos,keywords,ct);
		keywords.validate(new ClassContext(MemberScope.GLOBAL), res.asDeclaration());
		return res;
	}


	private EnumElement parseEnumElement(ClassContext ctx, EnumType parent){
		DeclKeywords keywords = parseDeclarationKeywordsThrow();
		Identifier name = new Identifier(accept(TokenType.id));
		if (acceptOptional(TokenType.lparen) == null){ //just a name
			return keywords.validate(ctx, new EnumElement(keywords,parent,name));
		} else { //params
			ExprList args = new ExprList();
			while (acceptOptional(TokenType.rparen) == null) {  //no lparen
				args.add(parseExpression());
				if (acceptOptional(TokenType.comma) == null) { //no comma
					accept(TokenType.rparen);
					break;
				}
			}
			return keywords.validate(ctx,new EnumElement(keywords,parent, name, args));
		}
	}

	//parses everything **after** the class keyword token, e.g. [public class] A {} or [static @Annotated enum] stuff
	private ClassMemberDecl parseClassLike(MemberScope scope, SourcePosition startpos, DeclKeywords keywords, ClassType ct) {
		Token classNameToken = accept(TokenType.id);
		List<GenericVar> generics = new ArrayList<GenericVar>();
		Token chevy;
		if ((chevy = acceptOptional(TokenType.lchevron)) != null){ //class generics
			if (ct == ClassType.ENUM || ct == ClassType.ANNOTATION){
				throw err(new SyntaxError("Type parameters not allowed for classtype " + ct, chevy));
			}
			do {
				generics.add(parseGenericVar());
			} while (acceptOptional(TokenType.comma) != null);
			accept(TokenType.rchevron);
		}

		ClassContext ctx = new ClassContext(ct,classNameToken.getTokenText(),keywords.modifiers.containsType(ModifierType.Abstract));
		TypeDenoter superclass = null;
		boolean extend_as_implement = false; //this is so stupid; they moved implements to extends with interfaces but still allow multiple
		//I'm just gonna have the code jump to the implements-parsing code and leave it at that

		Token ex;
		if (!Compiler.IS_MINI && ((ex = acceptOptional(TokenType.extendsKeyword)) != null)){
			if (ct == ClassMemberDecl.ClassType.ENUM){
				throw err(new SyntaxError("Enums cannot have superclasses",ex));
			}
			if (ct == ClassMemberDecl.ClassType.INTERFACE) extend_as_implement = true;
			else superclass = parseType(false);
		}
		
		List<TypeDenoter> interfaces = new ArrayList<TypeDenoter>();
		Token imp = null;
		if (!Compiler.IS_MINI && (extend_as_implement || ((imp = acceptOptional(TokenType.implementsKeyword)) != null))){
			if (ct == ClassMemberDecl.ClassType.INTERFACE && !extend_as_implement){
				throw err(new SyntaxError("Implements keyword not allowed for interfaces, use extends to define a superinterface",imp));
			}
			interfaces.add(parseType());
			while (acceptOptional(TokenType.comma) != null){
				interfaces.add(parseType());
			}
		}
		accept(TokenType.lcurly);

		List<EnumElement> els = new ArrayList<EnumElement>();


		Identifier cn = new Identifier(classNameToken);

		//parse enum elements, potentially end early if no members
		EnumType etype = new EnumType(null, startpos); //NOTE: I really don't know if this is necessary but it's easy enough to implement
		if (ct == ClassType.ENUM){
			while (acceptOptional(TokenType.semicolon) == null){
				els.add(parseEnumElement(ctx,etype));
				if (acceptOptional(TokenType.comma) == null){
					if (acceptOptional(TokenType.rcurly) != null){ //enum ends with no members;
						EnumDecl decl = new EnumDecl(keywords, cn, els, interfaces, startpos);
						etype.parent = decl;
						return keywords.validate(ctx, decl);

					}
				}
			}
		}

		List<FieldDecl> fdl = new ArrayList<FieldDecl>();
		List<MethodDecl> mdl = new ArrayList<MethodDecl>();
		List<ConstructorDecl> cdl = new ArrayList<ConstructorDecl>();
		List<ClassMemberDecl> cmdl = new ArrayList<ClassMemberDecl>();

		while (acceptOptional(TokenType.rcurly) == null){
			List<MemberDecl> mds = parseMember(ctx);
			for (MemberDecl md : mds){
				switch (md.getMemberType()){
					case FIELD:
						fdl.add(md.asField());
						break;
					case METHOD:
						mdl.add(md.asMethod());
						break;
					case CONSTRUCTOR:
						cdl.add(md.asConstuctor());
						break;
					case CLASSLIKE:
						cmdl.add(md.asClassLike());
						break;
					case ENUM:
						throw new SyntaxError("Cannot have enum element outside of enum body",md.asDeclaration().posn);
				}
			}
		}
		
		switch (ct){
			case CLASS:
				return keywords.validate(ctx, new ClassDecl(keywords, cn, generics, fdl, mdl, cdl, cmdl, superclass, interfaces, startpos));
			case INTERFACE:
				return keywords.validate(ctx, new InterfaceDecl(keywords, cn, generics,fdl, mdl, cdl, cmdl, interfaces, startpos));
			case ENUM:
				EnumDecl decl = new EnumDecl(keywords, cn, els, fdl, mdl, cdl, cmdl, interfaces, startpos);
				etype.parent = decl;
				return keywords.validate(ctx, decl);
			case ANNOTATION:
				return keywords.validate(ctx, new AnnotationDecl(keywords, cn, fdl, mdl, cdl, cmdl, superclass, interfaces, startpos));
			default:
				throw new UnsupportedOperationException("Unexpected ClassType: " + ct);
		}
	}


	// [protection] [modifier*] Type Identifier (FieldBody | MethodBody)
	private List<MemberDecl> parseMember(ClassContext cctx) throws SyntaxError {
		
		List<MemberDecl> members = new ArrayList<MemberDecl>(); //we need to return a list because of vardecls with multi-initializer syntax
		//type
		MemberType type = null; //null can be any
		TypeDenoter memberType = null;
		

		ClassType ct = null;
		DeclKeywords keywords;
		//protection, modifier
		try{
			keywords = parseDeclarationKeywords();
		} catch (InterfaceAnnotationFound i){
			keywords = i.keywords;
			ct = ClassType.ANNOTATION;
		}

		Token classToken;
		if ((classToken = acceptAnyOptional(TokenType.classKeyword,TokenType.enumKeyword,TokenType.interfaceKeyword)) != null){
			switch (classToken.getTokenType()) {
				case classKeyword:
					ct = ClassType.CLASS;
					break;
				case enumKeyword:
					ct = ClassType.ENUM;
					break;
				case interfaceKeyword:
					ct = ClassType.INTERFACE;
					break;
				default:
					throw new IllegalStateException("Unexpected token type " + classToken.getTokenType() + " of token " + classToken);
			}
		}

		if (ct != null){ //member is a classmemberdecl, parse
			SourcePosition startpos = (keywords != null ? keywords.posn : classToken.getTokenPosition());
			members.add(parseClassLike(cctx.outerscope, startpos, keywords, ct));
			return members;
		}

		List<GenericVar> generics = null; 
		Token chevy = null;
		if ((chevy = acceptOptional(TokenType.lchevron)) != null){ //generic methods
			type = MemberType.METHOD;
			//NOTE: don't need to worry about empty generics, inferred generics not allowed for methods
			generics = new ArrayList<GenericVar>();
			do {
				generics.add(parseGenericVar());
			} while (acceptOptional(TokenType.comma) != null);
			accept(TokenType.rchevron);
		}
		
		Token voidt = null;
		if ((voidt = acceptOptional(TokenType.voidKeyword)) != null){
			memberType = new PrimitiveType(TypeKind.VOID,voidt.getTokenPosition());
			type = MemberType.METHOD;
		} else {
			memberType = parseType();
		}

		


		SourcePosition startpos = (keywords != null ? keywords.posn : memberType.posn);
		Identifier methodName;

		Token p;
		if ((p = acceptOptional(TokenType.lparen)) != null) { //that type we parsed was actually a constructor name
			if (type == MemberType.METHOD){
				if (chevy != null) throw err(new SyntaxError("Generic parameters not allowed in constructor declarations",chevy));
				else throw err(new SyntaxError("Method missing name", p));
			}
			if (Compiler.IS_MINI){
				throw err(new SyntaxError("Classes are a lie in miniJava - there are no constructors??",memberType.posn));
			}
			//note: we need an additional check to ensure that the constructor name matches the class name, but that will have to happen during identification
			if (!(memberType.isIdentifier())){ //just asking if it's a raw name, irrelevant to whether class/interface/etc
				throw err(new SyntaxError("Invalid constructor name: " + memberType, memberType.posn));
			}
			methodName = (memberType.asIdentifier());
			if (!methodName.equals(cctx.classname)){
				throw err(new SyntaxError("Method missing return type, or constructor name " + methodName + " does not match classname " + cctx.classname,memberType.posn));
			}
			if (cctx.ct == ClassType.INTERFACE) {
				throw err(new SyntaxError("Interfaces cannot have constructors",memberType.posn));
			}
			memberType = null; //return type null marks this as a constructor for later
		} else {
		
			//attempt to parse variables (with potential initialization)

			KwargList fields = new KwargList();
			while (true) {
				Token name = accept(TokenType.id);
				methodName = new Identifier(name);
				Kwarg init;
				Token assign;
				if ((assign = acceptOptional(TokenType.assignment)) != null){
					if (Compiler.IS_MINI){
						throw err(new SyntaxError("Assignment on field declaration not allowed in miniJava",assign));
					}
					if (type == MemberType.METHOD){
						throw err(new SyntaxError("Assignment token '=' not allowed here",assign));
					}
					type = MemberType.FIELD;
					Expression exp = parseExpression(true);
					init = new Kwarg(new Identifier(name), exp, name.getTokenPosition());
				} else {
					init = new Kwarg(name);
				}

				fields.add(init);

				Token com;
				if (acceptOptional(TokenType.semicolon) != null){ //[keywords] Type name; must be a field
					if (type == MemberType.METHOD){
						if (voidt != null) {
							throw err(new SyntaxError("void keyword not allowed in field declaration",voidt));
						} else {
							throw err(new SyntaxError("Generic parameters not allowed in field declaration",voidt));
						}
					}
					type = MemberType.FIELD; //complete field declaration
					for (Kwarg field : fields){ //make a fielddecl for *each* initializer
						members.add(new FieldDecl(keywords, memberType, field.name, field.initExpression, startpos));
					}
					break;
				} else if ((com = acceptOptional(TokenType.comma)) != null){
					if (Compiler.IS_MINI){
						throw err(new SyntaxError("simultaneous declarations not allowed in miniJava",com));
					}
					//multiple declarations
					continue;
				} else {
					//gotta be a method
					if (type == MemberType.FIELD) {
						throw err(new SyntaxError("Unexpected token in field declaration: " + _currentToken,_currentToken));
					}
					accept(TokenType.lparen);
					type = MemberType.METHOD;
					break;
				}
			}
		}
		if (members.size() == 0){
			//not found field; must be method, constructor, or classlike
			List<ParameterDecl> params = new ArrayList<ParameterDecl>();
			if (acceptOptional(TokenType.rparen) == null)
			{
				boolean defaulted = false;
				do {
					DeclKeywords paramKeywords;
					paramKeywords = parseDeclarationKeywordsThrow();
					TypeDenoter t = parseType();
					if (!Compiler.IS_MINI) {
						if (acceptOptional(TokenType.ellipsis) != null){
							t = new EllipsisType(t);
						}
						
					}
					Token name = accept(TokenType.id);
					Kwarg init;
					if (acceptOptional(TokenType.assignment) != null){
						Expression exp = parseExpression(true); //initialization statement
						init = new Kwarg(new Identifier(name),exp,name.getTokenPosition());
						defaulted = true;
					} else {
						if (defaulted){
							throw err(new SyntaxError("Required parameter not allowed after default parameter",name));
						}
						init = new Kwarg(name);
					}
					params.add(paramKeywords.validate(new ClassContext(MemberScope.PARAMETER),new ParameterDecl(paramKeywords, t, init.name, init.initExpression, t.posn)));
				} while (acceptOptional(TokenType.comma) != null);
				accept(TokenType.rparen);

			}
			List<TypeDenoter> ExceptionTypes = new ArrayList<TypeDenoter>();
			Token throwing;
			if ((throwing = acceptOptional(TokenType.throwsKeyword)) != null){
				if (type == MemberType.FIELD){
					throw err(new SyntaxError("Keyword 'throws' is not allowed in field declaration",throwing));
				}
				if (Compiler.IS_MINI){
					throw err(new SyntaxError("Invalid keyword \"throws\" in MiniJava",throwing));
				}
				type = MemberType.METHOD;
				ExceptionTypes.add(parseType(false)); //method throws exceptions
				while (true){
					if (acceptOptional(TokenType.comma) != null){
						ExceptionTypes.add(parseType(false));
					} else {
						break;
					}
				}
			}	

			if (acceptOptional(TokenType.semicolon) != null){ //empty
				members.add(new EmptyMethodDecl(keywords, generics, memberType, methodName, params, ExceptionTypes, startpos));
			} else {
				List<Statement> statements = parseBlockStatement().sl;
				if (memberType == null){
					members.add(new ConstructorDecl(keywords, methodName, params, statements, ExceptionTypes, startpos));
				} else {
					members.add(new MethodDecl(keywords, generics, memberType, methodName, params, statements, ExceptionTypes, startpos));
				}
			}
		}
		for (MemberDecl mem : members){
			keywords.validate(cctx,mem.asDeclaration());
		}
		return members;
	}


	private GenericVar parseGenericVar() {
		DeclKeywords keywords = parseDeclarationKeywordsThrow();
		Identifier name = new Identifier(accept(TokenType.id));
		List<TypeDenoter> supers = new ArrayList<TypeDenoter>();
		if (acceptOptional(TokenType.extendsKeyword) != null){
			do {
				supers.add(parseType());
			} while (acceptAnySpec(new TokenSpec(TokenType.binOp,"&")) != null);
		}
		ClassContext ctx = new ClassContext(MemberScope.TYPEPARAMETER);
		return keywords.validate(ctx, new GenericVar(keywords, name, supers, name.posn));
	}

	private BlockStmt parseBlockStatement(){
		return parseBlockStatement(null);
	}

	private BlockStmt parseBlockStatement(Token lcurly){
		if (lcurly == null){
			lcurly = accept(TokenType.lcurly);
		}
		List<Statement> statements = new ArrayList<Statement>();
		while (acceptOptional(TokenType.rcurly) == null){
			statements.add(parseStatement()); //foreach does not nest
		}
		return new BlockStmt(statements,lcurly.getTokenPosition());
	}

	private Statement parseStatement(){
		return parseStatement(false,true,false);
	}

	//block = require {Statement*}
	//foreach = allow Type id : reference;  if foreach, returns whether the statement is a valid foreach statement
	private Statement parseStatement(boolean block, boolean allow_keyword, boolean foreach){
		Token lcurly = null;
		if (block || ( allow_keyword && (lcurly = acceptOptional(TokenType.lcurly)) != null)){
			return parseBlockStatement(lcurly);
		}
		Token t = _currentToken;
		if (allow_keyword && acceptOptional(TokenType.returnKeyword) != null){
			SourcePosition retStart = t.getTokenPosition();
			if (acceptOptional(TokenType.semicolon) != null){
				return new ReturnStmt(null, retStart);
			} else {
				Expression exp = parseExpression();
				accept(TokenType.semicolon);
				return new ReturnStmt(exp, retStart);
			}
		} else if (allow_keyword && !Compiler.IS_MINI && (acceptOptional(TokenType.throwKeyword) != null)){
			Expression exp = parseExpression();
			accept(TokenType.semicolon);
			return new ThrowStmt(exp,t.getTokenPosition());
		} else if (allow_keyword && !Compiler.IS_MINI && (acceptAnyOptional(TokenType.breakKeyword,TokenType.continueKeyword) != null)){
			accept(TokenType.semicolon);
			if (t.getTokenType() == TokenType.breakKeyword){
				return new BreakStmt(t.getTokenPosition());
			} else {
				return new ContinueStmt(t.getTokenPosition());
			}
		} else if (allow_keyword && !Compiler.IS_MINI && (acceptOptional(TokenType.tryKeyword) != null)) {

			BlockStmt tryBody = parseBlockStatement();
			List<CatchBlock> catchBlocks = new ArrayList<>();
			BlockStmt finallyBody = null;
			Token tryKey = t;
			Token finallyKey = null;
			// if (true){
			// 	throw new SyntaxError("implement catch types!!!");
			// }
			Token catchKey = acceptOptional(TokenType.catchKeyword);
			TypeDenoter catchType = null;
			if (catchKey != null){
				do {
					accept(TokenType.lparen);
					DeclKeywords paramKeys = parseDeclarationKeywordsThrow();
					catchType = parseType(false);
					Token catchName = accept(TokenType.id);
					accept(TokenType.rparen);
					BlockStmt catchBody = parseBlockStatement();
					catchBlocks.add(
						new CatchBlock(
							paramKeys.validate(new ClassContext(MemberScope.PARAMETER),
								new ParameterDecl(
									paramKeys,
									catchType, 
									new Identifier(catchName), 
									null,
									catchType.posn)), 
								catchBody, 
								catchKey.getTokenPosition()));
				} while ((catchKey = acceptOptional(TokenType.catchKeyword)) != null);
				finallyKey = acceptOptional(TokenType.finallyKeyword);
			} else {
				finallyKey = accept(TokenType.finallyKeyword);
			}
			if (finallyKey != null){
				finallyBody = parseBlockStatement();
			}

			return new TryCatchFinallyStmt(tryBody,catchBlocks,finallyBody,tryKey.getTokenPosition());

		} else if (allow_keyword && (acceptOptional(TokenType.ifKeyword) != null)){
			accept(TokenType.lparen);
			Expression ifCond = parseExpression();
			accept(TokenType.rparen);
			Statement ifStmt = parseStatement();
			if (acceptOptional(TokenType.elseKeyword) != null){
				return new IfStmt(ifCond,ifStmt,parseStatement(),t.getTokenPosition());
			}
			return new IfStmt(ifCond, ifStmt, t.getTokenPosition());
		} else if (allow_keyword && (acceptOptional(TokenType.forKeyword) != null)){
			//not doing for-each, interacts strangely with parsestatement
			accept(TokenType.lparen);
			Statement s1 = parseStatement(false,false,true);
			if (s1 instanceof ForEachDummy){
				accept(TokenType.rparen);
				Statement body = parseStatement();
				return new ForEachStmt(s1.asForEachDummy(), body, t.getTokenPosition());
			} else { //returning true means parsed foreach statement
				//don't need to parse a semicolon, included in statement
				Expression s2 = parseExpression();
				accept(TokenType.semicolon);
				Expression e3 = parseExpression();
				if (!e3.isStateable()){
					throw err(new SyntaxError("For each updater expression must be stateable on its own", e3.posn));
				}
				Statement s3 = new ExprStmt(e3,e3.posn);
				//don't need to parse a semicolon, included in statement
				accept(TokenType.rparen);
				Statement body = parseStatement(false,true,false);
				return new ForStmt(s1,s2,s3,body,t.getTokenPosition());

			}
		} else if (allow_keyword && !Compiler.IS_MINI && (acceptOptional(TokenType.doKeyword) != null)){
			//do-while
			Statement doStmt = parseStatement();
			accept(TokenType.whileKeyword,TokenType.lparen);
			Expression whileExp = parseExpression();
			accept(TokenType.rparen);
			accept(TokenType.semicolon);
			return new DoWhileStmt(doStmt,whileExp,t.getTokenPosition());
		} else if (allow_keyword && (acceptOptional(TokenType.whileKeyword) != null)){
			accept(TokenType.lparen);
			Expression whileExp = parseExpression();
			accept(TokenType.rparen);
			Statement doStmt = parseStatement();
			return new WhileStmt(whileExp,doStmt,t.getTokenPosition());
		} else if (allow_keyword && ((t = acceptOptional(TokenType.switchKeyword)) != null)){
			accept(TokenType.lparen);
			Expression target = parseExpression();
			accept(TokenType.rparen);
			accept(TokenType.lcurly);
			
			
			CaseBlock firstBlock = null;
			int numBlocks = 0;
			CaseBlock last = null;
			Token casey = accept(TokenType.caseKeyword);
			boolean defaulted = false; //have we seen a default case block? only allowed one
			while (true){ //case blocks:
				SourcePosition casepos = casey.getTokenPosition();
				Terminal term;
				Token tok;
				if (casey.getTokenType() == TokenType.modifier){ //actually default keyword
					if (defaulted){
						throw err(new SyntaxError("Cannot have multiple default options in switch/case", casey));
					}
					defaulted = true;
					term = null;
				} else if ((tok = acceptOptional(TokenType.id)) != null){
					term = new Identifier(tok);
				} else {
					(tok) = acceptAny(Token.literals);
					term = Terminal.fromToken(tok);
				} 

				casey = null;
				
				accept(TokenType.colon);
				List<Statement> casebody = new ArrayList<Statement>();
				while ((casey = acceptAnySpec(
						new TokenSpec(TokenType.caseKeyword,"case"),
						new TokenSpec(TokenType.modifier,"default"))) == null){
					casebody.add(parseStatement());
					if (acceptOptional(TokenType.rcurly) != null){
						break;
					}
				}

				CaseBlock curr = new CaseBlock(term, casebody, null, casepos);
				numBlocks++;
				if (firstBlock == null){
					firstBlock = curr;
				}
				if (last != null){
					last.nextBlock = curr;
				}
				last = curr;

				if (casey == null){ //parsed rcurly; end of cases
					return new SwitchStmt(target, firstBlock, numBlocks, t.getTokenPosition());
				}
			}
		} else {
			DeclKeywords keywords = parseDeclarationKeywordsThrow();
			ClassContext ctx = new ClassContext(MemberScope.LOCAL);
			TypeOrExpression ttype = parseTypeOrExpression(); //don't allow assignment expressions on the left-hand side; that should be a statement
			// System.out.println(ttype);
			Token name,op,semi;
			if ((name = acceptOptional(TokenType.id)) != null){ //received a name after it; definitely a type meaning a var declaration
				if (ttype.getClassification() == TypeClassification.EXPRESSION){
					throw err(new SyntaxError("Unexpected token after reference expression: " + t,t));
				}
				TypeDenoter type = ttype.asType(); //received Type (type) id (name)
				if (foreach && (acceptOptional(TokenType.colon) != null)){
					Expression iterExpr =  parseExpression();
					LocalDecl iterVar = keywords.validate(ctx,new LocalDecl(keywords, type, new Identifier(name), null, type.posn));
					return new ForEachDummy(iterVar, iterExpr, type.posn);
				}

				KwargList initializers = new KwargList();
				while (true){
					if (name == null){
						name = accept(TokenType.id);
					}
					Kwarg init;
					if (acceptOptional(TokenType.assignment) != null){ //initialization
						Expression initExp = parseExpression(true);
						init = new Kwarg(new Identifier(name), initExp, name.getTokenPosition());
					} else {
						if (Compiler.IS_MINI){
							throw err(new SyntaxError("Minijava does not allow local variable declaration without initialization",name));
						}
						init = new Kwarg(name);
					}
					name = null;
					initializers.add(init);
					if (acceptOptional(TokenType.comma) != null){
						continue;
					}
					accept(TokenType.semicolon);
					break;
				}
				List<LocalDecl> decls = new ArrayList<>();
				for (Kwarg init : initializers){
					decls.add(keywords.validate(ctx,new LocalDecl(keywords, type, init.name, init.initExpression, type.posn)));
				}
				return new LocalDeclStmt(decls);

			} else if ((op = acceptAnyOptional(TokenType.assignOp,TokenType.assignment)) != null){
				if (Compiler.IS_MINI && op.getTokenType() != TokenType.assignment) {
					throw err(new SyntaxError("Assignment operator " + op + " not valid in miniJava",op));
				}
				if (ttype.getClassification() == TypeClassification.TYPE){
					throw err(new SyntaxError("Type missing identifier; unexpected token " + t,t));
				}
				Expression ref = ttype.asExpression();
				if (Compiler.IS_MINI && !(ref.isReferrable() || ref instanceof IxExpr)){
					throw err(new SyntaxError("Only reference expressions can be assigned to in miniJava",op));
				}

				if (!ref.isAssignable()){ //unassignable
					throw err(new SyntaxError("Cannot assign to read only expression " + ref,op));
				}
				Expression exp = parseExpression();
				accept(TokenType.semicolon);
				return new AssignStmt(ref, new Operator(op), exp, ref.posn);
			} else if ((semi = acceptOptional(TokenType.semicolon)) != null) {
				if (ttype.getClassification() == TypeClassification.TYPE){
					throw err(new SyntaxError("Expected identifier after type, received ;",semi));
				}
				Expression e = ttype.asExpression();
				if (e.isStateable()){
					return new ExprStmt(e,e.posn);
				} else {
					throw err(new SyntaxError("Expression value unused",e.posn));
				}
			} else {
				throw err(new SyntaxError("Unexpected token after type/reference: " + _currentToken,_currentToken));
			}
		}
	}


	private DeclKeywords parseDeclarationKeywordsThrow() {
		try {
			return parseDeclarationKeywords();
		} catch (InterfaceAnnotationFound i){
			throw err(new SyntaxError("reserved keyword @interface not allowed",i.atsign));
		}
	}

	private static class TokenSpec {
		public TokenType t;
		public String[] spells;
		public TokenSpec(TokenType type, String... spellings){
			t = type;
			spells = spellings;
		}
	}

	private static enum Precedence {
		DOT(16,new TokenSpec(TokenType.dot,".")),
		UNARY(14,new TokenSpec(null)),
		CAST(13),
		MULTIPLICATIVE(12,TokenType.binOp,"*","/","%"),
		ADDITIVE(11,TokenType.genOp,"+","-"),
		SHIFT(10,TokenType.binOp,">>","<<", ">>>"),
		RELATIONAL(9,
			new TokenSpec(TokenType.compOp,"<=",">="),
			new TokenSpec(TokenType.lchevron,"<"),
			new TokenSpec(TokenType.rchevron,">"),
			new TokenSpec(TokenType.instanceOfKeyword,"instanceof")),
		EQUALITY(8,TokenType.compOp,"==","!="),
		BITAND(7,TokenType.binOp,"&"),
		BITXOR(6,TokenType.binOp,"^"),
		BITOR(5,TokenType.binOp,"|"),
		AND(4,TokenType.binOp,"&&"),
		OR(3,TokenType.binOp,"||"),
		ASSIGN(1,
			new TokenSpec(TokenType.assignOp,"+=","-=","*=","/=","%=","&=","^=","|=","<<=",">>=",">>>="),
			new TokenSpec(TokenType.assignment, "="));

		public int level;
		public TokenSpec[] specs;
		Precedence(int lvl,TokenType opType, String... spellings){
			level = lvl;
			specs = new TokenSpec[1];
			specs[0] = new TokenSpec(opType,spellings);
		}
		Precedence(int lvl,TokenSpec... tokenSpecs){
			level = lvl;
			specs = tokenSpecs;
		}

		public static Precedence bottomLevel = ASSIGN;

		public Precedence up1(){
			Precedence last = null;
			for (Precedence p : Precedence.values()) {
				if (p == this){
					return last;
				}
				last = p;
			}
			return null;
		}
	}


	private Expression parseExpression(Precedence precedence){
		return parseExpression(false, precedence);
	}

	private Expression parseExpression(boolean allow_array_literal){
		return parseExpression(allow_array_literal, Precedence.bottomLevel);
	}

	private Expression parseExpression(){
		return parseExpression(false, Precedence.bottomLevel);
	}

	private Expression parseExpression(boolean allow_array_literal, Precedence precedence){
		Expression initExpression;
		SourcePosition startpos = _currentToken.getTokenPosition();
		Token t = acceptAnyOptional(TokenType.unOp,TokenType.genOp, TokenType.incOp);
		if (t != null){ //looking for unops, priority over binop
			Expression target = parseExpression(Precedence.UNARY.up1());
			if (t.getTokenType() == TokenType.incOp){
				if (Compiler.IS_MINI){
					throw err(new SyntaxError("Increment operations not allowed in miniJava",t));
				}
				initExpression = new IncExpr(new Operator(t), target, false, startpos);
			}
			initExpression = new UnaryExpr(new Operator(t), target, t.getTokenPosition());
		} else if (allow_array_literal && ((t = acceptOptional(TokenType.lcurly)) != null)) {
			ExprList elements = new ExprList();
			while (acceptOptional(TokenType.rcurly) == null){
				elements.add(parseExpression());
				if (acceptOptional(TokenType.comma) == null) {
					accept(TokenType.rcurly);
					break;
				}
			}
			initExpression = new ArrayLiteralExpr(elements,t.getTokenPosition());
		} else if ((t = acceptAnyOptional(Token.literals)) != null){ //any literal is an expression
			initExpression = new LiteralExpr(Terminal.fromToken(t), t.getTokenPosition());
		} else if (acceptOptional(TokenType.lparen) != null){ //enclosed expression OR type cast
			initExpression = parseExpression();
			accept(TokenType.rparen);
		} else if (acceptOptional(TokenType.newKeyword) != null){
			TypeDenoter classType = parseType(false); //raw type without brackets
			if (acceptOptional(TokenType.lparen) != null){ //class type: initialized by method call
				if (Compiler.IS_MINI){
					accept(TokenType.rparen); //constructors cannot take arguments!
					if (!(classType instanceof IdentifierType)) {
						throw new SyntaxError("Cannot instantiate complex classes in miniJava; can only instantiate ClassType.",classType.posn);
					}

					return new NewObjectExpr(classType, new ExprList(), startpos);
				}
				else {
					ExprList args = new ExprList();
					while (acceptOptional(TokenType.rparen) == null){
						args.add(parseExpression());
						if (acceptOptional(TokenType.comma) == null) {
							accept(TokenType.rparen);
							break;
						}
					}
					return new NewObjectExpr(classType,args,startpos);
				}
			}
			ExprList sizes = new ExprList();
			while (acceptOptional(TokenType.lsquare) != null){
				sizes.add(parseExpression());
				accept(TokenType.rsquare);
			}
			if (sizes.size() == 0){ //no brackets at all;
				throw new SyntaxError("Must call constructor or initialize array",classType.posn);
			}
			if (Compiler.IS_MINI){
				if (sizes.size() > 1){
					throw new SyntaxError("N-d array initialization not alowed in miniJava",sizes.get(1).posn);
				}
				initExpression = new NewArrayExpr(classType, sizes.get(0), startpos);
			} else {
				initExpression = new NewArrayExpr(classType,sizes,startpos);
			}
		} else if (startsTypeOrExpression(_currentToken.getTokenType())) { //avoid infinite loops, make sure parseTypeOrExpression won't call this
			TypeOrExpression ttype = parseTypeOrExpression(TypeClassification.EXPRESSION);
			if (ttype.getClassification() == TypeClassification.TYPE){
				throw err(new SyntaxError("Type is not a valid expression",ttype.asType().posn));
			}
			initExpression = ttype.asExpression();
		} else {
			_currentToken.getTokenType();
			throw new SyntaxError("Unexpected token in expression: " + _currentToken,_currentToken);
		}
		
		if ((t = acceptAnyOptional(TokenType.incOp)) != null){
			if (!initExpression.isAssignable()){
				throw err(new SyntaxError("Cannot increment non-assignable expression " + initExpression,t));
			}
			initExpression = new IncExpr(new Operator(t), initExpression, true, startpos);
		}
		Precedence p;
		Expression opExpression = initExpression;
		while (true){
			p = precedence;
			boolean hit = false;
			while (p != null){
	
				Token op = acceptAnySpec(p.specs);
				if (op != null) {
					// System.out.println("wow");
					if (p == Precedence.DOT){
						//dot: two options - dotExpr or CallExpr
						Identifier name = new Identifier(acceptAny(TokenType.id,TokenType.classKeyword));
						Token par;
						if ((par = acceptOptional(TokenType.lparen)) != null){
							if (name.kind == TokenType.classKeyword){
								throw err(new SyntaxError("Unexpected token `(`", par));
							}
							ExprList args = parseMethodArgs();
							opExpression = new CallExpr(name, args, startpos);
						} else {
							opExpression = new DotExpr(opExpression,name);
						}
					} else {
						if (op.getTokenType() == TokenType.instanceOfKeyword){
							opExpression = new InstanceOfExpression(opExpression,parseType(),startpos);
						}
						opExpression = new BinaryExpr(new Operator(op), opExpression, parseExpression(allow_array_literal,p.up1()),startpos);
					}
					hit = true;
				} else { //didn't match anything of this level, check for up by one
					p = p.up1();
				}
			}
			if (!hit){ //found no more ops
				break;
			}
		}

		if (acceptOptional(TokenType.question) != null){ //ternary operator
			Expression e2 = parseExpression(allow_array_literal);
			accept(TokenType.colon);
			Expression e3 = parseExpression(allow_array_literal);
			opExpression = new TernaryExpr(opExpression, e2, e3, startpos);
		}
		return opExpression;

	}

	/**
	 * will accept the rparen!!!!
	 * @return ExprList args
	 */
	private ExprList parseMethodArgs() {
		ExprList args = new ExprList();
		while (acceptOptional(TokenType.rparen) == null){
			args.add(parseExpression());
			if (acceptOptional(TokenType.comma) == null){
				accept(TokenType.rparen);
				break;
			}
		}
		return args;
	}

	private boolean startsTypeOrExpression(TokenType t){
		return (t == TokenType.thisKeyword || t == TokenType.id || Token.primitives.values().contains(t));
	}

	private TypeOrExpression parseTypeOrExpression(){
		return parseTypeOrExpression(TypeClassification.AMBIGUOUS);
	}

	private TypeOrExpression parseTypeOrExpression(TypeClassification expected){
		return parseTypeOrExpression(expected,true);
	}

	/** This function parses the beginning of an assignment or declaration expression
	 * @implNote
	 * So this got even more fucked since the last time LMAO
	 * The key issue is that, in general, you **can** assign to expressions - at least some of them. Specifically, 
	 * you can assign to index expressions and dot syntax (which can be applied to any expression in real java, not just references).
	 * Thus, we have to distinguish between a type and **any** type of expression. One important note is that we don't need to find 
	 * any potential expression - we just need to parse assignable (can be in `[exp] = [exp]`) and stateable (can be on their own, `exp;`)
	 * expressions; others will be an error. 
	 * 
	 * The following operations are valid for types:
	 * -- dot syntax: A.B.C
	 * ----if not miniJava, the identifier can also have chevrons for generics: A.B.C<Integer>.D.E<String>.F[][]
	 * -- Brackets: at the end of a type, B.A[][]
	 * 
	 * 
	 * The following expressions are assignable or stateable (can exist at the beginning of a line), and can start with identifiers:
	 * -- Call expression, stateable: <exp>(args)
	 * -- Increment expression, stateable: <exp>++;
	 * -- dot expression, assignable: <exp>.A = 2; 
	 * -- index expression, assignable: <exp>[2] = 3;
	 * 
	 * new object expressions are stateable, but always start with new instead of an identifier, so we don't have to consider them.
	 * However, those four expressions we *do* care about could potentially start with an identifier, so we need to be careful.
	 * Luckily, all four of those expressions have quite high operator precedence, so we don't need to care about things like 
	 * addition or multiplication. In fact, those four operations - dot, index, call, and increment - are the highest-precednce operators
	 * in java! (Funny how that works). Therefore, we only need to check for those four.
	 * 
	 * Of course, the two type operations and the 4 expression operations are not mutually exclusive; Especially annoying is the dot, whose
	 * syntax is indistinguishable for expressions and types. If we see an index with contents, we know that's an indexing expression; 
	 * if we see a call expression (lparent) or an increment expression (incop), we know it's an expression.
	 * 
	 * @return Type object (-> declaration statement), Expression object (-> assignment statement, always a RefExpr in miniJava),
	 * OR an AmbiguouosTypeOrExpression object if it's still ambiguouos.
	 */
	private TypeOrExpression parseTypeOrExpression(TypeClassification expected, boolean allow_array_type) throws SyntaxError{
		Token _base_token = _currentToken;
		if (acceptAnyOptional(Token.primitives.values()) != null){
			TypeDenoter base = PrimitiveType.fromToken(_base_token);
			if (Compiler.IS_MINI && _base_token.getTokenType() == TokenType.boolPrimitive){
				return base; //don't allow boolean arrays because minijava is silly
			}
			if (allow_array_type){
				while (acceptOptional(TokenType.lsquare) != null){
					accept(TokenType.rsquare);
					base = new ArrayType(base, base.posn);
					if (Compiler.IS_MINI){
						return base;
					}
				}
			}
			return base;
		}

		// int type = TYPE_OR_REFERENCE; //start ambiguous
		TypeClassification ttype = expected;
		TypeDenoter type;
		Expression exp;
		Token startToken = acceptOptional(TokenType.thisKeyword);
		if (startToken != null){
			ttype = TypeClassification.EXPRESSION;
			exp = new ThisRef(startToken.getTokenPosition());
			type = null;
		} else {
			startToken = acceptOptional(TokenType.id);
			if (startToken != null){
				Identifier id = new Identifier(startToken);
				exp = new IdRef(id);
				type = new IdentifierType(id);
			} else {
				return parseExpression(Precedence.ASSIGN.up1()); //don't allow assignment expressions on the lefthand side
			}
		}

		

		// boolean last_assignable = false;
		// boolean last_id = true;
		while (true) {
			if (type == null && exp == null){
				//too many overconstraints
				throw err(new SyntaxError("Unexpected type/expression conflict",
					_currentToken.getTokenPosition()));
			}

			//dot syntax; either Type.<id> or Expression.<id>
			Token dotToken = acceptOptional(TokenType.dot);
			if (dotToken != null){
				Identifier id = new Identifier(acceptAny(TokenType.id,TokenType.classKeyword));
				if (type != null) {
					if (id.kind == TokenType.classKeyword){
						if (ttype == TypeClassification.TYPE){
							//uh-oh, two overconstraints
							throw err(new SyntaxError("Class keyword not valid in type", id.posn));
						}
						type = null;
						ttype = TypeClassification.EXPRESSION;
					} else {
						type = new QualType(type, id);
					}
				}
				if (exp != null) {
					if (Compiler.IS_MINI && !exp.isReferrable()){
						if (ttype == TypeClassification.EXPRESSION){
							throw new SyntaxError("Cannot use dot syntax on general expression result in miniJava",dotToken);
						}
						exp = null;
						ttype = TypeClassification.TYPE;
						
					} else {
						exp = new DotExpr(exp, id);
					}
				}
				continue;
			}
			Token chevy;
			if (!Compiler.IS_MINI && ttype != TypeClassification.EXPRESSION && ((chevy = acceptOptional(TokenType.lchevron)) != null)) { //type generics syntax
				if (ttype == TypeClassification.EXPRESSION){ 
					throw err(new SyntaxError("Generics syntax not allowed in reference, only in type",chevy));
				}	
				if (type instanceof GenericType){
					throw err(new SyntaxError("Chained generics not allowed",chevy));
				}
				ttype = TypeClassification.TYPE;
				exp = null;

				List<TypeDenoter> generics = new ArrayList<TypeDenoter>();
				if (acceptOptional(TokenType.rchevron) != null){ //empty generic
					type = new GenericType(type,type.posn);
					continue;
				}
				
				do {
					generics.add(parseType());
				} while (acceptOptional(TokenType.comma) != null);
				accept(TokenType.rchevron);

				type = new GenericType(type, generics, type.posn);
				continue;
			}

			//bracket syntax; either Type[][][] (with no more tokens) or Expression[exp] with potentially more tokens
			Token lsq;
			if ((allow_array_type || ttype != TypeClassification.TYPE) &&  ((lsq = acceptOptional(TokenType.lsquare)) != null)){
				// System.out.println("Parse type lbracket");
				Token next = acceptOptional(TokenType.rsquare);
				if (next == null){ // next token is *not* rsquare, must be an expression inside
					if (ttype == TypeClassification.TYPE){
						//uh-oh, two overconstraints
						throw err(new SyntaxError("Invalid type or reference; Types cannot evaluate expressions in brackets. Expected ']', received " + _currentToken,_currentToken));
					}
					ttype = TypeClassification.EXPRESSION;
					type = null;
					Expression index = parseExpression();
					accept(TokenType.rsquare);
					exp = new IxExpr(exp, index, exp.posn);
					if (Compiler.IS_MINI){
						return exp; //can't have more than one level of depth in miniJava;
					}
					continue;
				} else { //next token *is* rsquare, only valid in type expressions
					if (ttype == TypeClassification.EXPRESSION){
						//uh-oh two overconstraints
						throw err(new SyntaxError("Invalid type or reference; Expected expression after '['', found " + next,next));
					}
					if (!allow_array_type){
						throw err(new SyntaxError("Array type not allowed in this context",lsq));
					}
					type = new ArrayType(type,type.posn);
					if (Compiler.IS_MINI){
						return type;
					}
					exp = null;
					while (acceptOptional(TokenType.lsquare) != null){
						accept(TokenType.rsquare);
						type = new ArrayType(type, type.posn);
					}
					return type;
				}
			}
			
			//Call expression
			Token s;
			if ((ttype != TypeClassification.TYPE) && ((s = acceptOptional(TokenType.lparen)) != null)){
				if (!exp.isCallable()){
					throw err(new SyntaxError("Unexpected Token " + s + "; only method identifiers can be called as functions",s));
				}

				ttype = TypeClassification.EXPRESSION;
				ExprList args = parseMethodArgs();
				exp = exp.toMethodCall(args); //turn the qualified expression into a method call by adding params
				if (Compiler.IS_MINI){
					return exp; //can't chain method calls in miniJava
				}
				continue;
			}

			Token inc;
			if ((!Compiler.IS_MINI) && (ttype != TypeClassification.TYPE) && ((inc = acceptOptional(TokenType.incOp)) != null)){
				ttype = TypeClassification.EXPRESSION;
				exp = new IncExpr(new Operator(inc),exp,true,exp.posn);
				continue;
			}
			break;
		}
		switch (ttype) {
			case TYPE:
				return type;
			case EXPRESSION:
				return exp;
			case AMBIGUOUS:
				return new AmbiguousTypeOrExpression(type,exp,type.posn);
			default:
				throw new UnsupportedOperationException("Unrecognized TypeClassification " + ttype);
		}
	}



	private TypeDenoter parseType(){
		return parseType(true);
	}

	private TypeDenoter parseType(boolean allow_array){
		TypeOrExpression t = parseTypeOrExpression(TypeClassification.TYPE,allow_array);
		if (t.getClassification() == TypeClassification.EXPRESSION){
			throw err(new SyntaxError("Exprected Type; found Expression instead",t.asExpression().posn));
		}
		return t.asType();
	}


	private List<Token> acceptListOf(TokenType ...t){
		if (t.length == 0){
			return new ArrayList<Token>();
		}
		ArrayList<Token> tokens = new ArrayList<Token>();
		while (true){
			Token first;
			if ((first = acceptOptional(t[0])) != null){
				tokens.add(first);
				for (TokenType next : t){
					if (first != null){
						first = null;
						continue;
					}
					tokens.add(accept(next));
				}
			} else {
				break;
			}
			if (acceptOptional(TokenType.comma) == null){
				break;
			}
		}
		return tokens;
	}




	private Token acceptAnyOptional(Iterable<TokenType> types){
		//unlike acceptOptional, will only accept one of the types
		for (TokenType t : types){
			Token res = acceptOptional(t);
			if (res != null){
				return res;
			}
		}
		return null;
	}
	private Token acceptAnyOptional(TokenType ...types){
		return acceptAnyOptional(Arrays.asList(types));
	}

	private Token acceptOptional(TokenType expectedType) {
		if (_currentToken.getTokenType() == expectedType){
			Token t = _currentToken;
			_currentToken = _scanner.scan();
			return t;
		}
		return null;
	}
	
	private List<Token> acceptOptional(TokenType ...expectedType) {
		ArrayList<Token> tokens = new ArrayList<Token>();
		for (TokenType t : expectedType){
			Token a = acceptOptional(t);
			if (a != null){
				tokens.add(a);
			}
		}
		return tokens;
	}



	private Token acceptAny(TokenType ...types) throws SyntaxError {
		return acceptAny(Arrays.asList(types));
	}

	private Token acceptAny(Iterable<TokenType> tokens) throws SyntaxError {
		ArrayList<TokenType> ts = new ArrayList<>();
		for (TokenType t : tokens){
			ts.add(t);
			if (_currentToken.getTokenType() == t){
				try{
					return _currentToken;
				} finally {
					_currentToken = _scanner.scan();
				}
			}
		}
		String tokenString = null;
		for (TokenType t : tokens){
			if (tokenString == null){
				tokenString = "";
			} else {
				tokenString += ", ";
			}
			tokenString += t;
		}

		throw err(new SyntaxError("Unexpected Token: " + _currentToken + " does not match any of expected types " + 
						tokenString,_currentToken));
	}

	private Token acceptAnySpec(TokenSpec... specs){
		for (TokenSpec spec : specs){
			if (_currentToken.getTokenType() == spec.t){
				for (String s : spec.spells){
					if (_currentToken.getTokenText().equals(s)){
						return accept(spec.t);
					}
				}
			}
		}
		return null;
	}

	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private Token accept(TokenType expectedType) throws SyntaxError {
		if( _currentToken.getTokenType() == expectedType ) {
			Token t = _currentToken;
			_currentToken = _scanner.scan();
			return t;
		}
		throw err(new SyntaxError("Unexpected Token: " + _currentToken + " is not of type " + expectedType,_currentToken));
	}

	private List<Token> accept(TokenType ...expectedType) throws SyntaxError {
		List<Token> res = new ArrayList<Token>(); 
		for (TokenType t : expectedType){
			res.add(accept(t));
		}
		return res;
	}

	private SyntaxError err(SyntaxError err) {
		_errors.reportError(err);
		return err;
	}
}
