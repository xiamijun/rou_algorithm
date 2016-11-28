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
import java.io.IOException;

public class RouMiningPlugin implements MiningPlugin{

	//设置去噪参数，默认为0
	public final static int PARAMETER = 0;
	private LogEvents events;
	//计数器
	private DoubleMatrix1D startCount;	//一维数组
	private DoubleMatrix1D endCount;

	private DoubleMatrix2D directSuccessionCount;	//存储后继任务频度矩阵
	private DoubleMatrix2D causalSuccession; 	//存储相关性矩阵，通过Correlation方法获得
	//阈值
	private double val1=0;
	private double val2=0;
	//表示频度矩阵中不为0的个数
	private int notzero=0;

	public MiningResult mine(LogReader log){
		PetriNet result = new PetriNet();
		events = log.getLogSummary().getLogEvents();

		LogAbstraction logAbstraction = new LogAbstractionImpl(log);

		startCount = logAbstraction.getStartInfo().copy();
		endCount = logAbstraction.getEndInfo().copy();
		directSuccessionCount = logAbstraction.getFollowerInfo(1).copy();	//获取后继任务频度矩阵
		//总活动数
		int n=directSuccessionCount.rows();

		//计算阈值val1
		for (int i = 0; i < directSuccessionCount.rows(); i++){
			for (int j = 0; j < directSuccessionCount.rows(); j++){
				val1+=directSuccessionCount.get(i,j);
				if (directSuccessionCount.get(i,j)!=0) {
					notzero++;
				}
			}
		}
		try{
			val1=val1/notzero;
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}
		//计算阈值val2
		int a=0;	//用于计算
		int b=0;
		for (int i = 0; i < directSuccessionCount.rows(); i++){
			for (int j = i; j < directSuccessionCount.rows(); j++){
				a+=(directSuccessionCount.get(i,j)-directSuccessionCount.get(j,i));
				b+=(directSuccessionCount.get(i,j)+directSuccessionCount.get(j,i)+PARAMETER);
			}
		}
		val2=a/b;

		//构造相关性矩阵
		correlation(log,PARAMETER);

		//构造变迁
		private Transition transition[n];
		for (int i=0; i<n; i++) {
			transition[i]=new Transition("transition"+i,result);
			result.addTransition(transition[i]);
		}

		private Place place[n][n];
		//判断是否存在库所,并构造库所
		for (int i = 0; i < causalSuccession.rows(); i++){
			for (int j = 0; j < causalSuccession.rows(); j++){
				if (causalSuccession.get(i,j)>val2) {
					//活动i与j存在库所
					place[i][j]=new Place("place({"+i+"},{"+j+"})",result);
					result.addPlace(place[i][j]);
				}else{
					place[i][j]=null;
				}
			}
		}

		//判断路由结构
		for (int i = 0; i < causalSuccession.rows(); i++){
			for (int j = 0; j < causalSuccession.rows(); j++){
				if () {
					
				}
			}
		}

		//构造边


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

	//parameter表示去噪参数
	public void correlation(LogReader log,int parameter){
			row=directSuccessionCount.rows();	//获取行数
			//构建相关性矩阵
			causalSuccession.set(row, row);
		
		//计算相关性
		for (int i = 0; i < causalSuccession.rows(); i++) {
			for (int j = i; j < causalSuccession.rows(); j++) {
				if(i=j){
					if (directSuccessionCount.get(i,j)>val1) {	//获取频度矩阵的值
						causalSuccession.set(i,j,1);
					}else{
						causalSuccession.set(i,j,0);
					}
				}else{
					if (directSuccessionCount.get(i,j)==0&&directSuccessionCount.get(j,i)==0) {
						causalSuccession.set(i,j,0);
					}else{
						causalSuccession.set(i,j,(directSuccessionCount.get(i,j)-directSuccessionCount.get(j,i))/(directSuccessionCount.get(i,j)+directSuccessionCount.get(j,i)+parameter));
					}
				}		
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

