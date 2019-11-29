package data_structure;


public class StaticFieldRef extends InfoItem {
	private final String callee;
	private final String field;
	
	public StaticFieldRef(String callee, String field){
		this.callee = callee;
		this.field = field;
	}

	@Override
//	public String toString() {
//		// TODO Auto-generated method stub
//		StringBuilder builder = new StringBuilder();
//		builder.append("<").append(ItemType.S).append(":").append(this.callee).append(".").append(this.field).append(">");
//		return builder.toString();
//	}

	public String toSimplyString() {
		// TODO Auto-generated method stub
		StringBuilder builder = new StringBuilder();
		builder.append(this.callee).append(".").append(this.field);
		return builder.toString();
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		int result = 3;
		result = 37 * result + this.callee.hashCode();
		result = 37 * result + this.field.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return (o instanceof StaticFieldRef) && (((StaticFieldRef) o).callee.equals(this.callee)) && (((StaticFieldRef) o).field.equals(this.field));
	}

	public String getCallee() {
		return callee;
	}

	public String getField() {
		return field;
	}
	

}