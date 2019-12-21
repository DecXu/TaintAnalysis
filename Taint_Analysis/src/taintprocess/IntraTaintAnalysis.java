package taintprocess;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.RefLikeType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThisRef;
import soot.toolkits.graph.BriefUnitGraph;

import data_structure.InfoItem;
import data_structure.InstanceFieldRef;
import data_structure.Local;
import data_structure.Signature;
import data_structure.StaticFieldRef;


public class IntraTaintAnalysis extends MyForwardFlowAnalysis
{
	public static final String PARA = "@para";
	public static final String THIS = "@this";
	public static final String RETURN = "@return";
	
	final BriefUnitGraph cfg;
	final SootMethod method;
	final Map<String, Value> formalToLocal;
	final Map<String, String> localToFormal;

	final Map<Unit, HashSet<Unit>> CDSMap;
	final Map<SootMethod, Signature> signatures;
	final Set<SootMethod> changedMethods;

	public IntraTaintAnalysis(SootMethod m, BriefUnitGraph cfg, Map<SootMethod, Signature> sigs, Set<SootMethod> methods) {
		super(cfg);
		
		this.cfg = cfg;
		method = m;
		formalToLocal = new HashMap<String, Value>();
		localToFormal = new HashMap<String, String>();
		
		CDSMap = Collections.unmodifiableMap(new CDA(new BriefUnitGraph(m.getActiveBody())).getCDSMap());
		signatures = sigs;
		changedMethods = methods;
		
		doAnalysis();
		updateSignaturePost();
		
		//output the results
//		Iterator it = cfg.iterator();
//		while(it.hasNext()){
//			Stmt stmt = (Stmt) it.next();
//			System.out.println(this.getFlowBefore(stmt));
//			System.out.println(stmt.toString());
//			System.out.println(this.getFlowAfter(stmt));
//			System.out.println();
//		}
	}

	private void updateSignaturePost() {
		// TODO Auto-generated method stub
		List<Unit> tails = new BriefUnitGraph(method.getActiveBody()).getTails();
		if(tails.size() != 1){
			System.out.println(method.toString());
			System.out.println(tails.toString());
		}
		Set<InfoItem> outSet = new HashSet<InfoItem>();
		Set<String> returns = new HashSet<String>();
		for(Unit tail: tails){
			Stmt stmt = (Stmt) tail;
			outSet.addAll((Set<InfoItem>) this.getFlowAfter(stmt));
			if(stmt instanceof ReturnStmt){
				Value reVal = ((ReturnStmt) stmt).getOp();
				returns.add(reVal.toString());
				//consider the return variable which is an object
				if(reVal.getType() instanceof RefLikeType){
					localToFormal.put(reVal.toString(), RETURN);
					formalToLocal.put(RETURN, reVal);
				}
			}
		}
		
		
		//compute the post set of a method
		Set<InfoItem> preSigSet = signatures.get(method).getPreSet();
		Set<InfoItem> postSigSet = new HashSet<InfoItem>();
		Set<InfoItem> genSigSet = new HashSet<InfoItem>();
		Set<InfoItem> killSigSet = new HashSet<InfoItem>();
		
		for(InfoItem item: outSet){
			InfoItem newItem;
			if(item instanceof Local){
				String variable = ((Local) item).getVaraible();
				//check return variable
				if(returns.contains(variable)){
					newItem = new Local(RETURN);
					if(!preSigSet.contains(newItem)){
						genSigSet.add(newItem);
					}
					postSigSet.add(newItem);
				}
				//check array
				if(localToFormal.containsKey(variable) && (formalToLocal.get(localToFormal.get(variable)).getType() instanceof ArrayType)){
					newItem = new Local(localToFormal.get(variable));
					if(!preSigSet.contains(newItem)){
						genSigSet.add(newItem);
					}
					postSigSet.add(newItem);
				}
			}
			else if(item instanceof InstanceFieldRef){
				String callee = ((InstanceFieldRef) item).getCallee();
				String field = ((InstanceFieldRef) item).getField();
				if(localToFormal.containsKey(callee)){
					newItem = new InstanceFieldRef(localToFormal.get(callee), field);
					if(!preSigSet.contains(newItem)){
						genSigSet.add(newItem);
					}
					postSigSet.add(newItem);
				}
			}
			else if(item instanceof StaticFieldRef){
				if(!preSigSet.contains(item)){
					genSigSet.add(item);
				}
				postSigSet.add(item);
			}
		}
		
		for(InfoItem item: preSigSet){
			if(item instanceof Local){
				postSigSet.add(item);
			}
			//construct the kill set: instanceFieldRef and staticFieldRef
			else{
				if(!postSigSet.contains(item)){
					killSigSet.add(item);
				}
			}
		}
		
		if(!genSigSet.containsAll(signatures.get(method).getGenSet())){
			throw new RuntimeException("gen set error: non-monotone");
		}
		if(!signatures.get(method).getKillSet().containsAll(killSigSet)){
			throw new RuntimeException("kill set error: non-monotone");
		}
		
		if(!genSigSet.equals(signatures.get(method).getGenSet())){
			signatures.get(method).getGenSet().addAll(genSigSet);
			changedMethods.addAll(signatures.get(method).getPredecessors());
		}
		if(!killSigSet.equals(signatures.get(method).getKillSet())){
			signatures.get(method).getKillSet().retainAll(killSigSet);
			changedMethods.addAll(signatures.get(method).getPredecessors());
		}
	}

	@Override
	protected void flowThrough(Object in, Object d, Object out) {
		// TODO Auto-generated method stub
		Set<InfoItem> inSet = (HashSet<InfoItem>) in,
			outSet = (HashSet<InfoItem>) out;
		
		outSet.addAll(inSet);
		Stmt stmt = (Stmt) d;
		
		//compute kill and gen for each statement
		/**
		 * identityStmt: 
		 */
		if(stmt instanceof IdentityStmt){
			flowThroughIdentity(inSet, stmt, outSet);
		}
		/**
		 * AssignStmt:
		 */
		else if(stmt instanceof AssignStmt){
			//contain the invoke expression
			if(stmt.containsInvokeExpr()){
				SootMethod method = stmt.getInvokeExpr().getMethod();
				//application method call, use signature of an application method
				if(method.isConcrete() && !method.isJavaLibraryMethod()){
					flowThroughInvokeStmtAssign(inSet, stmt, outSet);
				}
				// library call or non-concrete call
				else{
					flowThroughAssignStmtNonApplicationInvoke(inSet, stmt, outSet);
				}
			}
			//does not contain invoke expression
			else{
				flowThroughAssignStmtNonApplicationInvoke(inSet, stmt, outSet);
			}
			
		}
		/**
		 * InvokeStmt:
		 */
		else if(stmt instanceof InvokeStmt){
			SootMethod method = stmt.getInvokeExpr().getMethod();
			if(method.isConcrete() && !method.isJavaLibraryMethod()){
				flowThroughInvokeStmtNonAssign(inSet, stmt, outSet);
			}
		}
		/**
		 * ReturnStmt:
		 */
		else if(stmt instanceof ReturnStmt){
			flowThroughReturnStmt(inSet, stmt, outSet);
		}
	}
	
	private void flowThroughIdentity(Set<InfoItem> inSet, Stmt stmt, Set<InfoItem> outSet){
		Set<InfoItem> genSet = new HashSet<InfoItem>(), killSet = new HashSet<InfoItem>();
		
		Value rightOp = ((IdentityStmt) stmt).getRightOp();
		Value leftOp = ((IdentityStmt) stmt).getLeftOp();
		if(rightOp instanceof ThisRef){
			formalToLocal.put(THIS, leftOp);
			localToFormal.put(leftOp.toString(), THIS);
			
			for(InfoItem item: signatures.get(method).getPreSet()){
				if(item instanceof InstanceFieldRef){
					if(((InstanceFieldRef) item).getCallee().equals(THIS)){
						InfoItem newItem = new InstanceFieldRef(leftOp.toString(), ((InstanceFieldRef) item).getField());
						genSet.add(newItem);
					}
				}
				else if(item instanceof Local){
					if(((Local) item).getVaraible().equals(THIS)){
						throw new RuntimeException("callee is tained!");
					}
				}
			}
		}
		else if(rightOp instanceof ParameterRef){
			int index = ((ParameterRef) rightOp).getIndex();
			formalToLocal.put(PARA + index, leftOp);
			localToFormal.put(leftOp.toString(), PARA + index);
			
			for(InfoItem item: signatures.get(method).getPreSet()){
				InfoItem newItem;
				if(item instanceof Local){
					if(((Local) item).getVaraible().equals(PARA + index)){
						newItem = new Local(leftOp.toString());
						genSet.add(newItem);
					}
				}
				else if(item instanceof InstanceFieldRef){
					if(((InstanceFieldRef) item).getCallee().equals(PARA + index)){
						newItem = new InstanceFieldRef(leftOp.toString(), ((InstanceFieldRef) item).getField());
						genSet.add(newItem);
					}
				}
			}
		}
		
		//generate and kill
		outSet.removeAll(killSet);
		outSet.addAll(genSet);
	}
	
	private void flowThroughAssignStmtNonApplicationInvoke(Set<InfoItem> inSet, Stmt stmt, Set<InfoItem> outSet){
		Set<InfoItem> genSet = new HashSet<InfoItem>(), killSet = new HashSet<InfoItem>();
		
		//construct the use set
		Set<InfoItem> useSet = new HashSet<InfoItem>();
		for(ValueBox vb: stmt.getUseBoxes()){
			Value value = vb.getValue();
			InfoItem newItem;
			if(value instanceof soot.jimple.ArrayRef){
				newItem = new Local(((soot.jimple.ArrayRef) value).getBase().toString());
				useSet.add(newItem);
			}
			else if(value instanceof soot.jimple.InstanceFieldRef){
				newItem = new InstanceFieldRef(((soot.jimple.InstanceFieldRef) value).getBase().toString(), ((soot.jimple.InstanceFieldRef) value).getField().toString());
				useSet.add(newItem);
			}
			else if(value instanceof soot.jimple.StaticFieldRef){
				newItem = new StaticFieldRef(((soot.jimple.StaticFieldRef) value).getField().getDeclaringClass().toString(), ((soot.jimple.StaticFieldRef) value).getField().toString());
				useSet.add(newItem);
			}
			else if(value instanceof soot.Local){
				newItem = new Local(value.toString());
				useSet.add(newItem);
			}
		}
		
		//compute the gen and kill set
		useSet.retainAll(inSet);
		if(useSet.isEmpty() && !controlDependenceTainted(stmt)){
			for(ValueBox vb: stmt.getDefBoxes()){
				Value value = vb.getValue();
				InfoItem newItem;
				if(value instanceof soot.jimple.ArrayRef){
					//do not kill array
				}
				else if(value instanceof soot.jimple.InstanceFieldRef){
					newItem = new InstanceFieldRef(((soot.jimple.InstanceFieldRef) value).getBase().toString(), ((soot.jimple.InstanceFieldRef) value).getField().toString());
					killSet.add(newItem);
				}
				else if(value instanceof soot.jimple.StaticFieldRef){
					newItem = new StaticFieldRef(((soot.jimple.StaticFieldRef) value).getField().getDeclaringClass().toString(), ((soot.jimple.StaticFieldRef) value).getField().toString());
					killSet.add(newItem);
				}
				else if(value instanceof soot.Local){
					newItem = new Local(value.toString());
					killSet.add(newItem);
				}
				else{
					System.out.println(stmt.toString());
					throw new RuntimeException("def variable error");
				}
			}
		}
		else{
			for(ValueBox vb: stmt.getDefBoxes()){
				Value value = vb.getValue();
				InfoItem newItem;
				if(value instanceof soot.jimple.ArrayRef){
					newItem = new Local(((soot.jimple.ArrayRef) value).getBase().toString());
					genSet.add(newItem);
				}
				else if(value instanceof soot.jimple.InstanceFieldRef){
					newItem = new InstanceFieldRef(((soot.jimple.InstanceFieldRef) value).getBase().toString(), ((soot.jimple.InstanceFieldRef) value).getField().toString());
					genSet.add(newItem);
				}
				else if(value instanceof soot.jimple.StaticFieldRef){
					newItem = new StaticFieldRef(((soot.jimple.StaticFieldRef) value).getField().getDeclaringClass().toString(), ((soot.jimple.StaticFieldRef) value).getField().toString());
					genSet.add(newItem);
				}
				else if(value instanceof soot.Local){
					newItem = new Local(value.toString());
					genSet.add(newItem);
				}
				else{
					System.out.println(stmt.toString());
					throw new RuntimeException("def variable error");
				}
			}
		}
		
		//generate and kill
		outSet.removeAll(killSet);
		outSet.addAll(genSet);
	}
	
	private void flowThroughInvokeStmtAssign(Set<InfoItem> inSet, Stmt stmt, Set<InfoItem> outSet){
		Set<InfoItem> genSet = new HashSet<InfoItem>(), killSet = new HashSet<InfoItem>();
		
		SootMethod call = stmt.getInvokeExpr().getMethod();
		assert(call.hasActiveBody() == true);
		
		//add the predecessor method caller
		signatures.get(call).getPredecessors().add(method);
		
		//construct the actual-formal and formal-actual mapping
		Map<String, String> actualToFormal = new HashMap<String, String>();
		Map<String, String> formalToActual = new HashMap<String, String>();
		if(!(stmt.getInvokeExpr() instanceof StaticInvokeExpr)){
			String callee = ((ValueBox) stmt.getInvokeExpr().getUseBoxes().get(0)).getValue().toString();
			actualToFormal.put(callee, THIS);
			formalToActual.put(THIS, callee);
		}
		for(int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++){
			String arg = stmt.getInvokeExpr().getArg(i).toString();
			actualToFormal.put(arg, PARA + i);
			formalToActual.put(PARA + i, arg);
		}
		formalToActual.put(RETURN, ((DefinitionStmt) stmt).getLeftOp().toString());
		
		//update the preSet
		Set<InfoItem> newPreSet = new HashSet<InfoItem>();// for debugging
		Set<InfoItem> preSet = signatures.get(call).getPreSet();
		for(InfoItem item: inSet){
			InfoItem newItem;
			if(item instanceof Local){
				String var = ((Local) item).getVaraible();
				if(actualToFormal.containsKey(var)){
					newItem = new Local(actualToFormal.get(var));
//					if(preSet.add(newItem)){
//						changedMethods.add(call);
//					}
					newPreSet.add(newItem);
				}
			}
			else if(item instanceof InstanceFieldRef){
				String callee = ((InstanceFieldRef) item).getCallee();
				if(actualToFormal.containsKey(callee)){
					newItem = new InstanceFieldRef(actualToFormal.get(callee), ((InstanceFieldRef) item).getField());
//					if(preSet.add(newItem)){
//						changedMethods.add(call);
//					}
					newPreSet.add(newItem);
				}
			}
			else if(item instanceof StaticFieldRef){
//				if(preSet.add(item)){
//					changedMethods.add(call);
//				}
				newPreSet.add(item);
			}
		}
//		if(!newPreSet.containsAll(preSet)){
//			throw new RuntimeException("pre set error: non-monotone");
//		}
		if(!preSet.containsAll(newPreSet)){
			preSet.addAll(newPreSet);
			changedMethods.add(call);
		}
		
		//compute the gen and kill set
		for(InfoItem item: signatures.get(call).getKillSet()){
			if(item instanceof InstanceFieldRef){
				killSet.add(new InstanceFieldRef(formalToActual.get(((InstanceFieldRef) item).getCallee()), ((InstanceFieldRef) item).getField()));
			}
			else if(item instanceof StaticFieldRef){
				killSet.add(item);
			}
			else{
				throw new RuntimeException("kill item type error");
			}
		}
		for(InfoItem item: signatures.get(call).getGenSet()){
			if(item instanceof Local){
				genSet.add(new Local(formalToActual.get(((Local) item).getVaraible())));
			}
			else if(item instanceof InstanceFieldRef){
				genSet.add(new InstanceFieldRef(formalToActual.get(((InstanceFieldRef) item).getCallee()), ((InstanceFieldRef) item).getField()));
			}
			else if(item instanceof StaticFieldRef){
				genSet.add(item);
			}
			else{
				throw new RuntimeException("unknown item type");
			}
		}
		//add the control-dependence-tainted return value
		if(controlDependenceTainted(stmt)){
			genSet.add(new Local(((DefinitionStmt) stmt).getLeftOp().toString()));
		}
		
		//generate and kill
		outSet.removeAll(killSet);
		outSet.addAll(genSet);
	}
	
	private void flowThroughInvokeStmtNonAssign(Set<InfoItem> inSet, Stmt stmt, Set<InfoItem> outSet){
		Set<InfoItem> genSet = new HashSet<InfoItem>(), killSet = new HashSet<InfoItem>();
		
		SootMethod call = stmt.getInvokeExpr().getMethod();
		assert(call.hasActiveBody() == true);
		
		//add the predecessor method caller
		signatures.get(call).getPredecessors().add(method);
		
		//construct the actual-formal and formal-actual mapping
		Map<String, String> actualToFormal = new HashMap<String, String>();
		Map<String, String> formalToActual = new HashMap<String, String>();
		if(!(stmt.getInvokeExpr() instanceof StaticInvokeExpr)){
			String callee = ((ValueBox) stmt.getInvokeExpr().getUseBoxes().get(0)).getValue().toString();
			actualToFormal.put(callee, THIS);
			formalToActual.put(THIS, callee);
		}
		for(int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++){
			String arg = stmt.getInvokeExpr().getArg(i).toString();
			actualToFormal.put(arg, PARA + i);
			formalToActual.put(PARA + i, arg);
		}
		
		//update the preSet
		Set<InfoItem> newPreSet = new HashSet<InfoItem>();// for debugging
		Set<InfoItem> preSet = signatures.get(call).getPreSet();
		for(InfoItem item: inSet){
			InfoItem newItem;
			if(item instanceof Local){
				String var = ((Local) item).getVaraible();
				if(actualToFormal.containsKey(var)){
					newItem = new Local(actualToFormal.get(var));
//					if(preSet.add(newItem)){
//						changedMethods.add(call);
//					}
					newPreSet.add(newItem);
				}
			}
			else if(item instanceof InstanceFieldRef){
				String callee = ((InstanceFieldRef) item).getCallee();
				if(actualToFormal.containsKey(callee)){
					newItem = new InstanceFieldRef(actualToFormal.get(callee), ((InstanceFieldRef) item).getField());
//					if(preSet.add(newItem)){
//						changedMethods.add(call);
//					}
					newPreSet.add(newItem);
				}
			}
			else if(item instanceof StaticFieldRef){
//				if(preSet.add(item)){
//					changedMethods.add(call);
//				}
				newPreSet.add(item);
			}
		}
		if(!preSet.containsAll(newPreSet)){
			preSet.addAll(newPreSet);
			changedMethods.add(call);
		}
		
		//compute the gen and kill set
		for(InfoItem item: signatures.get(call).getKillSet()){
			if(item instanceof InstanceFieldRef){
				killSet.add(new InstanceFieldRef(formalToActual.get(((InstanceFieldRef) item).getCallee()), ((InstanceFieldRef) item).getField()));
			}
			else if(item instanceof StaticFieldRef){
				killSet.add(item);
			}
			else{
				throw new RuntimeException("kill item type error");
			}
		}
		for(InfoItem item: signatures.get(call).getGenSet()){
			if(item instanceof Local){
				if(((Local) item).getVaraible().equals(RETURN)){//return but no assign
					continue;
				}
				genSet.add(new Local(formalToActual.get(((Local) item).getVaraible())));
			}
			else if(item instanceof InstanceFieldRef){
				if(((InstanceFieldRef) item).getCallee().equals(RETURN)){//return but no assign
					continue;
				}
				genSet.add(new InstanceFieldRef(formalToActual.get(((InstanceFieldRef) item).getCallee()), ((InstanceFieldRef) item).getField()));
			}
			else if(item instanceof StaticFieldRef){
				genSet.add(item);
			}
			else{
				throw new RuntimeException("unknown item type");
			}
		}
		
		//generate and kill
		outSet.removeAll(killSet);
		outSet.addAll(genSet);
	}
	
	private void flowThroughReturnStmt(Set<InfoItem> inSet, Stmt stmt, Set<InfoItem> outSet){
		Set<InfoItem> genSet = new HashSet<InfoItem>(), killSet = new HashSet<InfoItem>();
		Value reVal = ((ReturnStmt) stmt).getOp();
		if(controlDependenceTainted(stmt)){
			genSet.add(new Local(reVal.toString()));
		}
		//generate and kill
		outSet.removeAll(killSet);
		outSet.addAll(genSet);
	}
	
	
	/**whether stmt is tainted due to the control dependency
	 * @param stmt
	 * @return
	 */
	private boolean controlDependenceTainted(Stmt stmt){
		if(CDSMap.containsKey(stmt)){
			for(Unit uP: CDSMap.get(stmt)){
				if(!((uP instanceof IfStmt) || (uP instanceof LookupSwitchStmt) || (uP instanceof TableSwitchStmt))){
					throw new RuntimeException("control dependent analysis error");
				}
				Set<InfoItem> uSet = new HashSet<InfoItem>();
				for(ValueBox box: uP.getUseBoxes()){
					Value value = box.getValue();
					InfoItem newItem;
					if(value instanceof soot.Local){
						newItem = new Local(value.toString());
						uSet.add(newItem);
					}
				}
				uSet.retainAll((HashSet<InfoItem>) this.getFlowBefore(uP));
				if(!uSet.isEmpty()){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected Object newInitialFlow() {
		// TODO Auto-generated method stub
		return new HashSet<InfoItem>();
	}

	@Override
	protected Object entryInitialFlow() {
		// TODO Auto-generated method stub
		return new HashSet<InfoItem>();
	}

	@Override
	protected void merge(Object in1, Object in2, Object out) {
		// TODO Auto-generated method stub
		HashSet<InfoItem> in1Set = (HashSet<InfoItem>) in1,
				in2Set = (HashSet<InfoItem>) in2;
		HashSet<InfoItem> outSet = (HashSet<InfoItem>) out;
		outSet.clear();
		
		outSet.addAll(in1Set);
		outSet.addAll(in2Set);
	}

	@Override
	protected void copy(Object source, Object dest) {
		// TODO Auto-generated method stub
		HashSet<InfoItem> sSet = (HashSet<InfoItem>) source,
			dSet = (HashSet<InfoItem>) dest;
		dSet.clear();
		
		dSet.addAll(sSet);
	}

}
