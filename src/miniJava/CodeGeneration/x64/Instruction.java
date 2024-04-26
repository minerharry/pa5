package miniJava.CodeGeneration.x64;

import java.io.ByteArrayOutputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Instruction {
	protected ByteArrayOutputStream opcodeBytes = new ByteArrayOutputStream();
	protected ByteArrayOutputStream immBytes = new ByteArrayOutputStream();
	protected boolean rexW = false;
	protected boolean rexR = false;
	protected boolean rexX = false;
	protected boolean rexB = false;
	public int startAddress;
	public int listIdx;
	private int _size = -1;

	public AST sourceAst;
	public SourcePosition posn;

	public Instruction(AST source){
		sourceAst = source;
		if (source != null)
			posn = source.posn;
	}
	
	// caching could be done better here, instructions are "kinda" immutable
	/**Size of instruction in bytes */
	public int size() {
		if( _size == -1 )
			_size = getBytes().length;
		return _size;
	}
	
	public byte[] getBytes() {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		Byte prefix = getPrefix();
		if (prefix != null)
			b.write(prefix);
		Byte rex = getRex();
		if( rex != null )
			b.write(rex);
		if( opcodeBytes.size() > 0 )
			x64.writeBytes(b, opcodeBytes.toByteArray());
		if( immBytes.size() > 0 )
			x64.writeBytes(b, immBytes.toByteArray() );
		byte[] retBytes = b.toByteArray();
		_size = retBytes.length;
		return retBytes;
	}
	
	private Byte getRex() {
		if( !( rexW || rexX || rexB || rexR ) )
			return null;
		return (byte)((4 << 4) | (rexW ? 1 << 3 : 0) | (rexR ? 1 << 2 : 0) | (rexX ? 1 << 1 : 0) | (rexB ? 1 : 0));
	}

	/**
	 * For instructions like REP whose prefix occurs *before* the Rex flags
	 * @return Instruction prefix byte
	 */
	protected Byte getPrefix() {
		return null; 
	}
	
	protected void importREX(RByte rm64) {
		rexW = rexW || rm64.getRexW();
		rexR = rexR || rm64.getRexR();
		rexX = rexX || rm64.getRexX();
		rexB = rexB || rm64.getRexB();
	}

	//VERY IMPORTANT: THE INSTRUCTION SIZE NEEDS TO BE SET BEFORE THE FINAL PATCH
	public void final_patch(InstructionList asm){
		
	};
}
