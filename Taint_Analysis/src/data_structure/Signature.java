package data_structure;

import java.util.HashSet;
import java.util.Set;

import soot.SootMethod;

public class Signature {
	private final Set<InfoItem> preSet;
//	private final Set<InfoItem> postSet;
	private final Set<InfoItem> genSet;
	private final Set<InfoItem> killSet;
	
	private final Set<SootMethod> predecessors;
	
	public Signature(){
		this.preSet = new HashSet<InfoItem>();
		this.genSet = new HashSet<InfoItem>();
		this.killSet = new HashSet<InfoItem>();
		this.predecessors = new HashSet<SootMethod>();
	}

	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("preSet: ").append(preSet.toString()).append("\ngenSet: ").append(genSet.toString()).append("\nkillSet: ").append(killSet.toString())
			.append("\npredecessors: ").append(predecessors.toString());
		return builder.toString();
	}
	public String toStringWithoutMethod(){
		StringBuilder builder = new StringBuilder();
		builder.append("preSet: ").append(preSet.toString()).append("\ngenSet: ").append(genSet.toString()).append("\nkillSet: ").append(killSet.toString());
		return builder.toString();
	}
	
	public Set<InfoItem> getPreSet() {
		return preSet;
	}

	public Set<InfoItem> getGenSet() {
		return genSet;
	}

	public Set<InfoItem> getKillSet() {
		return killSet;
	}

	public Set<SootMethod> getPredecessors() {
		return predecessors;
	}

}