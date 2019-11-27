package taintprocess;

import java.util.Map;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;
import soot.util.Chain;

//主要是soot的相关配置工作，在其wjtp阶段插入自己的分析过程


public class InterMainDriver 
{
	public static void main(String[] args) 
	{
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTransform", new SceneTransformer()
		{

			@Override
			protected void internalTransform(String phaseName, Map options) 
			{
				// TODO Auto-generated method stub
				Chain<SootClass> classes = Scene.v().getApplicationClasses();
				System.out.println("Application classes analyzed: " + classes.toString());
				InterTaintAnalysis analysis = new InterTaintAnalysis(classes);
			}
			
		}));
		
		Options.v().set_whole_program(true);
		Options.v().setPhaseOption("cg", "enabled:false");
		Options.v().setPhaseOption("wjpp", "enabled:false");
		Options.v().setPhaseOption("wjap", "enabled:false");
		
		Options.v().setPhaseOption("jap", "enabled:false");
		Options.v().setPhaseOption("jb", "use-original-names:true");//保存局部变量的名字
		// jb -> Jimple Body Creation
		Options.v().set_keep_line_number(true);
		Options.v().set_output_format(Options.output_format_jimple);

		Options.v().set_prepend_classpath(true);
		soot.Main.main(args);
	}

}
