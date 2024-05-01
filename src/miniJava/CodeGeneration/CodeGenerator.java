package miniJava.CodeGeneration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import miniJava.Compiler;
import miniJava.CompilerError;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.SwitchStmt.SwitchType;
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


	public Instruction loopContinue; //jump locations for current loop
	public Instruction loopBreak;

	public boolean finallyInsideLoop; //whether the innermost finally (where a break/continue would go) is inside of a loop (interrupts break/continue)

	public Instruction currentHandler; //jump location for throw statement; if null, just doing a return
	public Instruction currentFinally; //jump location for finally block, for return/break/coninue which are guaranteed to not 

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

		res.addAll(visitList(prog.classes, arg));

		return res;
	}

	
	@Override
	public CodegenResult visitClassDecl(ClassDecl cd, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();

		for (FieldDecl fd : cd.fields){
			if (!fd.isStatic()){
				continue;
			}
			if (fd.initializer == null)
				res.code.add(new StoreImmediate(new RByte(Reg64.R15,fd.getStaticOffset(),Reg64.RAX),0,fd)); //code is the initialization code
			else {
				CodeGenArg fieldArg = new CodeGenArg(); //I don't think arg matters for expressions like this?
				res.code.addAll(fd.initializer.visit(this,fieldArg).code);
				res.code.add(new Store(new RByte(Reg64.R15,fd.getStaticOffset(),Reg64.R8),fd));
			}
		}
		res.addAll(visitList(cd.classmembers, arg));
		res.addAll(visitList(cd.methods, arg));
		
		return res;	
	}

	@Override
	public CodegenResult visitEnumDecl(EnumDecl ed, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();
		for (EnumElement el : ed.elements){
			//each element is 
		}
		for (FieldDecl fd : ed.fields){
			
		}
		res.addAll(visitList(ed.classmembers, arg));
		res.addAll(visitList(ed.methods, arg));
		
		return res;	
	}
	
	@Override
	public CodegenResult visitFieldDecl(FieldDecl fd, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();
		if (fd.isStatic()){
			if (fd.initializer == null)
				res.code.add(new StoreImmediate(new RByte(Reg64.R15,fd.getStaticOffset(),Reg64.RAX),0,fd)); //code is the initialization code
			else {
				CodeGenArg fieldArg = new CodeGenArg(); //I don't think arg matters for expressions like this?
				res.code.addAll(fd.initializer.visit(this,fieldArg).code);
				res.code.add(new Store(new RByte(Reg64.R15,fd.getStaticOffset(),Reg64.R8),fd));
			}
		}
		return res;
	}
	
	@Override
	public CodegenResult visitEnumElement(EnumElement el, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();

		CodeGenArg elArg = new CodeGenArg(); //I don't think arg matters for expressions like this?
		res.code.addAll(doNewObject(el.parent, el.constructor, el.args, el, elArg));
		res.code.add(new Store(new RByte(Reg64.R15,el.getStaticOffset(),Reg64.R8),el));
		return res;
	}
	

	@Override
	public CodegenResult visitConstructorDecl(ConstructorDecl cd, CodeGenArg arg) {
		return this.visitMethodDecl(cd, arg); //constructors are just methods! very handy, that
	}

	@Override
	public CodegenResult visitOverloadedMethod(OverloadedMethod overloadedMethod, CodeGenArg arg) {
		return new CodegenResult(visitList(overloadedMethod.methods, arg));
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

	public Instruction setStack(int offset, AST source){
		return new Lea(new RByte(Reg64.RBP, offset, Reg64.RSP),source);
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
		//block statement always defines a scope!
		int origOffset = arg.nextOffset;
		CodegenResult res = new CodegenResult(visitList(stmt.sl,arg));
		res.code.add(setStack(origOffset, stmt));
		arg.nextOffset = origOffset; //pop all local variables
		return res;
	}

	@Override
	public CodegenResult visitExprStatement(ExprStmt exprStatement, CodeGenArg arg) {
		//TODO: arg.isExprStmt optimization
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
		//label continue:
		// [reset stack pointer]
		// jump(Cond)
		//label break:
		// [reset stack pointer]
		//label end:

		Instruction cont = arg.loopContinue;
		Instruction brk = arg.loopBreak; //store for later. During eval(body), these will be set to cond and end respectively.
		boolean fil = arg.finallyInsideLoop;
		arg.finallyInsideLoop = false; //innermost block is now a loop
		int origOffset = arg.nextOffset;

		CodegenResult res = stmt.cond.visit(this,arg); //eval(condExpr)
		Instruction condExp = res.code.first();

		InstructionList continueBlock = new InstructionList();
		continueBlock.add(setStack(origOffset, stmt));
		continueBlock.add(new Jmp(condExp, false, false, stmt)); //the jump(Cond)); //add jump(Cond)
		arg.loopContinue = continueBlock.first();
		
		Instruction breakReset = setStack(origOffset, stmt);
		arg.loopBreak = breakReset;
		
		
		res.code.add(new Cmp(new RByte(Reg64.R8, true),0,stmt)); //check if 0; if so, jump to end
		res.code.add(new CondJmp(Condition.E, breakReset, false, true, stmt)); //jump_if_false(end)
		res.addAll(stmt.body.visit(this,arg));  //eval(body)

		res.code.addAll(continueBlock);
		res.code.add(breakReset);

		arg.nextOffset = origOffset;
		arg.loopContinue = cont;
		arg.loopBreak = brk;
		arg.finallyInsideLoop = fil;

		return res;
	}
	@Override
	public CodegenResult visitDoWhileStmt(DoWhileStmt stmt, CodeGenArg arg) {
		//Same idea as While, actually much simpler
		//General skeleton:
		//
		//label Body:
		// eval(body)
		//label continue:
		// [reset stack pointer]
		// eval(condExpr)
		// jump_if_true(Body)
		//label break:
		// [reset stack pointer]

		Instruction cont = arg.loopContinue;
		Instruction brk = arg.loopBreak; //store for later. During eval(body), these will be set to cond and end respectively.
		boolean fil = arg.finallyInsideLoop;
		arg.finallyInsideLoop = false; //innermost block is now a loop
		int origOffset = arg.nextOffset;

		InstructionList continueBlock = new InstructionList();
		continueBlock.add(setStack(origOffset, stmt));
		continueBlock.addAll(stmt.cond.visit(this,arg).code);
		continueBlock.add(new Cmp(new RByte(Reg64.R8,true),0, stmt));
		//jump requires evaluating the body, will be added in a bit
		arg.loopContinue = continueBlock.first();

		Instruction breakReset = setStack(origOffset, stmt);
		arg.loopBreak = breakReset;

		CodegenResult res = stmt.body.visit(this,arg);
		continueBlock.add(new CondJmp(Condition.NE,arg.loopContinue,false,false,stmt));

		res.code.addAll(continueBlock);
		res.code.add(breakReset);

		arg.nextOffset = origOffset;
		arg.loopContinue = cont;
		arg.loopBreak = brk;
		arg.finallyInsideLoop = fil;
	
		return res;
	}

	@Override
	public CodegenResult visitIfStmt(IfStmt stmt, CodeGenArg arg) {
		//Skeleton:
		//
		// eval(cond)
		// jump_if_false(else) or jump_if_false(end)
		// eval(thenBody)
		// [if has else statement jump(end)]
		//label else
		// [if has else statement] [reset stack pointer] 
		// [if has else statement] eval(elseBody)
		//label end
		// [reset stack pointer]
		
		int origOffset = arg.nextOffset;

		CodegenResult res = stmt.cond.visit(this,arg); //eval(cond)
		res.code.add(new Cmp(new RByte(Reg64.R8,true),0, stmt)); //check if 0; if so, jump to else
		
		InstructionList then = stmt.thenStmt.visit(this,arg).code;

		InstructionList elseBlock = new InstructionList();
		if (stmt.elseStmt != null){
			elseBlock.add(setStack(origOffset, stmt)); //[reset stack pointer] #2
			arg.nextOffset = origOffset;
			elseBlock.addAll(stmt.elseStmt.visit(this,arg).code); //[if has else statement] eval(elseBody)
		}

		Instruction lastReset = setStack(origOffset, stmt);
		elseBlock.add(lastReset); //this means the jump location is always elseblock.first()
		arg.nextOffset = origOffset;

		res.code.add(new CondJmp(Condition.E, elseBlock.first(), false, false, stmt)); //jump_if_false(else)
		res.code.addAll(then); //eval(thenBody)
		if (stmt.elseStmt != null){
			res.code.add(new Jmp(lastReset, false, false, stmt));
		}
		res.code.addAll(elseBlock);

		return res;
	}

	public InstructionList doReturn(AST source, CodeGenArg arg){
		InstructionList res = new InstructionList();
		//IF THERE IS A FINALLY: JUMP THERE INSTEAD
		if (arg.currentFinally != null){
			//store: we are doing a return
			if (!(source instanceof TryCatchFinallyStmt)) //if this is from a trycatchfinally, the interrupt code is already set
				res.add(new LoadImmediate(Reg64.R14, FinallyAction.RETURN.code, source)); 
			res.add(new Jmp(arg.currentFinally, false, false, source));
			return res;
		}

		//executing Ret requires the return location to be the top of the stack. Reset RSP to it's base offset (RBP + 0)
		res.add(new Store(new RByte(Reg64.RSP, Reg64.RBP),source));
		//we also need to pop and restore the old RBP
		res.add(new Pop(Reg64.RBP, source));

		//return!
		res.add(new Ret(source)); //I'm just gonna pop the parameters during method call
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

		res.code.addAll(doReturn(stmt,arg));
		return res;
	}

	@Override
	public CodegenResult visitForStmt(ForStmt stmt, CodeGenArg arg) {
		//skeleton:
		//  eval(init)
		//label cond:
		//  eval(cond)
		//  jump_if_false(end)
		//  eval(body)
		//label continue:
		//  [reset stack offset, except for the init()]
		//  eval(inc)
		//  jump(cond)
		//label end:
		//  [reset stack offset]
		//

		Instruction cont = arg.loopContinue;
		Instruction brk = arg.loopBreak; //store for later. During eval(body), these will be set to cond and end respectively.
		boolean fil = arg.finallyInsideLoop;
		arg.finallyInsideLoop = false; //innermost block is now a loop

		int outerOffset = arg.nextOffset;
		CodegenResult res = stmt.initStmt.visit(this, arg); //eval(init)
		
		int innerOffset = arg.nextOffset;
		CodegenResult comp = stmt.compExp.visit(this,arg); //eval(cond)
		arg.loopContinue = comp.code.get(0);
		res.addAll(comp);

		Instruction finalJump = new Jmp(arg.loopContinue, false, false, stmt);
		// arg.loopBreak = finalJump;

		res.code.add(new Cmp(new RByte(Reg64.R8,true),0,stmt));
		res.code.add(new CondJmp(Condition.E, arg.loopBreak, false, true, stmt)); //jump_if_false(end)
		
		
		arg.nextOffset = innerOffset;
		InstructionList continueBlock = new InstructionList();
		continueBlock.add(setStack(innerOffset, stmt)); // [reset stack offset, except for the init()]
		continueBlock.addAll(stmt.incStmt.visit(this,arg).code); //eval(inc)
		continueBlock.add(finalJump); //jump(cond)

		Instruction finalReset = setStack(outerOffset, stmt);

		arg.loopContinue = continueBlock.first();
		arg.loopBreak = finalReset;
		res.addAll(stmt.body.visit(this,arg)); //eval(body)

		res.code.addAll(continueBlock);
		res.code.add(finalReset);

		arg.loopContinue = cont;
		arg.loopBreak = brk;
		arg.finallyInsideLoop = fil;
		return res;
	}

	@Override
	public CodegenResult visitForEachStmt(ForEachStmt stmt, CodeGenArg arg) {
		Instruction cont = arg.loopContinue;
		Instruction brk = arg.loopBreak; //store for later. During eval(body), these will be set to cond and end respectively.
		boolean fil = arg.finallyInsideLoop;
		arg.finallyInsideLoop = false; //innermost block is now a loop
		
		//TODO Needs Iterable conventions - much later
		
		
		arg.loopContinue = cont;
		arg.loopBreak = brk;
		arg.finallyInsideLoop = fil;
		return null;
	}

	@Override
	public CodegenResult visitSwitchStmt(SwitchStmt stmt, CodeGenArg arg) {
		//skeleton:
		// 	eval(target)
		//	[per case block]
		//  eval(target == case1_val)
		//  jump_if_equal(case1)
		/// eval(target == case2_val)
		//  jump_if_equal(case2)
		//  ...
		//  jump(default) or jump(end) 
		//label case1:
		//  eval(case1)
		//label case2:
		//  eval(case2)
		//  ...
		//[if default exists]
		//label default:
		//  eval(default)
		//label end:
		//	[reset stack pointer]

		Instruction outerBreak = arg.loopBreak;
		int origOffset = arg.nextOffset;

		CodegenResult res = stmt.target.visit(this,arg);
		RByte compByte; //specify register size
		switch (stmt.switchType){
			case ENUM: //enum value is an integer stored at ptr+8
				res.code.add(new Load(new RByte(Reg64.R8,EnumElement.valueOffset,Reg64.R8), stmt));
				compByte = new RByte(Reg64.R8,true); //R8 - 8 byte register
				break;
			case INT:
				compByte = new RByte(Reg64.R8,true); //R8 - 8 byte register
				break;
			case CHAR:
				compByte = new RByte(Reg8.R8B, true); //R8B - 1 byte register
				break;
			case STRING:
			default:
				throw new UnsupportedOperationException("String switch-case not supported");
			

		}

		Instruction last = setStack(origOffset, stmt); //RBP + offset -> origOffset, clear any locals from case blocks
		arg.loopBreak = last;

		CodegenResult blocks = new CodegenResult();
		InstructionList conditions = new InstructionList();
		CaseBlock current = stmt.firstCase;
		Instruction defaultInst = null;
		while (current != null){
			CodegenResult block = new CodegenResult(visitList(current.caseBody,arg));
			blocks.addAll(block);

			if (current.literal == null){
				//default block, no condition necessary
				defaultInst = block.code.first();
			}
			int immediate;
			switch (current.literal.kind) {
				case intLiteral:
					immediate = Integer.parseInt(current.literal.spelling);
					break;
				case charLiteral:
					immediate = ((CharLiteral)current.literal).value();
					break;
				case id: //enum constant
					immediate = ((EnumElement)((Identifier)current.literal).refDecl).enumIndex;
					break;
				
				case stringLiteral:
				default:
					throw new UnsupportedOperationException();
			}
			
			if (stmt.switchType != SwitchType.STRING){ //simple comparison
				conditions.add(new Cmp(compByte,immediate,current));
				conditions.add(new CondJmp(Condition.E, block.code.first(),false,false,current));
			} else {
				
			}
		}
		if (defaultInst != null){
			conditions.add(new Jmp(defaultInst,false,false,stmt)); //jump to default block, wherever it is
		} else {
			conditions.add(new Jmp(last,false,false,stmt)); //jump to end of switch
		}

		//assemble code sections

		res.code.addAll(conditions);
		res.addAll(blocks);
		res.code.add(last);
		arg.nextOffset = origOffset;


		arg.loopBreak = outerBreak;

		return res;
	}

	public InstructionList doThrow(AST source, CodeGenArg arg){
		InstructionList res = new InstructionList();
		if (arg.currentHandler == null){
			//reset stack pointer
			res.add(new Store(new RByte(Reg64.RSP, Reg64.RBP),source));
			
			//restore old base pointer
			res.add(new Pop(Reg64.RBP, source));

			//return!
			res.add(new Ret(source)); //pop params during method call

		} else {
			res.add(new Jmp(arg.currentHandler,false,false,source));
		}
		return res;
	}

	@Override
	public CodegenResult visitThrowStmt(ThrowStmt throwStmt, CodeGenArg arg) {
		/* PLAN FOR EXCEPTION HANDLING */
		//Current exception is stored in R14. During normal execution, it is null (value 0)
		//after every callexpression, a check is made if the exception is null. If it's not,
		//jump to the current exception handler
		//The location of the current exception handler is a stored in CodeGenArg. if the call is in 
		//a try block, it jumps to that block's handler; otherwise, it returns and the next method deals with it.

		CodegenResult res = throwStmt.exp.visit(this,arg);
		res.code.add(new Store(new RByte(Reg64.R14,Reg64.R8), throwStmt));

		res.code.addAll(doThrow(throwStmt, arg));
		return res;
	}

	public enum FinallyAction { //NOTE: "Throw" is not included because R14 would be a pointer to the exception instance
		NONE(0),
		RETURN(0b01),
		BREAK(0b10), //second bit flag means loop-based for easier
		CONTINUE(0b11);
		public int code;
		FinallyAction(int code){
			this.code = code;
		}
	}

	@Override
	public CodegenResult visitTryCatchFinallyStmt(TryCatchFinallyStmt stmt, CodeGenArg arg) {
		//Skeleton:
		//  eval(try block)
		//  jump(finally) or jump(end)
		//label handler: (jumped to by callexpr or throwstmt)
		//  [RESET STACK POINTER TO PRE-TRY CONDITIONS]
		//	for each exception type:
		//		if current exception (R15) instance of exception type:
		//			jump to catch{i}
		//	if not jumped, jump to the next handler up (or return if no handlers)
		//label catch1:
		//  eval(catch1)
		//  jump(finally) or jump(end)
		//label catch2:
		//  eval(catch2)
		//  jump(finally) or jump(end)
		// ...
		//label finally:
		//  [RESET STACK POINTER TO PRE-TRY CONDITIONS]
		//  eval(finally)
		//  now evaluate the finally based on the type (details below)
		//label end:
		//[if !finally: RESET STACK POINTER TO PRE-TRY CONDITIONS]


		Instruction outerhandler = arg.currentHandler;
		Instruction outerfinally = arg.currentFinally;
		boolean fil = arg.finallyInsideLoop;
		

		int origOffset = arg.nextOffset;
		
		InstructionList fin = new InstructionList();
		if (stmt.finallyBlock != null){
			//TODO: SCOPES
			//--MAKE SURE THAT THESE OUT OF ORDER VISITS DON'T BREAK THE STACK OFFSET (shouldn't because the local variables don't trasnfer)
			//--MAKE BLOCK STATEMENTS / SCOPE CLOSURES DEALLOCATE LOCAL VARS (**AND DECREMENT ARG**)
			//--MAKE SURE ALL THAT WORKS WITH EXCEPTION THROWING.... ;((
			
			//clear stack from *any* internal shenanigans (handy, this one!)
			fin.add(setStack(origOffset, stmt)); //RBP + offset -> origOffset, clear any locals
			//we need to save R14 and R8 (for return), push onto the stack temporarily
			fin.add(new Push(Reg64.R14, stmt));
			fin.add(new Push(Reg64.R8,stmt));
			arg.getStackOffset(); //R14 can be a pointer, we've pushed the whole thing. Doesn't depend on type
			arg.getStackOffset(); //R8 can also be a pointer. TODO: Optimize for return type?
			int pre = arg.nextOffset;
			fin.addAll(stmt.finallyBlock.visit(this,arg).code);
			//since it's a block statement, should be the same offset after 
			if (arg.nextOffset != pre){
				throw new UnsupportedOperationException("what");
			}

			//then pop R14 and R8
			fin.add(new Pop(Reg64.R8,stmt));
			fin.add(new Pop(Reg64.R14,stmt));
			arg.nextOffset = origOffset;
			
			//evaluate the finally, kinda complicated
			//Note: exception/interrupt data will be in R14:
			// if exception thrown: R14 = pointer to exception object
			// if break/continue/return: R14 = low int, see FinallyAction
			// if no interrupt (try/catch block finished normally): 0
			//
			// if this finally is inside of another trycatchfinally, all four interrupts will go to the same place
			// if this finally is inside a loop inside of a trycatchfinally, break and continue will jump to loop
			//		but throw and return will go to the outer finally
			// if this finally is inside of a loop inside of no trycatchfinally, break and continue will jump to loop
			//		but throw and return will return as normal
			// if this finally isn't inside of anything, all four interrupts will do their own thing
			RByte r14 = new RByte(Reg64.R14,true);
			InstructionList lcond = new InstructionList();
			lcond.add(new Cmp(r14,FinallyAction.CONTINUE.code, stmt));
			lcond.add(new CondJmp(Condition.E,arg.loopContinue,false,false,stmt));
			lcond.add(new Cmp(r14,FinallyAction.BREAK.code,stmt));
			lcond.add(new CondJmp(Condition.E,arg.loopBreak,false,true,stmt)); //break is after

			if (outerfinally != null){
				if (fil || arg.loopBreak == null){ //all four interrupts get forwarded to the outer finally
					fin.add(new Cmp(r14,0,stmt));
					fin.add(new CondJmp(Condition.NE,outerfinally,false,false,stmt)); //if not zero, jump away
				} else { 
					//fil is false, so there's a loop in the outer finally. 
					//Forward break and continue to the loop, but everything else to the outer finally
					//TODO: can this be further optimized with special instructions?
					fin.addAll(lcond);
					fin.add(new Cmp(r14,FinallyAction.NONE.code,stmt));
					fin.add(new CondJmp(Condition.NE, outerfinally, false, false, stmt)); //if nonzero, return or throw, so forward to outer finally
					//if zero, just let it fall through!
				}
			} else {
				if (arg.loopBreak != null) { //loop but no trycatchfinally
					fin.addAll(lcond);
				} //but if no loop, break/continue statements shouldn't exist, so we don't need to consider them at all
				
				//now: return/throw have to do the right thing
				//skeleton:
				//  jump_if_NONE(after)
				//  jump_if_not_RETURN(throw)
				//  eval(return)
				//label throw:
				//  eval(throw)
				//label after:

				InstructionList throwBlock = doThrow(stmt, arg);
				Instruction after = throwBlock.last();

				fin.add(new Cmp(r14,FinallyAction.NONE.code, stmt));
				fin.add(new CondJmp(Condition.E, after,false,true, stmt)); //jump to after throwblock
				fin.add(new Cmp(r14, FinallyAction.RETURN.code,stmt));
				fin.add(new CondJmp(Condition.NE, throwBlock.first(),false,false, stmt)); //jump to start of throw block
				fin.addAll(doReturn(stmt, arg)); //return block; includes Ret, execution jumps
				fin.addAll(throwBlock);
			}
			arg.currentFinally = fin.first();
			arg.finallyInsideLoop = true; //didn't apply to anything *in* the finally block, but does to stuff in the try & catch blocks
		} else {
			fin.add(setStack(origOffset, stmt)); //RBP + offset -> origOffset, clear any locals from catch blocks
		}

		List<InstructionList> catchBlocks = new ArrayList<InstructionList>();
		for (CatchBlock c : stmt.catchBlocks){
			InstructionList block = new InstructionList();
			//push exception as local variable
			block.add(new Push(Reg64.R14,c.exception));
			c.exception.basePointerOffset = arg.getStackOffset();
			
			block.addAll(c.statement.visit(this,arg).code);
			
			//pop local variable, restore stack
			block.add(new Pop(Reg64.RAX,c));
			arg.nextOffset = origOffset; 

			block.add(new Jmp(fin.first(), false, false, stmt)); //whether or not there is a finally block, always jump to it
			catchBlocks.add(block);
		}
		
		//hardest part: assemble handler
		InstructionList handler = new InstructionList();
		if (catchBlocks.size() > 0){ //if no catch blocks, no handler
			handler.add(setStack(origOffset, stmt)); //RBP + offset -> origOffset, clear any locals from try block
			//R14 is pointer to exception object. Load pointer to static decl (type)
			handler.add(new Load(new RByte(Reg64.R14,0,Reg64.R8), stmt));
			for (int i = 0; i < catchBlocks.size(); i++){
				//TODO: If instanceof is improved, do this one too! 
				//For now, just check that the static pointer of the class checked is the same as that stored in the instance
				handler.add(new Lea(new RByte(Reg64.R15,stmt.catchBlocks.get(i).exception.type.typeDeclaration.getStaticOffset(),Reg64.R9),stmt)); //R15 + offset -> R9
				handler.add(new Cmp(new RByte(Reg64.R8,Reg64.R9),stmt));
				handler.add(new CondJmp(Condition.E, catchBlocks.get(i).first(),false,false, stmt));
			} 
			//if all the handlers fail: if there's a finally, fall through to that and it will dispatch. 
			//Otherwise, jump to the next handler:
			if (stmt.finallyBlock == null){
				handler.addAll(doThrow(stmt, arg));
			}
			arg.currentHandler = handler.first();
		} else if (stmt.finallyBlock != null) {
			arg.currentHandler = arg.currentFinally; //even if we can't handle the exception, if there's a finally, we have to visit it
		} //if we have neither catches nor finally, currentHandler will be the outer handler (arg.currentHandler's current value)

		//finally: Try block
		InstructionList tryBlock = stmt.tryBlock.visit(this,arg).code;
		tryBlock.add(new Jmp(fin.first(),false,false,stmt)); //just like the end of catch block, jump to finally if exists or end if not

		CodegenResult res = new CodegenResult(); //PUT IT ALL TOGETHER
		res.code.addAll(tryBlock);
		res.code.addAll(handler);
		res.code.addAll(catchBlocks);
		res.code.addAll(fin);

		//reset all to old values
		arg.currentHandler = outerhandler;
		arg.currentFinally = outerfinally;
		arg.finallyInsideLoop = fil;
		
		return res;
	}

	@Override
	public CodegenResult visitBreakStmt(BreakStmt breakStmt, CodeGenArg arg) {
		if (arg.loopBreak == null){ //not inside loop
			throw new CompilerError("Break statement outside of loop body");
		}
		CodegenResult res = new CodegenResult();
		if (arg.currentFinally == null || !arg.finallyInsideLoop){
			res.code.add(new Jmp(arg.loopBreak, false,false, breakStmt));
		} else {
			res.code.add(new LoadImmediate(Reg64.R14, FinallyAction.BREAK.code, breakStmt));
			res.code.add(new Jmp(arg.currentFinally, false, false, breakStmt));
		}
		return res;
	}
	@Override
	public CodegenResult visitContinueStmt(ContinueStmt continueStmt, CodeGenArg arg) {
		if (arg.loopContinue == null){ //not inside loop
			throw new CompilerError("Continue statement outside of loop body");
		}
		CodegenResult res = new CodegenResult();
		if (arg.currentFinally == null || !arg.finallyInsideLoop){
			res.code.add(new Jmp(arg.loopContinue, false,false, continueStmt));
		} else {
			res.code.add(new LoadImmediate(Reg64.R14, FinallyAction.CONTINUE.code, continueStmt));
			res.code.add(new Jmp(arg.currentFinally, false, false, continueStmt));
		}
		return res;
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
		RByte b89_m32 = new RByte(Reg32.R8D,Reg32.R9D);
		RByte b89_m8 = new RByte(Reg8.R8B,Reg8.R9B);
		switch (op.spelling) {
			case "+":
				res.code.add(new Add(b89,op));
				break;
			case "-":
				res.code.add(new Sub(b89,op)); //R8 (left) - R9 (right) -> R8 (out)
				break;
			case "&&":
				res.code.add(new And(b89_m8,op));
				break;
			case "||":
				res.code.add(new Or(b89_m8,op));
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
				switch (expr.left.returnType.getTypeSize()) { //select cmp size
					case 8: //8 byte
						res.code.add(new Cmp(b89,op));
						break;

					case 4: //4 byte
						res.code.add(new Cmp(b89_m32,op));
						break;

					case 1: //1 byte
						res.code.add(new Cmp(b89_m8,op));
				
					default:
						throw new UnsupportedOperationException();
				}
				res.code.add(new SetCond(Condition.getCond(expr.operator),Reg8.R8B, op));
				// Instruction setFalse = new LoadImmediate(Reg64.R8, 0,op);
				// Instruction setTrue = new LoadImmediate(Reg64.R8, 1,op);
				// res.code.add(new CondJmp(Condition.getCond(expr.operator), setTrue,true,false,op));
				// res.code.add(setFalse);
				// res.code.add(new Jmp(setTrue,true,true,op)); //jump to **after** setTrue (next instruction)
				// res.code.add(setTrue);
				break;
			default:
				break;
		}
		return res;
	}


	@Override
	public CodegenResult visitInstanceOf(InstanceOfExpression expr, CodeGenArg arg) {
		//NOTE: Instanceof is borked because I *really* don't want to do inheritance.
		//However, I *can* check to see that a class's static pointer matches the type's
		CodegenResult res = expr.expr.visit(this,arg); //R8 = object pointer
		res.code.add(new Load(new RByte(Reg64.R8,0,Reg64.R8),expr)); //[R8+0]->R8
		res.code.add(new Lea(new RByte(Reg64.R15,expr.type.typeDeclaration.getStaticOffset(),Reg64.R9),expr)); //R9 = location
		res.code.add(new Cmp(new RByte(Reg64.R8,Reg64.R9),expr)); //check if pointers are the sames
		res.code.add(new SetCond(Condition.E,Reg8.R8B, expr));
		return res;
	}

	public InstructionList doNewObject(TypeDenoter objType, ConstructorDecl cd, ExprList args, AST source, CodeGenArg arg){
		InstructionList res = new InstructionList();
		res.addAll(makeMalloc(source)); //allocate space on the heap for the class
		//Class pointer is in RAX. Populate class body!
		//We're gonna do some complex operations that use RAX, so store it in R9 for now
		res.add(new Store(new RByte(Reg64.R9,Reg64.RAX),source));

		//Zero out the memory; just do it in 8byte chunks, an extra 7 doesn't matter(?)
		RegSize zeroSize = RegSize.m64;
		//clear the DF flag, make sure STOS will go the correct direction (WHICH IS?)
		res.add(new Cld(source));
		//set RAX to zero (-> write zeros)
		res.add(new Xor(new RByte(Reg64.RAX,Reg64.RAX),source));
		//set RDI to start address (currently in R9)
		res.add(new Store(new RByte(Reg64.RDI,Reg64.R9),source));
		//how many bytes do we need to write?
		int numStores = (int)(Math.ceil(objType.typeDeclaration.instanceSize / (1.0 * zeroSize.size)));
		System.out.println("numstores: " + numStores);
		//set RCX to the number of operations we need to do
		res.add(new StoreImmediate(new RByte(Reg64.RCX,true), numStores,source));
		//zero the memory!
		res.add(new RepStos(zeroSize,source));

		//I don't know if this is necessary??? but let's put a pointer to the static declarator in the class header
		res.add(new Lea(new RByte(Reg64.R15,objType.typeDeclaration.getStaticOffset(),Reg64.R8),source));
		res.add(new Store(new RByte(Reg64.R9,0,Reg64.R8),source));

		//initializers!
		//the only instance members that need to be initialized are fields. Remember that the whole thing was zero'd beforehand!
		for (FieldDecl fd : objType.typeDeclaration.fields){
			if (fd.isStatic()) {continue;}
			if (fd.initializer != null){
				res.add(new Push(Reg64.R9,source)); //push class pointer so it's saved
				res.addAll(fd.initializer.visit(this,arg).code);
				res.add(new Pop(Reg64.R9,source));
				res.add(new Store(new RByte(Reg64.R9, fd.basePointerOffset, Reg64.R8),source)); //[ptr+offset] = R8
			}
		}

		//call constructor!
		if (cd != null){
			//we can do something clever. We'll have to push the pointer to store it, so use the pop to transfer to R8
			res.add(new Push(Reg64.R9,source));
			//now call the constructor:
			res.addAll(doCallExpr(Reg64.R9, cd, args, source, arg)); //use the existing this in R9
			//R8 and R9 are clobbered, but it's still pushed
			res.add(new Pop(Reg64.R8, source)); 
			//and now the class pointer is in R8
		} else {
			//Finally, copy class pointer (R9) to R8 and return it
			res.add(new Store(new RByte(Reg64.R8,Reg64.R9),source));
		}
		return res;
	}

	@Override
	public CodegenResult visitNewObjectExpr(NewObjectExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		CodegenResult res = new CodegenResult();
		res.code.addAll(doNewObject(expr.returnType, expr.constructor, expr.args, expr, arg));
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
	public CodegenResult visitCharLiteral(CharLiteral literal, CodeGenArg arg) {
		int code = (int)literal.value();
		CodegenResult res = new CodegenResult();
		res.code.add(new LoadImmediate(Reg64.R8, code, literal));
		return res;
	}

	@Override
	public CodegenResult visitNullLiteral(NullLiteral literal, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();
		res.code.add(new LoadImmediate(Reg64.R8, 0, literal)); //null pointer is value 0
		return res;
	}

	@Override
	public CodegenResult visitIntLiteral(IntLiteral num, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();
		res.code.add(new LoadImmediate(Reg64.R8, Integer.parseInt(num.spelling),num));
		return res;
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
	public CodegenResult visitBooleanLiteral(BooleanLiteral bool, CodeGenArg arg) {
		CodegenResult res = new CodegenResult();
		res.code.add(new LoadImmediate(Reg64.R8, (Boolean.parseBoolean(bool.spelling) ? 1 : 0), bool));
		return res;
	}

	@Override
	public CodegenResult visitLiteralExpr(LiteralExpr expr, CodeGenArg arg) {
		arg.getAssign(expr);
		return expr.lit.visit(this, arg);
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

	/**
	 * ASSUMES "THIS" IS ALREADY IN thisReg
	 */
	public InstructionList doCallExpr(Reg64 thisReg, MethodDecl md, ExprList args, AST source, CodeGenArg arg){
		InstructionList res = new InstructionList();
		//RSP = THIS
		//RSP+8 = ARG1
		//RSP+16 = ARG2
		//ETC
		//then, when we push, ret gets put into what is right now RSP-8

		int argsize = 8*(args.size()+1); //TODO: for more accurate object sizes
		res.add(new Sub(new RByte(Reg64.RSP,true),argsize,source));

		//this pointer loaded into R8, store into RSP+8
		res.add(new Store(new RByte(Reg64.RSP,0,thisReg),source)); //[RSP] = R8

		//eval arguments, store
		// int offset = 8;
		for (int i = 0; i < args.size(); i++){
			int offset = 8*i + 8; //starts at RSP + 8
			res.addAll(args.get(i).visit(this,arg).code); //eval argument expression, stored in R8
			res.add(new Store(new RByte(Reg64.RSP,offset,Reg64.R8),source)); //[RSP+offset] = R8
		}

		//okay, now args are set up. Calling the method will push return address; callee is responsible for pushing, changing, and restoring RBP
		res.add(new Call(md,source));

		//Ok, method called. We get back here when the result has returned. Return value (if it exists) will be in R8; 
		//same as return value of this expression. 

		//"pop" arguments
		res.add(new Add(new RByte(Reg64.RSP,true),argsize,source));
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
		
		res.code.addAll(doCallExpr(Reg64.R8,method,expr.argList,expr, arg));
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

	private InstructionList makeFileOpen(RByte filenamePointer){
		//syscall:
		//2 -> RAX (fileOpen)
		//pointer to filename string -> rdi
		//file open flags -> rsi
		//file permission flags -> rdx
		//syscall
		return null;
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
	public CodegenResult visitPackageDecl(PackageReference packageDecl, CodeGenArg arg) {
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
	public CodegenResult visitInterfaceDecl(InterfaceDecl interfaceDecl, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitInterfaceDecl'");
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
	public CodegenResult visitProgram(Program program, CodeGenArg arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitProgram'");
	}



}
