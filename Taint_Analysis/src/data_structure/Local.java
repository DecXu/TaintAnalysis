package data_structure;

public class Local extends InfoItem {
	private final String varaible; 

	public Local(String var){
		this.varaible = var;
	}
	@Override
//	public String toString() {
//		// TODO Auto-generated method stub
//		StringBuilder builder = new StringBuilder();
//		builder.append("<").append(ItemType.P.toString()).append(":").append(this.varaible).append(">");
//		return builder.toString();
//	}

	public String toSimplyString() {
		return this.varaible;
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		int result = 3;
		result = 37 * result + this.varaible.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return (o instanceof Local) && (((Local) o).varaible.equals(this.varaible));
	}
	public String getVaraible() {
		return varaible;
	}

	
}
