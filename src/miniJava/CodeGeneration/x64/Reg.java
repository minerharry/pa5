package miniJava.CodeGeneration.x64;

public interface Reg {
	public enum RegSize{
		m8(1),
		m16(2),
		m32(4),
		m64(8);
		public int size;
		private RegSize(int size){
			this.size = size;
		}

	}
	public int getIdx();
	public RegSize getSize();
	
	default public int size(){
		return getSize().size;
	}
	
	public static Reg RegFromIdx(int idx) {
		return null;
	}
}
