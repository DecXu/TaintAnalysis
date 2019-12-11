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
	public IntraTaintAnalysis(SootMethod m, BriefUnitGraph cfg, Map<SootMethod, Signature> sigs, Set<SootMethod> methods)
	{
		super(cfg);
	}

	@Override
	protected void flowThrough(Object arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void copy(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Object entryInitialFlow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void merge(Object arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Object newInitialFlow() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
