package taintprocess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.IRegion;
import soot.toolkits.graph.pdg.PDGNode;

public class CDA 
{

	final DirectedGraph cfg; 
	
	//maintain only direct control dependency: <unit1, unit2> where statement unit1 is control dependent of statement unit2 
	final Map<Unit, Unit> CDMap;
	
	//maintain both direct and indirect control dependency: <unit1, set2> where statement unit1 is control dependent of all the statements in set2
	final Map<Unit, HashSet<Unit>> CDSMap;
	
	
	/** constructor
	 * @param cfg
	 */
	public CDA(DirectedGraph cfg) {
		this.cfg = cfg;
		this.CDMap = new HashMap<Unit, Unit>();
		this.CDSMap = new HashMap<Unit, HashSet<Unit>>();
		
		analyzeControlDependency();
	}


	/** analyze control dependence information and store it in CDSMap
	 * 
	 */
	private void analyzeControlDependency(){
		/* first step: get direct control dependency information */
		HashMutablePDG pdg = new HashMutablePDG((BriefUnitGraph) cfg);
		List<Object> nodes = pdg.getNodes(); // a list of all the pdg nodes: PDGNode
		for(Iterator<PDGNode> it = pdg.iterator(); it.hasNext();){
			PDGNode controlNode = it.next();
			if(controlNode.getType() == PDGNode.Type.CFGNODE){
				//if it's CFGNODE, cast this pdg node to a block
				Block bk = (Block) controlNode.getNode();
				Unit tail = bk.getTail();
				//check whether the tail unit of the block contains controlling expression: ifStmt and SwitchStmt
				if(tail instanceof IfStmt || tail instanceof TableSwitchStmt || tail instanceof LookupSwitchStmt){
					for(Object o: nodes){
						PDGNode node = (PDGNode) o;
						//check whether node is control dependent of controlNode 
						if(pdg.dependentOn(node, controlNode)){
							IRegion region = (IRegion) node.getNode();
							//all the units in this region are control dependent of tail 
							for(Unit u: region.getUnits()){
								CDMap.put(u, tail);
							}
						}
					}
				}
			}
		}
		
		/* second step: calculate both indirect and direct control dependency maintained by CDSMap */
		for(Unit key: CDMap.keySet()){
			CDSMap.put(key, new HashSet<Unit>());
			Unit u = key;
			while(CDMap.containsKey(u)){
				u = CDMap.get(u);
				CDSMap.get(key).add(u);
			}
		}
	}
	
	
	public Map<Unit, Unit> getCDMap() {
		return CDMap;
	}

	public Map<Unit, HashSet<Unit>> getCDSMap() {
		return CDSMap;
	}	
}
