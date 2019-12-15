package data_structure;

public class Local extends InfoItem 
{
	private final String varaible; 

	public Local(String var)
	{
		this.varaible = var;
	}
	@Override
//	public String toString() {
//		// TODO Auto-generated method stub
//		StringBuilder builder = new StringBuilder();
//		builder.append("<").append(ItemType.P.toString()).append(":").append(this.varaible).append(">");
//		return builder.toString();
//	}

	public String toSimplyString() 
	{
		return this.varaible;
	}
	
	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((varaible == null) ? 0 : varaible.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Local other = (Local) obj;
		if (varaible == null) {
			if (other.varaible != null)
				return false;
		} else if (!varaible.equals(other.varaible))
			return false;
		return true;
	}
	public String getVaraible() 
	{
		return varaible;
	}

	
}
