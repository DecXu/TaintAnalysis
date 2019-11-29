package data_structure;

public abstract class InfoItem 
{
	public static enum ItemType
	{
		P, F, A, S
	}
	public String toString()
	{
		return toSimplyString();
	
	}
	public abstract String toSimplyString();
	public abstract int hashCode();
	public abstract boolean equals(Object o);
}