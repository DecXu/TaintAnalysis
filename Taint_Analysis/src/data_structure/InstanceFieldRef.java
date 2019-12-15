package data_structure;


public class InstanceFieldRef extends InfoItem 
{
	private final String callee;
	private final String field;
	
	public InstanceFieldRef(String callee, String field)
	{
		this.callee = callee;
		this.field = field;
	}

	@Override
//	public String toString() {
//		// TODO Auto-generated method stub
//		StringBuilder builder = new StringBuilder();
//		builder.append("<").append(ItemType.F).append(":").append(this.callee).append(".").append(this.field).append(">");
//		return builder.toString();
//	}
	
	public String toSimplyString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append(this.callee).append(".").append(this.field);
		return builder.toString();
	}


	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((callee == null) ? 0 : callee.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
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
		InstanceFieldRef other = (InstanceFieldRef) obj;
		if (callee == null) {
			if (other.callee != null)
				return false;
		} else if (!callee.equals(other.callee))
			return false;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		return true;
	}

	public String getCallee() 
	{
		return callee;
	}

	public String getField() 
	{
		return field;
	}
	

}
