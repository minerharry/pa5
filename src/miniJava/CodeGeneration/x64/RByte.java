package miniJava.CodeGeneration.x64;

import java.io.ByteArrayOutputStream;

import miniJava.CodeGeneration.x64.Reg.RegSize;

public class RByte {
	private ByteArrayOutputStream _b;
	private boolean rexW = false;
	private boolean rexR = false;
	private boolean rexX = false;
	private boolean rexB = false;
	
	public boolean getRexW() {
		return rexW;
	}
	
	public boolean getRexR() {
		return rexR;
	}
	
	public boolean getRexX() {
		return rexX;
	}
	
	public boolean getRexB() {
		return rexB;
	}
	
	public byte[] getBytes() {
		_b = new ByteArrayOutputStream();
		// construct
		if( rdisp != null && ridx != null && r != null )
			Make(rdisp,ridx,mult,disp,r);
		else if( ridx != null && r != null )
			Make(ridx,mult,disp,r);
		else if( rdisp != null && r != null )
			Make(rdisp,disp,r);
		else if( rm != null && r != null )
			Make(rm,r);
		else if( r != null )
			Make(disp,r);
		else throw new IllegalArgumentException("Cannot determine RByte");
		
		return _b.toByteArray();
	}
	
	private Reg64 rdisp = null, ridx = null;
	private Reg rm = null, r = null;
	private int disp = 0, mult = 0;
	private RegSize size;
	
	public RegSize getSize() {
		return size;
	}

	// [rdisp+ridx*mult+disp],r32/64
	public RByte(Reg64 rdisp, Reg64 ridx, int mult, int disp, Reg r) {
		SetRegR(r);
		SetRegDisp(rdisp);
		SetRegIdx(ridx);
		SetDisp(disp);
		SetMult(mult);
	}
	
	// r must be set by some mod543 instruction set later
	// [rdisp+ridx*mult+disp]
	public RByte(Reg64 rdisp, Reg64 ridx, int mult, int disp) {
		SetRegDisp(rdisp);
		SetRegIdx(ridx);
		SetDisp(disp);
		SetMult(mult);
	}
	
	// [rdisp+disp],r
	public RByte(Reg64 rdisp, int disp, Reg r) {
		SetRegDisp(rdisp);
		SetRegR(r);
		SetDisp(disp);
	}
	
	// r will be set by some instruction to a mod543
	// [rdisp+disp]
	public RByte(Reg64 rdisp, int disp) {
		SetRegDisp(rdisp);
		SetDisp(disp);
	}
	
	// rm64,r64
	public RByte(Reg rm, Reg r) {
		SetRegRM(rm);
		SetRegR(r);
	}
	
	// rm or r
	public RByte(Reg r_or_rm, boolean isRm) {
		if( isRm )
			SetRegRM(r_or_rm);
		else
			SetRegR(r_or_rm);
	}
	
	public int getRMSize() {
		if( rm == null ) return 0;
		return rm.size();
	}
	
	//public RByte() {
	//}

	public void setSize(Reg r){
		if (size == null){
			size = r.getSize();
		} else {
			if (size != r.getSize()){
				throw new IllegalArgumentException("R and RM must be the same register size!");
			}
		}
	}
	
	public void SetRegRM(Reg rm) {
		if( rm.getIdx() > 7 ) rexB = true;
		rexW = rexW || rm instanceof Reg64;
		this.rm = rm;
	}
	
	public void SetRegR(Reg r) {
		if( r.getIdx() > 7 ) rexR = true;
		rexW = rexW || r instanceof Reg64;
		this.r = r;
	}
	
	public void SetRegDisp(Reg64 rdisp) {
		if( rdisp.getIdx() > 7 ) rexB = true;
		this.rdisp = rdisp;
	}
	
	public void SetRegIdx(Reg64 ridx) {
		if( ridx.getIdx() > 7 ) rexX = true;
		this.ridx = ridx;
	}
	
	public void SetDisp(int disp) {
		this.disp = disp;
	}
	
	public void SetMult(int mult) {
		this.mult = mult;
	}
	
	// public boolean IsRegR_R8() {
	// 	return r instanceof Reg8;
	// }
	
	// public boolean IsRegR_R64() {
	// 	return r instanceof Reg64;
	// }
	
	// public boolean IsRegRM_R8() {
	// 	return rm instanceof Reg8;
	// }
	
	// public boolean IsRegRM_R64() {
	// 	return rm instanceof Reg64;
	// }

	public int RMByte(int mod, Reg r, Reg rm){ //different order to match byte order - IMPORTANT!
		return ( mod << 6 ) | ( getIdx(r) << 3 ) | getIdx(rm);
	}

	public int SIBByte(int ss, Reg index, Reg base){
		return RMByte(ss, index, base); //shorthand
	}
	
	// rm,r
	private void Make(Reg rm, Reg r) {
		int mod = 3;
		
		int regByte = RMByte(mod, r, rm);
		_b.write( regByte ); 
	}
	
	// [rdisp+disp],r
	private void Make(Reg64 rdisp, int disp, Reg r) {
		// TODO: construct the byte and write to _b
		// Operands: [rdisp+disp],r
		int mod;

		if (rdisp == Reg64.RSP){
			//we can't save to RSP + disp without an SIB, but we *can* if we have it. So we need to adapt slightly...
			Make(
				Reg64.RSP, //*base* can be RSP
				null, //no index (will be set to RSP!)
				0, //mult doesn't matter without index
				disp,
				r
			);
			return;
		}

		//No SIB, yes displacement. How many bytes?
		//let's just default to 4...
		mod = 2; //4 bytes displacement

		//write like above
		int regByte = RMByte(mod,r,rdisp);
		_b.write(regByte);

		//write displacement
		x64.writeInt(_b, disp);

	}
	
	// [ridx*mult+disp],r
	private void Make( Reg64 ridx, int mult, int disp, Reg r ) {
		//NO RDISP: set rdisp to null :)
		Make(null, ridx, mult, disp, r);
	}

	// [rdisp+ridx*mult+disp],r
	private void Make( Reg64 rdisp, Reg64 ridx, int mult, int disp, Reg r) {
		if( ridx == Reg64.RSP )
			throw new IllegalArgumentException("Index cannot be rsp");
		if ( ridx == null){ //CUSTOM BEHAVIOR! IF we want to set something to null, let's just pass null and use the special value!
			ridx = Reg64.RSP;
			mult = 1; //make sure it's small
		}
		else if( !(mult == 1 || mult == 2 || mult == 4 || mult == 8) ) //we don't need to check mult if ridx is null
			throw new IllegalArgumentException("Invalid multiplier value: " + mult);

		if(rdisp == Reg64.RBP )
			throw new IllegalArgumentException("RDisp cannot be rbp");
		if (rdisp == null)
			rdisp = Reg64.RBP;
		
		// Operands: [rdisp + ridx*mult + disp], r

		//Yes SIB, yes displacement. 
		int mod, ss;

		mod = 2; //again assume 4-byte displacement
		int regByte = RMByte(mod, r, Reg64.RSP); //RSP indicates include SIB
		_b.write(regByte);

		switch (mult){
			case 1:
				ss = 0;
				break;
			case 2:
				ss = 1;
				break;
			case 4:
				ss = 2;
				break;
			case 8:
				ss = 3;
				break;
			default:
				throw new IllegalArgumentException("Invalid multiplier value: " + mult);
		}

		int SIB = SIBByte(ss, ridx, rdisp); 
		_b.write(SIB);

		//write displacement
		assert mod == 2;
		x64.writeInt(_b, disp);
	}
	
	// [disp],r
	private void Make( int disp, Reg r ) {
		_b.write( ( getIdx(r) << 3 ) | 4 );
		_b.write( ( 4 << 3 ) | 5 ); // ss doesn't matter
		writeInt(_b,disp);
	}
	
	private int getIdx(Reg r) {
		return x64.getIdx(r);
	}
	
	// TODO: This is a duplicate declaration from x64.writeInt
	//  You should remove this, but the reason it is here is so that
	//  you can immediately see what it does, and so you know what
	//  is available to you in the x64 class.
	private void writeInt(ByteArrayOutputStream b, int n) {
		for( int i = 0; i < 4; ++i ) {
			b.write( n & 0xFF );
			n >>= 8;
		}
	}
}
