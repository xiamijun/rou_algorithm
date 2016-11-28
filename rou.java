import java.awt.BorderLayout;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.framework.log.AuditTrailEntry;
import org.processmining.framework.log.AuditTrailEntryList;
import org.processmining.framework.log.LogEvent;
import org.processmining.framework.log.LogReader;
import org.processmining.framework.log.LogSummary;
import org.processmining.framework.log.ProcessInstance;
import org.processmining.framework.models.petrinet.PNEdge;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.models.petrinet.Transition;
import org.processmining.framework.plugin.ProvidedObject;
import org.processmining.framework.plugin.Provider;
import org.processmining.mining.MiningPlugin;
import org.processmining.mining.MiningResult;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import org.processmining.mining.logabstraction.LogAbstraction;

public class RouMiningPlugin implements MiningPlugin{

	private LogEvents events;
	//计数器
	private DoubleMatrix1D startCount;	//一维数组
	private DoubleMatrix1D endCount;
	private DoubleMatrix2D directSuccessionCount;	//存储后继任务频度矩阵
	private DoubleMatrix2D causalSuccession; 	//存储相关性矩阵，通过Correlation方法获得
	//阈值
	private double val1;
	private double val2;

	public MiningResult mine(LogReader log){
		PetriNet result = new PetriNet();
		events = log.getLogSummary().getLogEvents();

		LogAbstraction logAbstraction = new LogAbstractionImpl(log);

		startCount = logAbstraction.getStartInfo().copy();
		endCount = logAbstraction.getEndInfo().copy();
		directSuccessionCount = logAbstraction.getFollowerInfo(1).copy();	//后继任务频度矩阵

		//矩阵方法
		val1=
		val2=

		Place start = new Place("Start", result);
		Place end = new Place("End", result);
		Place middle1 = new Place("Middle1", result);
		Place middle2 = new Place("Middle2", result);
		result.addPlace(start);
		result.addPlace(end);
		result.addPlace(middle1);
		result.addPlace(middle2);
		Transition first = new Transition("first", result);
		Transition last = new Transition("last", result);
		result.addTransition(first);
		result.addTransition(last);
		PNEdge startToFirst = new PNEdge(start, first);
		PNEdge firstToMiddle = new PNEdge(first, middle1);
		PNEdge middleToLast = new PNEdge(middle2, last);
		PNEdge lastToEnd = new PNEdge(last, end);
		result.addEdge(startToFirst);
		result.addEdge(firstToMiddle);
		result.addEdge(middleToLast);
		result.addEdge(lastToEnd);
	}

	public void correlation(LogReader log){
		log.reset();
		while (log.hasNext()){
			ProcessInstance pi = log.next();
			AuditTrailEntries ate = pi.getAuditTrailEntries();

			int i = 0;
			boolean terminate = false;

			while (!terminate){
				ate.reset();

			}
		}
		//计算相关性
		for (int i = 0; i < causalSuccession.rows(); i++) {
			for (int j = i; j < causalSuccession.columns(); j++) {
				if (directSuccessionCount.get(i,j)>val1) {
					causalSuccession.set(i,j,1);
				}else{
					causalSuccession.set(i,j,0);
				}
				causalSuccession.set(i, j, ((double) causalSuccession.get(i, j)) /
						longRangeSuccessionCount.get(i, j));
			}
		}
	}

	public String getHtmlDescription() {
		return "rou_algorithm";
	}

	public String getName() {
		return "rou_algorithm";
	}
}

