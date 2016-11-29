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
import java.util.ArrayList;
import java.util.List;

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
		for (int i = 0; i < n; i++){
			for (int j = 0; j < n; j++){
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
		for (int i = 0; i < n; i++){
			for (int j = i; j < n; j++){
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
		for (int i = 0; i < n; i++){
			for (int j = 0; j < n; j++){
				if (causalSuccession.get(i,j)>val2) {
					//活动i与j存在库所
					place[i][j]=new Place("place({"+i+"},{"+j+"})",result);
					result.addPlace(place[i][j]);
				}else{
					place[i][j]=null;
				}
			}
		}

		//构造边,n个节点有向图，最多n(n-1)条边
		List<PNEdge> Edgelist=new ArrayList<PNEdge>();

		//判断路由结构
		private int sum=0;
		private int flag[n][n];	//标志，判断并发和选择，默认并发结构flag[1][2]=3表示2与3是选择结构，前去为1，共享同一输入库所
		for (int i = 0; i < n; i++){
			for (int j = 0; j < n; j++){
				for (int k=0; k<n; k++) {
					if (place[i][j]!=null&&place[i][k]!=null) {
						for (int m=0; m<n; m++) {
							sum+=directSuccessionCount.get(i,m);
						}
						if (sum==directSuccessionCount.get(i,j)&&sum==directSuccessionCount.get(i,k)) {
							flag[i][j]=k;
							flag[i][k]=j;
						}
						//构造边
						if (flag[i][j]=k&&flag[i][k]=j) {
							Edgelist.add(new PNEdge(i, j));
							Edgelist.add(new PNEdge(i, k));
						}else if (directSuccessionCount.get(i,j)>val1&&causalSuccession.get(i,j)>val2) {
							Edgelist.add(new PNEdge(i, j));
						}
					}
				}
			}
		}

		//将边加入petri网
		for(int i=0;i<Edgelist.size();i++){
			result.addEdge(Edgelist.get(i));
		}
	
		return result;

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

