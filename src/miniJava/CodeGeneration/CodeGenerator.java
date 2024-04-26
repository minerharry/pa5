package miniJava.CodeGeneration;

import java.lang.reflect.Field;
import java.util.Stack;

import miniJava.Compiler;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;
import miniJava.CodeGeneration.x64.Reg.RegSize;
import miniJava.ContextualAnalysis.TypeResult;
class CodeGenArg{
	public int nextOffset = -8; //starts at 8 because 0 is rbp; - because stack goes backwards
    public int getStackOffset(){ //kinda dumb but I love it
        int res = nextOffset;
        nextOffset -= 8;
        return res;
    }
	private boolean isAssign = false;
	public void setAssign(boolean ass){
		isAssign = ass;
	}
	public boolean getAssign(Expression expr){
		if (isAssign && !expr.isAssignable()){
			throw new UnsupportedOperationException("Cannot assign to expression: " + expr);
		}
		boolean ass = isAssign;
		isAssign = false;
		return ass;
	}
	public boolean isExprStmt = false;
}
public class CodeGenerator implements Visitor<CodeGenArg, CodegenResult> {

	private ErrorReporter _errors;
	// private InstructionList _asm; // our list of instructions that are used to make the code section
	
	public CodeGenerator(ErrorReporter errors) {
		this._errors = errors;
	}
	
	public void parse(Package prog,String destFile,boolean output_instructions) {

		//Context: allocation - create space for static class stuff
		InstructionList initInstructions = new StaticInitializer().parse(prog);
		
		// Here is some example code.
		
		// Simple operations:
		// _asm.add( new Push(0) ); // push the value zero onto the stack
		// _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX
		
		// Fancier operations:
		// _asm.add( new Cmp(new RByte(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
		// _asm.add( new Cmp(new RByte(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
		// _asm.add( new Add(new RByte(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx
		
		// Thus:
		// new RByte( ... ) where the "..." can be:
		//  RegRM, RegR						== rm, r
		//  RegRM, int, RegR				== [rm+int], r
		//  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
		// Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
		//
		// Note there are constructors for RByte where RegR is skipped.
		// This is usually used by instructions that only need one register operand, and often have an immediate
		//   So they actually will set RegR for us when we create the instruction. An example is:
		// _asm.add( new Mov_rmi(new RByte(Reg64.RDX,true), 3) ); // mov rdx,3
		//   In that last example, we had to pass in a "true" to indicate whether the passed register
		//    is the operand RM or R, in this case, true means RM
		//  Similarly:
		// _asm.add( new Push(new RByte(Reg64.RBP,16)) );
		//   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed
		
		// Patching example:
		// Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
		// _asm.add( someJump ); // populate listIdx and startAddress for the instruction
		// ...
		// ... visit some code that probably uses _asm.add
		// ...
		// patch method 1: calculate the offset yourself
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
		// -=-=-=-
		// patch method 2: let the jmp calculate the offset
		//  Note the false means that it is a 32-bit immediate for jumping (an int)
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );
		
		CodegenResult programCode = prog.visit(this,null);

		//finally: put all the code together!
		InstructionList fullCode = new InstructionList();
		//first, we need to assemble all of our initializer code. This consists of initInstructions (above) plus the
		//initializer code generated from this (in res.code)
		fullCode.addAll(initInstructions);
		fullCode.addAll(programCode.code);

		//set RBP to RSP
		fullCode.add(new Store(new RByte(Reg64.RBP,Reg64.RSP),prog));

		//we're going to have our entrypoint be right at the start of our initializer code, execute, and then *call* the main method
		//prog.mainMethod is populated by staticinitializer (is *is* a static method, after all!)
		//TODO: populate args pointers
		//stack will look like Args[] ptr, static class pointer
		//for now, just push static class pointer and call
		fullCode.add(new Lea(new RByte(Reg64.R15,prog.mainMethod.enclosingDecl().getStaticOffset(),Reg64.R8),prog)); //get main method static pointer for "this"
		fullCode.add(new Push(Reg64.R8,prog)); //push static pointer onto the stack, first argument
		fullCode.add(new Call(prog.mainMethod,prog));
		fullCode.add(new Pop(Reg64.R8,prog)); //pop static pointer again
		
		//TESTING
		// fullCode = new InstructionList();
		// fullCode.add(new Store(new RByte(Reg64.RSP,8,Reg64.R8)));
		
		//exit!
		fullCode.addAll(makeExit(0,prog)); //error code 0, successful exit
		

		//then, we need to put all of our methods together. 
		//Importantly, all of our method offsets have been floating; we need to update them now
		for (MethodTuple meth : programCode.methods){
			meth.method.basePointerOffset = fullCode.addAll(meth.body);
		}

		//finally: execute the final patch! bring it all home!
		fullCode.patch_all();

		// Output the file "a.out" if no errors
		if( !_errors.hasErrors() )
			makeElf(destFile,fullCode,fullCode.get(0).startAddress,output_instructions); //remember, our entrypoint is the initialization section, not the code itself
		
		System.out.println("Code generated successfully");		
	}

	@Override
	public CodegenResult visitPackage(Package prog, CodeGenArg arg) {
		if (!Compiler.IS_MINI){ prog.header.visit(this,arg); }
		
		CodegenResult res = new CodegenResult();

		for (ClassMemberDecl cmd : prog.classes){
			//Assign offsets for **INSTANCE** fields, **ENCLOSING CLASS POINTER**
			int offset = 8; //STARTING AT 8 - FIRST PART OF EVERY INSTANCE WILL BE A POINTER TO THE STATIC CLASS
			for (FieldDecl fd : cmd.fields){
				if (fd.isStatic()) { continue; }
				fd.basePointerOffset = offset;
				offset += fd.type.getTypeSize();
			}
			cmd.instanceSize = offset;
		}

		res.addAll(visitList(prog.classes, arg));



		// TODO: visit relevant parts of our AST
		return res;
	}

	
	@Override
	public CodegenResult visitClassDecl(ClassDecl cd, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();

		for (FieldDecl fd : cd.fields){
			if (!fd.isStatic()){
				continue;
			}
			//TODO: use initializers to initialize static fields
			//for now, just zero them out
			res.code.add(new StoreImmediate(new RByte(Reg64.R15,fd.getStaticOffset(),Reg64.RAX),0,fd));
		}
		res.addAll(visitList(cd.classmembers, arg));
		res.addAll(visitList(cd.methods, arg));
		
		return res;	
	}

	@Override
	public CodegenResult visitMethodDecl(MethodDecl md, CodeGenArg arg) {
		//Recall code setup on method call:
		// [RBP] - old RBP, restored on red
		// [RBP + 8] - return address
		// [RBP + 16] - "Self" arg; pointer to classdecl in case of static method, or instance object in case of instance method
		// [RBP + 24] - argument 1
		// [RBP + 32] - argument 2
		// etc.
		// The code generated here starts at the beginning of the method, after Call. arguments, cls/self, ret all pushed already

		CodeGenArg methodArg = new CodeGenArg();



		//CONVENTION: EXPRESSION RESULTS ALWAYS STORED IN R8
		CodegenResult res = new CodegenResult(); //just using it as instructionlist



		int offset = 24; //starting offset is [RBP + 24]; +16 is 'this', +8 is previous RBP, +0 is return address
		for (ParameterDecl pd : md.parameters){
			pd.basePointerOffset = offset;
			offset += pd.type.getTypeSize();
		}


		InstructionList body = new InstructionList();
		
		//first things first: fix RBP
		body.add(new Push(Reg64.RBP,md)); //push old
		body.add(new Store(new RByte(Reg64.RBP,Reg64.RSP),md)); //update current
		for (CodegenResult stmt : visitList(md.statementList, methodArg)){
			body.addAll(stmt.code);
		}
		

		res.methods.add(new MethodTuple(body,md));
		return res;
	}

	public CodegenResult doAssign(Expression val, Expression dest, AST assignObj, CodeGenArg arg){
		CodegenResult res = new CodegenResult();
		res.addAll(val.visit(this,arg)); //expression value is in R8
		res.code.add(new Push(Reg64.R8,assignObj)); //push onto stack while we get the pointer

		//Ok, so... assignments are to references. How do we do this?
		//The key difference in parsing an expression for assignment and parsing an expression for value is that the final step
		//is not an A.a, but an &A.a. However, we still need to evaluate A for value and then use the address of A.a
		//We can do this with arg!
		//Use arg to tell the expression that we are currently assigning. Each assignable expression will change its instructions
		//to instead store the *pointer* to its memory in R8, as opposed to the value in that location.
		arg.setAssign(true);
		res.addAll(dest.visit(this, arg));
		arg.setAssign(false); //inner method should set to false but just to be sure
		
		//now: Pop(R9) -> [R8]
		res.code.add(new Pop(Reg64.R9,assignObj));
		res.code.add(new Store(new RByte(Reg64.R8, 0, Reg64.R9),assignObj));
		//Assign complete
		return res;
	}

	@Override
	public CodegenResult visitAssignStmt(AssignStmt stmt, CodeGenArg arg) {
		return doAssign(stmt.val,stmt.expRef, stmt, arg);
	}

	@Override
	public CodegenResult visitBlockStmt(BlockStmt stmt, CodeGenArg arg) {
		return new CodegenResult(visitList(stmt.sl,arg));
	}

	@Override
	public CodegenResult visitExprStatement(ExprStmt exprStatement, CodeGenArg arg) {
		//TODO: arg.isExprStmt
		return exprStatement.baseExpr.visit(this,arg); //don't need to store the result anywhere though
	}

	@Override
	public CodegenResult visitLocalDeclStmt(LocalDeclStmt localDeclStmt, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();
		res.addAll(visitList(localDeclStmt.decls,arg));
        return res; //initialize and add declarations
	}

	@Override
	public CodegenResult visitLocalDecl(LocalDecl decl, CodeGenArg arg) {
		//pretty much just a series of assign statements, except you push instead of move the result of initialization

		CodegenResult res = new CodegenResult();
		if (decl.initializer != null){
			res.addAll(decl.initializer.visit(this, arg));
		} else {
			res.code.add(new Xor(new RByte(Reg64.R8,Reg64.R8),decl)); //0 the R8 register for initialization
		}
		res.code.add(new Push(Reg64.R8,decl));
		decl.basePointerOffset = arg.getStackOffset(); //negative because stack is reversed
		return res;
	}

	@Override
	public CodegenResult visitSyscallStmt(SyscallStmt syscallStmt, CodeGenArg arg) {
		switch (syscallStmt.type) {
			case PRINTLN:
				//takes an argument; parse it	
				CodegenResult res = syscallStmt.args.get(0).visit(this,arg); //should always be an idref; this is still same # of instructions
				//makePrintLn assumes the value is in R8, which it will have been
				//TODO: toString? pointer input to this function?
				res.code.addAll(makePrintln(false,syscallStmt));
				return res;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public CodegenResult visitWhileStmt(WhileStmt stmt, CodeGenArg arg) {
		//While is a somewhat complicated routine. 
		//General skeleton:
		//
		//label Cond:
		// eval(condExpr)
		// jump_if_false(end)
		// eval(body)
		// jump(Cond)
		//label end:

		CodegenResult res = stmt.cond.visit(this,arg); //eval(condExpr)
		Instruction condExp = res.code.get(0);
		Instruction finalJump = new Jmp(condExp, false, false, stmt); //the jump(Cond)
		res.code.add(new Cmp(new RByte(Reg64.R8, true),0,stmt)); //check if 0; if so, jump to end
		res.code.add(new CondJmp(Condition.E, finalJump, false, true, stmt)); //jump_if_false(end)
		res.addAll(stmt.body.visit(this,arg));  //eval(body)
		res.code.add(finalJump); //add jump(Cond)

		return res;
	}
	@Override
	public CodegenResult visitDoWhileStmt(DoWhileStmt stmt, CodeGenArg arg) {
		//Same idea as While, actually much simpler
		//General skeleton:
		//
		//label Body:
		// eval(body)
		// eval(condExpr)
		// jump_if_true(Body)

		CodegenResult res = stmt.body.visit(this,arg);
		res.addAll(stmt.cond.visit(this,arg));
		res.code.add(new Cmp(new RByte(Reg64.R8,true),0, stmt)); //check if 0; if not, jump to start
		res.code.add(new CondJmp(Condition.NE,res.code.get(0),false,false, stmt));
		return res;
	}

	@Override
	public CodegenResult visitIfStmt(IfStmt stmt, CodeGenArg arg) {
		//Skeleton:
		//
		// eval(cond)
		// jump_if_false(else)
		// eval(thenBody)
		// [if has else statement jump(end)]
		//label else
		// [if has else statement] eval(elseBody)
		//label end
		//
		
		CodegenResult res = stmt.cond.visit(this,arg); //eval(cond)
		res.code.add(new Cmp(new RByte(Reg64.R8,true),0, stmt)); //check if 0; if so, jump to else
		InstructionList then = stmt.thenStmt.visit(this,arg).code;
		if (stmt.elseStmt == null){
			Instruction lastStmt = then.last();
			Instruction falseJump = new CondJmp(Condition.E, lastStmt, false, true, stmt); 
			res.code.add(falseJump); //jump_if_false(else)
			res.code.addAll(then); //eval(thenBody)
		} else {
			InstructionList elseBody = stmt.elseStmt.visit(this,arg).code;
			Instruction elseLast = elseBody.last();
			Instruction endJump = new Jmp(elseLast,false,true, stmt);
			Instruction falseJump = new CondJmp(Condition.E,endJump,false,true, stmt);
			res.code.add(falseJump); //jump_if_false(else)
			res.code.addAll(then); //eval(thenBody)
			res.code.add(endJump); //jump(end)
			res.code.addAll(elseBody); //eval(elsebody)
		}
		return res;
	}

	@Override
	public CodegenResult visitReturnStmt(ReturnStmt stmt, CodeGenArg arg) {
		//first evaluate return expression, if it exists (return value just stays in R8)
		CodegenResult res = new CodegenResult();
		if (stmt.returnExpr != null){
			res.addAll(stmt.returnExpr.visit(this,arg));
			//load into r8
		}

		//executing Ret requires the return location to be the top of the stack. Therefore, we need to pop all the local variables!
		//TODO: arg needs to be way improved for scoped liveness. At least for this, we can just set RSP to RBP
		res.code.add(new Store(new RByte(Reg64.RSP, Reg64.RBP),stmt));
		//we also need to pop and restore the old RBP
		res.code.add(new Pop(Reg64.RBP, stmt));

		//return!
		res.code.add(new Ret(stmt)); //I'm just gonna pop the parameters during method call
		return res;
	}

	@Override
	public CodegenResult visitForStmt(ForStmt stmt, CodeGenArg arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CodegenResult visitSwitchStmt(SwitchStmt stmt, CodeGenArg arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CodegenResult visitThrowStmt(ThrowStmt throwStmt, CodeGenArg arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CodegenResult visitTryCatchFinallyStmt(TryCatchFinallyStmt stmt, CodeGenArg arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CodegenResult visitBreakStmt(BreakStmt breakStmt, CodeGenArg arg) {
		// TODO Needs more context - put in arg! this is just a goto I think?
		return null;
	}
	@Override
	public CodegenResult visitContinueStmt(ContinueStmt continueStmt, CodeGenArg arg) {
		// TODO Needs more context - put in arg! this is just a goto I think?
		return null;
	}

	@Override
	public CodegenResult visitForEachStmt(ForEachStmt stmt, CodeGenArg arg) {
		//TODO Needs Iterable conventions - much later
		return null;
	}

	@Override
	public CodegenResult visitUnaryExpr(UnaryExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		CodegenResult res = expr.expr.visit(this,arg);
		//Expression result in R8, time to evaluate
		switch (expr.operator.spelling) {
			case "!":
				//type checking means we know the value here is already a boolean (int value 0 or 1);
				res.code.add(new Xor(new RByte(Reg64.R8, true),1,expr)); //executes in place
				break;
			case "+":
				//make positive
				throw new UnsupportedOperationException("lazy");
				// break;

			case "-":
			//TODO: does Neg work?
				res.code.add(new Imul(Reg64.R8,new RByte(Reg64.R8,true),-1,expr));
				break;
		
			default:
				throw new UnsupportedOperationException("Invalid unary operator " + expr.operator.spelling);
		}
		return res;
	}

	@Override
	public CodegenResult visitBinaryExpr(BinaryExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		//TODO: add fast evaluation for && and ||
		CodegenResult res = expr.right.visit(this,arg);
		res.code.add(new Push(Reg64.R8, expr));
		res.addAll(expr.left.visit(this,arg));
		res.code.add(new Pop(Reg64.R9, expr));
		Operator op = expr.operator;
		//Left: R8, Right: R9
		RByte b89 = new RByte(Reg64.R8,Reg64.R9); //store in 8
		switch (op.spelling) {
			case "+":
				res.code.add(new Add(b89,op));
				break;
			case "-":
				res.code.add(new Sub(b89,op)); //R8 (left) - R9 (right) -> R8 (out)
				break;
			case "&&":
				res.code.add(new And(b89,op));
				break;
			case "||":
				res.code.add(new Or(b89,op));
				break;
			case "*":
				//annoyingly, Rbyte alone doesn't work for imul, it has same weirdness as idiv if we do it that way
				//but it does have a version that does what I want... annoying
				res.code.add(new Imul(Reg64.R8,new RByte(Reg64.R9,true),op));
				break;
			case "/": //as always divide f*cks things up
			case "%":
				res.code.add(new Store(new RByte(Reg64.RAX, Reg64.R8),op)); //copy R8 to RAX
				res.code.add(new Xor(new RByte(Reg64.RDX,Reg64.RDX),op)); //zero RDX (upper bits)
				res.code.add(new Idiv(b89,op));
				if (expr.operator.spelling.equals("/")){
					res.code.add(new Store(new RByte(Reg64.R8, Reg64.RAX),op));
				} else {
					res.code.add(new Store(new RByte(Reg64.R8, Reg64.RDX),op)); //very handy
				}
				break;
			case "<":
			case ">":
			case "<=":
			case ">=":
			case "==":
				//TODO: CONDITIONAL MOVE
				//OR EVEN BETTER, SETCOND
				res.code.add(new Cmp(b89,op));
				Instruction setFalse = new LoadImmediate(Reg64.R8, 0,op);
				Instruction setTrue = new LoadImmediate(Reg64.R8, 1,op);
				res.code.add(new CondJmp(Condition.getCond(expr.operator), setTrue,true,false,op));
				res.code.add(setFalse);
				res.code.add(new Jmp(setTrue,true,true,op)); //jump to **after** setTrue (next instruction)
				res.code.add(setTrue);
				break;
			default:
				break;
		}
		return res;
	}

	@Override
	public CodegenResult visitNewObjectExpr(NewObjectExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		CodegenResult res = new CodegenResult();
		res.code.addAll(makeMalloc(expr)); //allocate space on the heap for the class
		//Class pointer is in RAX. Populate class body!
		//We're gonna do some complex operations that use RAX, so store it in R9 for now
		res.code.add(new Store(new RByte(Reg64.R9,Reg64.RAX),expr));

		//Zero out the memory; just do it in 8byte chunks, an extra 7 doesn't matter(?)
		RegSize zeroSize = RegSize.m64;
		//clear the DF flag, make sure STOS will go the correct direction (WHICH IS?)
		res.code.add(new Cld(expr));
		//set RAX to zero (write zeros)
		res.code.add(new Xor(new RByte(Reg64.RAX,Reg64.RAX),expr));
		//set RDI to start address (currently in R9)
		res.code.add(new Store(new RByte(Reg64.RDI,Reg64.R9),expr));
		//how many bytes do we need to write?
		int numStores = (int)(Math.ceil(expr.type.typeDeclaration.instanceSize / (1.0 * zeroSize.size)));
		System.out.println("numstores: " + numStores);
		//set RCX to the number of operations we need to do
		res.code.add(new StoreImmediate(new RByte(Reg64.RCX,true), numStores,expr));
		//zero the memory!
		res.code.add(new RepStos(zeroSize,expr));

		//I don't know if this is necessary??? but let's put a pointer to the static declarator in the class header
		res.code.add(new Lea(new RByte(Reg64.R15,expr.type.typeDeclaration.getStaticOffset(),Reg64.R8),expr));
		res.code.add(new Store(new RByte(Reg64.R9,0,Reg64.R8),expr));

		//Finally, copy class pointer (R9) to R8 and return it
		res.code.add(new Store(new RByte(Reg64.R8,Reg64.R9),expr));
		

		//TODO: Field initializers?
		//TODO: Call constructor?
		//TODO: Class offsets?

		return res;
	}

	@Override
	public CodegenResult visitDotExpr(DotExpr expr, CodeGenArg arg) {
		//TODO: What happens if the object at the address is smaller than 8 bytes? Did I put them too close together
		//ASSIGNABLE EXPRESSION! Two modes: pointer return and value return
		//first, evaluate the underlying statement
		boolean doAssign = arg.getAssign(expr);
		CodegenResult res = expr.exp.visit(this,arg);

		//since we're dotting, the return type must be an object, and R8 must hold a pointer to somewhere on the heap
		//get the offset to the actual memory location 
		int offset = expr.name.refDecl.basePointerOffset;
		RByte address = new RByte(Reg64.R8,offset,Reg64.R8);
		if (doAssign){
			//set R8 to R8-offset
			res.code.add(new Lea(address,expr));
			
		} else {
			//load [R8 - offset] into R8
			res.code.add(new Load(address,expr));
		}
		return res;
	}

	@Override
	public CodegenResult visitThisRef(ThisRef ref, CodeGenArg arg) {
		arg.getAssign(ref);
		
		//NOTE: THIS ASSUMES WE ARE IN A METHOD!!
		//ThisRef is always stored in RBP+16, then ret, then old rbp
		CodegenResult res = new CodegenResult();
		res.code.add(new Load(new RByte(Reg64.RBP,16, Reg64.R8),ref)); //[RBP+16] -> R8
		return res;
	}

	//THIS IS THE HARD ONE
	@Override
	public CodegenResult visitIdRef(IdRef ref, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();
		Declaration idDec = ref.id.refDecl;
		boolean doLea = arg.getAssign(ref);
		RByte r; //we will always be either Loading or Leaing a memory location. just populate rbyte
		if (idDec.isMember()){ //ref is a classname. Need to get the reference to that static class!
			switch (idDec.asMemberDecl().getMemberType()) {
				case CLASSLIKE:
					if (doLea){
						throw new UnsupportedOperationException("Cannot assign to type");
					}

					//total offset is thus the offset to the static class we want. offset starts from R15, so class position is R15+offset.
					doLea = true; //we actually know the pointer wholesale, no loads necessary
					r = new RByte(Reg64.R15,idDec.asMemberDecl().getStaticOffset(),Reg64.R8); //load that pointer!
					break;
				case FIELD:
					FieldDecl fd = idDec.asMemberDecl().asField();
					//TODO: intelligent version for nested instance classes. For now, ignore
					if (!fd.isStatic()){
						//ASSUMPTION: That means it is in *this* class!
						//simple check: its enclosingDecl should be this class; if not, its an element of an enclosing class
						// (which is unsupported)

						if (!fd.enclosingDecl().equals(ref.context)){
							throw new UnsupportedOperationException("Instance field referenced in inner class??");
						}
						
						res.code.add(new Load(new RByte(Reg64.RBP,16, Reg64.R8),ref)); //first, load this class pointer
						r = new RByte(Reg64.R8,fd.basePointerOffset,Reg64.R8); //then, load the offset from the class pointer
					} else {
						//find the static location like above
						r = new RByte(Reg64.R15,fd.getStaticOffset(),Reg64.R8); //can assign to or load
					}
					break;
				default:
					throw new UnsupportedOperationException();
			}
			
		} else if (idDec instanceof LocalDecl){
			//base offset relative to RBP
			r = new RByte(Reg64.RBP,idDec.basePointerOffset,Reg64.R8);
		} else {
			throw new UnsupportedOperationException();
		}

		if (doLea){
			res.code.add(new Lea(r,ref));
		} else {
			res.code.add(new Load(r,ref));
		}
		return res;
	}

	@Override
	public CodegenResult visitIncExpr(IncExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		arg.setAssign(true);
		CodegenResult res = expr.incExp.visit(this,arg);
		
		//R8 now has pointer to object (can be on the stack!)
		RByte rm = new RByte(Reg64.R8, true);
		switch (expr.incOp.spelling) {
			case "++":
				//increment
				res.code.add(new Inc(rm,expr.incOp));
				break;
			case "--":
				//decrement
				res.code.add(new Dec(rm,expr.incOp));
				break;
		
			default:
				throw new UnsupportedOperationException("Invalid operator " + expr.incOp.repr());
		}
		
		//increment is an expression! but value can sometimes not be used. If this is an expr statement, don't return value
		if (!arg.isExprStmt){
			res.code.add(new Load(new RByte(Reg64.R8,Reg64.R8),expr)); //[R8] -> R8, load just-incremented value
		}
		return res;
	}

	@Override
	public CodegenResult visitLiteralExpr(LiteralExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		CodegenResult res = new CodegenResult();
		switch (expr.lit.kind) {
			case intLiteral:
				res.code.add(new LoadImmediate(Reg64.R8, Integer.parseInt(expr.lit.spelling),expr.lit));
				break;
			case nullLiteral:
				res.code.add(new LoadImmediate(Reg64.R8, 0, expr.lit)); //null pointer is value 0
				break;
			case boolLiteral:
				res.code.add(new LoadImmediate(Reg64.R8, (Boolean.parseBoolean(expr.lit.spelling) ? 1 : 0), expr.lit));
				break;
			
			default:
				throw new UnsupportedOperationException("Literal " + expr.lit.repr() + " unsupported");
		}
		return res;
	}


	@Override
	public CodegenResult visitNewArrayExpr(NewArrayExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		//Array implementation! On the heap. **POINTER IS TO THE *SIZE* OF THE ARRAY (8 byte int)**; first element is ptr - 8
		//TODO: Multidimensional arrays
		CodegenResult res = expr.sizeExprs.get(0).visit(this,arg); //get size of array in R8
		res.code.add(new Push(Reg64.R8, expr)); //R8 gets clobbered by syscall
		res.code.addAll(makeMalloc(expr)); //allocate space on the heap for the array
		res.code.add(new Pop(Reg64.R8,expr)); //R8 gets clobbered by syscall
		res.code.add(new Store(new RByte(Reg64.RAX, 0, Reg64.R8),expr)); //[RAX + 0] = R8
		res.code.add(new Load(new RByte(Reg64.RAX, Reg64.R8),expr)); //load pointer (RAX) to R8 as expression results
		return res;
	}

	@Override
	public CodegenResult visitIxExpr(IxExpr expr, CodeGenArg arg) {
		boolean doAssign = arg.getAssign(expr);
		CodegenResult res = expr.exRef.visit(this,arg); //R8 is array pointer
		res.code.add(new Push(Reg64.R8,expr)); //push pointer for later
		res.addAll(expr.ixExpr.visit(this,arg)); //get index in R8
		res.code.add(new Pop(Reg64.R9,expr)); //load array pointer

		//now: array pointer is R9 (rdisp), index is R8.
		int elemSize = expr.returnType.getTypeSize(); //how large is each element? (mult value for RM)
		int offset = 8; //skip over the first 8 bytes, which represent the length of the array

		RByte rb = new RByte(Reg64.R9, Reg64.R8, elemSize, offset, Reg64.R8); //Load [R9 + R8*elemSize + offset] into R8
		if (doAssign){
			res.code.add(new Lea(rb, expr));
		} else {
			res.code.add(new Load(rb, expr));
		}
		return res;
	}



	@Override
	public CodegenResult visitCallExpr(CallExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		//So this is very annoying. I want to evaluate the expressions from left to right (method object, param1, param2, etc),
		//but the argument convention is right to left. How do I do this??
		//wishy-washy solution: stack pointer based, push a bunch of zeros onto the stack. 
		// **use RSP+ for offset instead of RBP- so it's consistent and needs no registers**

		CodegenResult res = new CodegenResult();
		//"Push" nargs+1 zeros onto the stack
		int argsize = 8*(expr.argList.size()+1); //TODO: for more accurate object sizes
		res.code.add(new Sub(new RByte(Reg64.RSP,true),argsize,expr)); 

		//RSP = THIS
		//RSP+8 = ARG1
		//RSP+16 = ARG2
		//ETC
		//then, when we push, ret gets put into what is right now RSP-8

		MethodDecl method = expr.methodName.refDecl.asMemberDecl().asMethod();

		if (expr.baseExp != null){
			//Evaluate 'this' (*this can be a static pointer! works just fine.*)
			res.addAll(expr.baseExp.visit(this,arg));
			//R8 now has pointer to "this" object, store in RSP+8
		} else {
			//unqualified method call, needs the right 'this'... hoorayyyyy
			if (method.isStatic()){
				res.code.add(new Lea(new RByte(Reg64.R15,method.enclosingDecl().getStaticOffset(), Reg64.R8),expr)); //get pointer to static class
			} else {
				//ASSUMPTION: That means it is in *this* class! Just load this class's pointer, pass it on
				//simple check: its enclosingDecl should be this class; if not, its an element of an enclosing class
				// (which is unsupported)
				
				if (!method.enclosingDecl().equals(expr.context)){
					throw new UnsupportedOperationException("Instance field referenced in inner class??");
				}
				res.code.add(new Load(new RByte(Reg64.RBP,16, Reg64.R8),expr)); //load this class pointer
			}
		}
		
		//this pointer loaded into R8, store into RSP+8
		res.code.add(new Store(new RByte(Reg64.RSP,0,Reg64.R8),expr)); //[RSP] = R8
		
		
		//eval arguments, store
		for (int i = 0; i < expr.argList.size(); i++){
			int offset = 8*i + 8; //starts at RSP + 8
			res.addAll(expr.argList.get(i).visit(this,arg)); //eval argument expression, stored in R8
			res.code.add(new Store(new RByte(Reg64.RSP,offset,Reg64.R8),expr)); //[RSP+offset] = R8
		}

		//okay, now args are set up. Calling the method will push return address; callee is responsible for pushing, changing, and restoring RBP
		res.code.add(new Call(method,expr));

		//Ok, method called. We get back here when the result has returned. Return value (if it exists) will be in R8; 
		//same as return value of this expression. 

		//"pop" arguments
		res.code.add(new Add(new RByte(Reg64.RSP,true),argsize,expr)); 
		return res;
	}

	@Override
	public CodegenResult visitMethodRefExpr(MethodRefExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		//pointer to that method??
		// TODO this can just be a pointer I guess but needs to be patched to the method's location at assemble time.
		return null;
	}


	@Override
	public CodegenResult visitArrayLiteralExpr(ArrayLiteralExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		// TODO Auto-generated method stub
		return null;
	}	

	@Override
	public CodegenResult visitTernaryExpr(TernaryExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		// TODO Auto-generated method stub
		return null;
	}

	
	public void makeElf(String fname, InstructionList code, long mainMethod, boolean save_instructions) {
		ELFMaker elf = new ELFMaker(_errors, code.getSize(), 8); // bss ignored until PA5, set to 8
		elf.outputELF(fname, code.getBytes(), mainMethod);
		if (save_instructions){
			String instruction_loc = fname + ".instrs";
			elf.saveInstructions(instruction_loc, code.getBytes(), mainMethod);
		}
	}
	
	public static InstructionList makeMalloc(AST reason) {
		InstructionList malloc = new InstructionList();
		malloc.add( new StoreImmediate(    new RByte(Reg64.RAX,true),0x09, reason) ); // mmap
		malloc.add( new Xor(		       new RByte(Reg64.RDI,Reg64.RDI), reason) 	); // addr=0
		malloc.add( new StoreImmediate(	   new RByte(Reg64.RSI,true),0x1000, reason) ); // 4kb alloc
		malloc.add( new StoreImmediate(	   new RByte(Reg64.RDX,true),0x03, reason) 	); // prot read|write
		malloc.add( new StoreImmediate(	   new RByte(Reg64.R10,true),0x22, reason) 	); // flags= private, anonymous
		malloc.add( new StoreImmediate(	   new RByte(Reg64.R8, true),-1, reason) 	); // fd= -1
		malloc.add( new Xor(		       new RByte(Reg64.R9,Reg64.R9), reason) 	); // offset=0
		malloc.add( new Syscall( reason) );
		
		// pointer to newly allocated memory is in RAX
		// return the instructions; original return address, the index of the first instruction in this method, can be found with malloc.get(0)
		return malloc;
	}

	private InstructionList makeExit(RByte error, AST caller) { //w
		InstructionList res = new InstructionList();
		// movq $60, %rax  ; use the `_exit` [fast] syscall
		res.add(new StoreImmediate(new RByte(Reg64.RAX,true),60, caller));
		// movq $0, %rdi   ; error code 0
		error.SetRegR(Reg64.RDI);
		res.add(new Load(error, caller));
		// syscall         ; make syscall
		res.add(new Syscall(caller));
		return res;
	}

	private InstructionList makeExit(int errCode, AST caller) {
		InstructionList res = new InstructionList();
		// movq $60, %rax  ; use the `_exit` [fast] syscall
		res.add(new StoreImmediate(new RByte(Reg64.RAX,true),60, caller));
		// movq $0, %rdi   ; error code 0
		res.add(new LoadImmediate(Reg64.RDI,errCode, caller));
		// syscall         ; make syscall
		res.add(new Syscall(caller));
		return res;
	}
	
	private InstructionList makePrintln(boolean pointer, AST caller) {
		InstructionList res = new InstructionList();
		// movq $1, %rax   ; use the `write` [fast] syscall
		res.add(new StoreImmediate(new RByte(Reg64.RAX, true), 1, caller));
		// movq $1, %rdi   ; write to stdout, filedescriptor=1
		res.add(new StoreImmediate(new RByte(Reg64.RDI, true),1, caller));
		

		// movq $msg, %rsi ; use string "Hello World"
		// movq $12, %rdx  ; write 12 characters

		//STRING GENERATION
		//$msg below is the address to the start of the string. We need to assemble that string!
		//Syscall takes an argument; loaded into R8. that argument is either a single int value to print (miniJava)
		//or a pointer to a string somewhere in memory (pointer = true)
		if (pointer){
			throw new UnsupportedOperationException();
		} else {
			//let's make a string on the stack
			
			//push 0 (null-terminate)
			res.add(new Push(0, caller));
			//push the value (in R8)
			res.add(new Push(Reg64.R8, caller));
			//then get the pointer to the start, that's RSP - put into rsi
			res.add(new Store(new RByte(Reg64.RSI,Reg64.RSP), caller));
			//message assembled

			//since the length of the string is dynamic too, we need to set it (it's just 1)
			res.add(new StoreImmediate(new RByte(Reg64.RDX,true),1, caller));
		}

		//finally, the syscall itself
		// syscall         ; make syscall
		res.add(new Syscall(caller));

		//if not a pointer, we need to clean up that string we just made
		if (!pointer){
			res.add(new Pop(Reg64.RAX, caller)); //register doesn't matter here
			res.add(new Pop(Reg64.RAX, caller));
		}

		return res;
	}

	@Override
	public CodegenResult visitFileHeader(FileHeader fileHeader, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitFileHeader'");
	}

	@Override
	public CodegenResult visitPackageDecl(PackageDecl packageDecl, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitPackageDecl'");
	}

	@Override
	public CodegenResult visitImportStatement(ImportStatement importStatement, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitImportStatement'");
	}

	@Override
	public CodegenResult visitDeclKeywords(DeclKeywords declParams, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitDeclKeywords'");
	}

	@Override
	public CodegenResult visitModifier(Modifier modifier, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitModifier'");
	}

	@Override
	public CodegenResult visitProtection(Protection protection, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitProtection'");
	}

	@Override
	public CodegenResult visitGenericVar(GenericVar genericVar, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitGenericVar'");
	}

	@Override
	public CodegenResult visitAnnotationDecl(AnnotationDecl annotationDecl, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitAnnotationDecl'");
	}

	@Override
	public CodegenResult visitEnumDecl(EnumDecl enumDecl, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitEnumDecl'");
	}

	@Override
	public CodegenResult visitInterfaceDecl(InterfaceDecl interfaceDecl, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitInterfaceDecl'");
	}

	@Override
	public CodegenResult visitEnumElement(EnumElement enumElement, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitEnumElement'");
	}

	@Override
	public CodegenResult visitFieldDecl(FieldDecl fd, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitFieldDecl'");
	}

	@Override
	public CodegenResult visitConstructorDecl(ConstructorDecl constructorDecl, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitConstructorDecl'");
	}

	@Override
	public CodegenResult visitEmptyMethodDecl(EmptyMethodDecl emptyMethodDecl, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitEmptyMethodDecl'");
	}

	@Override
	public CodegenResult visitParameterDecl(ParameterDecl pd, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitParameterDecl'");
	}

	@Override
	public CodegenResult visitInitializer(Kwarg initializer, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitInitializer'");
	}

	@Override
	public CodegenResult visitPrimitiveType(PrimitiveType type, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitPrimitiveType'");
	}

	@Override
	public CodegenResult visitIdentifierType(IdentifierType type, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIdentifierType'");
	}

	@Override
	public CodegenResult visitArrayType(ArrayType type, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitArrayType'");
	}

	@Override
	public CodegenResult visitEllipsisType(EllipsisType ellipsisType, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitEllipsisType'");
	}

	@Override
	public CodegenResult visitQualType(QualType qualType, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitQualType'");
	}

	@Override
	public CodegenResult visitGenericType(GenericType genericType, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitGenericType'");
	}

	@Override
	public CodegenResult visitCatchBlock(CatchBlock block, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitCatchBlock'");
	}

	@Override
	public CodegenResult visitCaseBlock(CaseBlock block, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitCaseBlock'");
	}

	@Override
	public CodegenResult visitIdentifier(Identifier id, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIdentifier'");
	}

	@Override
	public CodegenResult visitOperator(Operator op, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitOperator'");
	}

	@Override
	public CodegenResult visitIntLiteral(IntLiteral num, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIntLiteral'");
	}

	@Override
	public CodegenResult visitBooleanLiteral(BooleanLiteral bool, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitBooleanLiteral'");
	}

	@Override
	public CodegenResult visitStringLiteral(StringLiteral literal, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitStringLiteral'");
	}

	@Override
	public CodegenResult visitFloatLiteral(FloatLiteral literal, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitFloatLiteral'");
	}

	@Override
	public CodegenResult visitCharLiteral(CharLiteral literal, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitCharLiteral'");
	}

	@Override
	public CodegenResult visitNullLiteral(NullLiteral literal, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitNullLiteral'");
	}

	

}
