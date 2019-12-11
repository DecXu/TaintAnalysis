package taintprocess;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import soot.Timers;
import soot.options.Options;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.interaction.FlowInfo;
import soot.toolkits.graph.interaction.InteractionHandler;
import soot.toolkits.scalar.FlowAnalysis;

public abstract class MyForwardFlowAnalysis<N,A> extends FlowAnalysis<N,A>
{
    /** Construct the analysis from a DirectedGraph representation of a Body.
     */
    public MyForwardFlowAnalysis(DirectedGraph<N> graph)
    {
        super(graph);
    }

    protected boolean isForward()
    {
        return true;
    }

    protected void doAnalysis()
    {
        final Map<N, Integer> numbers = new HashMap<N, Integer>();

        List<N> orderedUnits = constructOrderer().newList(graph,false);

        int i = 1;
        for( Iterator<N> uIt = orderedUnits.iterator(); uIt.hasNext(); ) {
            final N u = uIt.next();
            numbers.put(u, new Integer(i));
            i++;
        }

        Collection<N> changedUnits = constructWorklist(numbers);

        List<N> heads = graph.getHeads();
        int numNodes = graph.size();
        int numComputations = 0;
        
        // Set initial values and nodes to visit.
        {
            Iterator<N> it = graph.iterator();

            while(it.hasNext())
            {
                N s = it.next();

                changedUnits.add(s);

                unitToBeforeFlow.put(s, newInitialFlow());
                unitToAfterFlow.put(s, newInitialFlow());
            }
        }

        {
            Iterator<N> it = heads.iterator();
            
            while (it.hasNext()) {
                N s = it.next();
                // this is a forward flow analysis
                unitToBeforeFlow.put(s, entryInitialFlow());
            }
        }
        
        // Perform fixed point flow analysis
        {

        	A previousAfterFlow = newInitialFlow();
            while(!changedUnits.isEmpty())
            {
                A beforeFlow;
                A afterFlow;

                //get the first object
                N s = changedUnits.iterator().next();
                changedUnits.remove(s);
                boolean isHead = heads.contains(s);

                copy(unitToAfterFlow.get(s), previousAfterFlow);

                // Compute and store beforeFlow
                {
                    List<N> preds = graph.getPredsOf(s);

                    beforeFlow = unitToBeforeFlow.get(s);
                    
                    if(preds.size() == 1)
                        copy(unitToAfterFlow.get(preds.get(0)), beforeFlow);
                    else if(preds.size() != 0)
                    {
                        Iterator<N> predIt = preds.iterator();

                        copy(unitToAfterFlow.get(predIt.next()), beforeFlow);

                        while(predIt.hasNext())
                        {
                            A otherBranchFlow = unitToAfterFlow.get(predIt.next());
                            mergeInto(s, beforeFlow, otherBranchFlow);
                        }
                    }

                    if(isHead && preds.size() != 0)
                    		mergeInto(s, beforeFlow, entryInitialFlow());
                    	}
                
                {
                    // Compute afterFlow and store it.
                    afterFlow = unitToAfterFlow.get(s);
                    if (Options.v().interactive_mode()){
                        
                        A savedInfo = newInitialFlow();
                        if (filterUnitToBeforeFlow != null){
                            savedInfo = filterUnitToBeforeFlow.get(s);
                            copy(filterUnitToBeforeFlow.get(s), savedInfo);
                        }
                        else {
                            copy(beforeFlow, savedInfo);
                        }
                        FlowInfo fi = new FlowInfo(savedInfo, s, true);
                        if (InteractionHandler.v().getStopUnitList() != null && InteractionHandler.v().getStopUnitList().contains(s)){
                            InteractionHandler.v().handleStopAtNodeEvent(s);
                        }
                        InteractionHandler.v().handleBeforeAnalysisEvent(fi);
                    }
                    flowThrough(beforeFlow, s, afterFlow);
                    if (Options.v().interactive_mode()){
                        A aSavedInfo = newInitialFlow();
                        if (filterUnitToAfterFlow != null){
                            aSavedInfo = filterUnitToAfterFlow.get(s);
                            copy(filterUnitToAfterFlow.get(s), aSavedInfo);
                        }
                        else {
                            copy(afterFlow, aSavedInfo);
                        }
                        FlowInfo fi = new FlowInfo(aSavedInfo, s, false);
                        InteractionHandler.v().handleAfterAnalysisEvent(fi);
                    }
                    numComputations++;
                }

                // Update queue appropriately
//                System.out.println();
//                System.out.println(changedUnits.toString());
                    if(!afterFlow.equals(previousAfterFlow))
                    {
                        Iterator<N> succIt = graph.getSuccsOf(s).iterator();

                        while(succIt.hasNext())
                        {
                            N succ = succIt.next();
                            
                            changedUnits.add(succ);
                        }
                    }
                    
                }
            }
        
        Timers.v().totalFlowNodes += numNodes;
        Timers.v().totalFlowComputations += numComputations;
    }
    
	protected Collection<N> constructWorklist(final Map<N, Integer> numbers) {
		return new TreeSet<N>( new Comparator<N>() {
            public int compare(N o1, N o2) {
                Integer i1 = numbers.get(o1);
                Integer i2 = numbers.get(o2);
                return (i1.intValue() - i2.intValue());
            }
        } );
	}

}