package taintprocess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;
import data_structure.Local;
import data_structure.Signature;

public class InterTaintAnalysis {
	final Chain<SootClass> classes;
	final Map<SootMethod, Signature> signatures;
	final Set<SootMethod> changedMethods;
	
	public InterTaintAnalysis(Chain<SootClass> classes) {
		// TODO Auto-generated constructor stub
		this.classes = classes;
		this.signatures = new HashMap<SootMethod, Signature>();
		this.changedMethods = new HashSet<SootMethod>();
		
		initialize();
//		System.out.println(signatures.toString());
		doAnalysis();
		
		//print out the final results
		System.out.println("\n\nThe fixed signatures for each method are as follows: \n========================================================\n");
		printSignatures();
		System.out.println("========================================================\n\n");
		System.out.println("\n\nThe taint information within each method is as follows: \n========================================================\n");
		for(SootMethod method: signatures.keySet()){
			IntraTaintAnalysis analysis = new IntraTaintAnalysis(method, new BriefUnitGraph(method.getActiveBody()), signatures, changedMethods);
			Iterator it = new BriefUnitGraph(method.getActiveBody()).iterator();
			System.out.println(method.toString() + "\n--------------------------------------");
			while(it.hasNext()){
				Stmt stmt = (Stmt) it.next();
				System.out.println(analysis.getFlowBefore(stmt));
				System.out.println(stmt.toString());
				System.out.println(analysis.getFlowAfter(stmt));
				System.out.println();
			}
			System.out.println("\n");
		}
		System.out.println("========================================================\n\n");
		
	}
	
	private void initialize(){
		for(SootClass c: classes){
			for(SootMethod m: c.getMethods()){
				final Signature sig;
				if(m.isMain()){
					changedMethods.add(m);
					sig = new Signature();
					sig.getPreSet().add(new Local("@para0"));
				}
				else{
					sig = new Signature();
				}
				signatures.put(m, sig);
			}
		}
	}
	
	public void doAnalysis(){
		while(!changedMethods.isEmpty()){
			SootMethod m = changedMethods.iterator().next();
			changedMethods.remove(m);
			
			IntraTaintAnalysis analysis = new IntraTaintAnalysis(m, new BriefUnitGraph(m.getActiveBody()), signatures, changedMethods);
		}
	}
	
	private void printSignatures(){
		for(SootMethod method: signatures.keySet()){
			System.out.println(method.getSignature());
			System.out.println(signatures.get(method).toStringWithoutMethod());
			System.out.println();
		}
	}
	

}