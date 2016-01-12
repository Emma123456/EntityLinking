package process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import rdf.EntityMapping;
import rdf.MergedWord;
import rdf.NodeSelectedWithScore;
import fgmt.EntityFragment;
import nlp.ds.Word;
import global.Globals;

public class EntityRecognition {
	public String preLog = "";
	
	double EntAcceptedScore = 26;	//the_mayor_of_Berlin: Governing Mayor of Berlin score:25.8919727593077
	double TypeAcceptedScore = 0.5;
	double AcceptedDiffScore = 1;
	
	public ArrayList<MergedWord> mWordList = null;
	public ArrayList<String> stopEntList = null;
	public ArrayList<String> badTagListForEntAndType = null;
	ArrayList<ArrayList<Integer>> selectedList = null;
	
	public void init()
	{
		preLog = "";
		stopEntList = new ArrayList<String>();
		stopEntList.add("people");
		stopEntList.add("list");
		stopEntList.add("car");
		stopEntList.add("flow");
		stopEntList.add("i");
		stopEntList.add("state");
		stopEntList.add("instrument");
		stopEntList.add("inhabitant");
		stopEntList.add("employee");
		stopEntList.add("entrance");
		stopEntList.add("war");
		stopEntList.add("city");
		stopEntList.add("capital");
		stopEntList.add("host");
		stopEntList.add("munch");
		stopEntList.add("show");
		stopEntList.add("show_I");
		stopEntList.add("president_of_the_unite_state");
		stopEntList.add("span");
		stopEntList.add("budget");
		stopEntList.add("population");
		stopEntList.add("density");
		stopEntList.add("population_density");
		
		badTagListForEntAndType = new ArrayList<String>();
		badTagListForEntAndType.add("RBS");
		badTagListForEntAndType.add("JJS");
		badTagListForEntAndType.add("W");
		badTagListForEntAndType.add(".");
		badTagListForEntAndType.add("VBD");
		badTagListForEntAndType.add("VBN");
		badTagListForEntAndType.add("VBZ");
		badTagListForEntAndType.add("VBP");
		badTagListForEntAndType.add("POS");
	}
	
	public ArrayList<String> process(String question)
	{
		init();
	
		ArrayList<String> fixedQuestionList = new ArrayList<String>();
		ArrayList<Integer> literalList = new ArrayList<Integer>();
		HashMap<Integer, Double> entityScores = new HashMap<Integer, Double>();
		HashMap<Integer, String> entityMappings = new HashMap<Integer, String>();
		HashMap<Integer, Double> typeScores = new HashMap<Integer, Double>();
		HashMap<Integer, String> typeMappings = new HashMap<Integer, String>();
		HashMap<String, Double> mappingScores = new HashMap<String, Double>();
		ArrayList<Integer> mustSelectedList = new ArrayList<Integer>();
		
		System.out.println("--------- pre entity/type recognition start ---------");
		
		Word[] words = Globals.coreNLP.getTaggedWords(question);
		mWordList = new ArrayList<MergedWord>();
		
		long t1 = System.currentTimeMillis();
		int checkEntCnt = 0, checkTypeCnt = 0;
		boolean needRemoveCommas = false;
		
		//check entity
		//ע��len��С�����˳���ܱ䣬��ΪһЩ��ent�Ƿ�����÷ֲ��������ڶ�ent��ʶ�����
		for(int len=1;len<=words.length;len++)
		{
			for(int st=0,ed=st+len; ed<=words.length; st++,ed++)
			{
				String originalWord = "",baseWord = "", allUpperWord = "";
				for(int j=st;j<ed;j++)
				{
					originalWord += words[j].originalForm;
					baseWord += words[j].baseForm;
					String tmp = words[j].originalForm;
					if(tmp.length()>0 && tmp.charAt(0)>='a' && tmp.charAt(0)<='z')
					{
						String pre = tmp.substring(0,1).toUpperCase();
						tmp = pre + tmp.substring(1);
					}
					allUpperWord += tmp;
					
					if(j < ed-1)
					{
						originalWord += "_";
						baseWord += "_";
					}
				}
				
				//��һЩ�������find������������Щ�����������ĳЩentity
				boolean entOmit = false, typeOmit = false;
				int prep_cnt=0;
				
				//�����һ��word���Ǵ�д��ĸ��ͷ���������������word�м䣬��Ҳ������д��eg��"Melbourne , Florida"������ض���mapping��������й����ж��Ƿ���mapping
				int UpperWordCnt = 0;
				for(int i=st;i<ed;i++)
					if((words[i].originalForm.charAt(0)>='A' && words[i].originalForm.charAt(0)<='Z') || (words[i].posTag.equals(",") && i>st && i<ed-1))
						UpperWordCnt++;
				
				//�������һЩ "��������������entity"�Ĺ����򲻽���ent���
				if(UpperWordCnt<len || st==0)
				{
					if(st==0)
					{
						if(!words[st].posTag.startsWith("DT") && !words[st].posTag.startsWith("N"))
						{
							entOmit = true;
							typeOmit = true;
						}
					}
					
					//��Щrule��Ҫ�ж���һ���ʣ�����Ҫ��st����0��ע����Щruleֻ���ent
					if(st>0)
					{
						Word formerWord = words[st-1];
						//as princess
						if(formerWord.baseForm.equals("as"))
							entOmit = true;
						//how many dogs?
						if(formerWord.baseForm.equals("many"))
							entOmit = true;
						//obama's daughter ; your height
						if(formerWord.posTag.startsWith("POS") || formerWord.posTag.startsWith("PRP"))
							entOmit = true;
						//the father of you
						if(ed<words.length)
						{
							Word nextWord = words[ed];
							if(formerWord.posTag.equals("DT") && nextWord.posTag.equals("IN"))
								entOmit = true;
						}
						//the area code of ; the official language of
						boolean flag1=false,flag2=false;
						for(int i=0;i<=st;i++)
							if(words[i].posTag.equals("DT"))
								flag1 = true;
						for(int i=ed-1;i<words.length;i++)
							if(words[i].posTag.equals("IN"))
								flag2 = true;
						if(flag1 && flag2)
							entOmit = true;
					}
					
					if(ed < words.length)
					{
						Word nextWord = words[ed];
						
						//�������һ�����Ǵ�д������Ϊ����Ĵ���һ��ʵ�壨��ʵ��ĺ�׺������ô��ǰ��������ǰ׺������֮ǰ�Ĵʡ���������Ϊ����������ʵ����ȫ����
						if(nextWord.originalForm.charAt(0)>='A' && nextWord.originalForm.charAt(0)<='Z')
							entOmit = true;
					}
					
					for(int i=st;i<ed;i++)
					{
						if(words[i].posTag.startsWith("I"))
							prep_cnt++;
						
						for(String badTag: badTagListForEntAndType)
						{
							if(words[i].posTag.startsWith(badTag))
							{
								entOmit = true;
								typeOmit = true;
								break;
							}
						}
						if(words[i].posTag.startsWith("P") && (i!=ed-1 || len==1)){
							entOmit = true;
							typeOmit = true;
						}
						//���״ʵ��ж�
						if(i==st)
						{
							if(words[i].posTag.startsWith("I")){
								entOmit = true;
								typeOmit = true;
							}
							if(words[i].posTag.startsWith("D") && len==2){
								entOmit = true;
								typeOmit = true;
							}
							if(words[i].posTag.startsWith("EX")){
								entOmit = true;
								typeOmit = true;
							}
							if(words[i].posTag.startsWith("TO")){
								entOmit = true;
								typeOmit = true;
							}
							if(words[i].baseForm.startsWith("list") || words[i].baseForm.startsWith("many"))
							{
								entOmit = true;
								typeOmit = true;
							}
							if(words[i].baseForm.equals("and"))
							{
								entOmit = true;
								typeOmit = true;
							}
						}
						//��β�ʵ��ж�
						if(i==ed-1)
						{
							if(words[i].posTag.startsWith("I")){
								entOmit = true;
								typeOmit = true;
							}
							if(words[i].posTag.startsWith("D")){
								entOmit = true;
								typeOmit = true;
							}
							if(words[i].posTag.startsWith("TO"))
							{
								entOmit = true;
								typeOmit = true;
							}
							if(words[i].baseForm.equals("and"))
							{
								entOmit = true;
								typeOmit = true;
							}
						}
						//������ֻ��һ����
						if(len==1)
						{
							//TODO:ֻ���� ���� ����check�����ǳ��� Ӧ���������ġ�ͨ�����ʡ������Ϊent
							if(!words[i].posTag.startsWith("N"))
							{
								entOmit = true;
								typeOmit = true;
							}
						}
					}
					//��������̫��Ľ�ʲ�̫������ent
					if(prep_cnt >= 3)
					{
						entOmit = true;
						typeOmit = true;
					}
				}
				//�������
				
				//search entity
				ArrayList<EntityMapping> emList = new ArrayList<EntityMapping>();
				if(!entOmit && !stopEntList.contains(baseWord))
				{
					System.out.println("Ent Check: "+originalWord);
					checkEntCnt++;
					//ע������ĵڶ�������������dblk������
					emList = getEntityIDsAndNamesByStr(originalWord,(UpperWordCnt>=len-1 || len==1),len);
					if(emList == null || emList.size() == 0)
					{
						emList = getEntityIDsAndNamesByStr(baseWord, (UpperWordCnt>=len-1 || len==1), len);
					}
					if(emList!=null && emList.size()>10)
					{
						ArrayList<EntityMapping> tmpList = new ArrayList<EntityMapping>();
						for(int i=0;i<10;i++)
						{
							tmpList.add(emList.get(i));
						}
						emList = tmpList;
					}
				
				}
				
				MergedWord mWord = new MergedWord(st,ed,originalWord);
				
				//add literal
				if(len==1 && checkLiteralWord(words[st]))
				{
					mWord.mayLiteral = true;
					int key = st*(words.length+1) + ed;
					literalList.add(key);
				}
				
				//add entity mappings
				if(emList!=null && emList.size()>0)
				{
					//�����߷�̫�ͣ�ֱ������
					if(emList.get(0).score < EntAcceptedScore)
						entOmit = true;
					
					//������� the German Shepherd dog��ʽ����ֹthe German Shepherd dog����ʶ��Ϊһ��ent || The Pillars of the Earth����The_Storm on the Sea_of_GalileeӦ����һ��ent�����rule��ͻ
					else if(len > 2)
					{
						for(int key: entityMappings.keySet())
						{
							int te=key%(words.length+1),ts=key/(words.length+1);
							//�����еĵ�һ��word�ǡ�ent��
							if(ts == st+1 && ts <= ed)
							{
								//��һ������DT
								if(words[st].posTag.startsWith("DT") && !(words[st].originalForm.charAt(0)>='A'&&words[st].originalForm.charAt(0)<='Z'))
								{
									entOmit = true;
								}
							}
						}
					}
					
					//ƥ����Ϣ����merge word
					if(!entOmit)
					{
						mWord.mayEnt = true;
						mWord.emList = emList;
						
						//����֮���remove duplicate and select
						int key = st*(words.length+1) + ed;
						entityMappings.put(key, emList.get(0).entityID);
						
						//�����ent�����ֽ���һЩ����
						double score = emList.get(0).score;
						String likelyEnt = emList.get(0).entityName.toLowerCase().replace(" ", "_");
						String lowerOriginalWord = originalWord.toLowerCase();
						//����ַ�����ȫƥ�䣬��÷ֳ���word������������word�������Ѿ��ܸ߲��Ҿ�������ֲ���Ҫ������������൱��û�����ӵ���word�÷�
						if(likelyEnt.equals(lowerOriginalWord))
							score *= len;
						//������Ent��һЩС��ent��ȫ���ǣ���ô���Ŀ��Ŷ�Ӧ�ñ���ЩС��ent�ĺ͸��ߡ����磺Robert Kennedy��[Robert]��[Kennedy]�����ҵ���Ӧent������Ȼ����Ӧ����[Robert Kennedy]
						//��Social_Democratic_Party��������word������϶���ent�����·���̫�ࣻ��Ƚϡ���ͻѡ�ĸ���������or��Ӧ�������Եø���Ҫ������ʵ�ʴ����Ϊ�������Ĵ��󣩣���������ֱ�����������ǵ�Сent
						//��Abraham_Lincoln���ڡ������ӡ��ķ����У��������ʶ�������node�����÷ֳ�������ȷ�𰸵ĵ÷֣��ʶ������ִ�����Ϊ��ѡ
						if(len>1)
						{
							boolean[] flag = new boolean[words.length+1];
							ArrayList<Integer> needlessEntList = new ArrayList<Integer>();
							double tmpScore=0;
							for(int preKey: entityMappings.keySet())
							{
								if(preKey == key)
									continue;
								int te=preKey%(words.length+1),ts=preKey/(words.length+1);
								for(int i=ts;i<te;i++)
									flag[i] = true;
								if(st<=ts && ed>= te)
								{
									needlessEntList.add(preKey);
									tmpScore += entityScores.get(preKey);
								}
							}
							int hitCnt = 0;
							for(int i=st;i<ed;i++)
								if(flag[i])
									hitCnt++;
							//����ȫ���ǵ�������Ϊ ��ȫ���� ||�󲿷ָ��ǲ��Ҵ󲿷ִ�д || ȫ����д
							if(hitCnt == len || ((double)hitCnt/(double)len > 0.6 && (double)UpperWordCnt/(double)len > 0.6) || UpperWordCnt == len || len>=4)
							{
								//���м��ж��ţ���Ҫ�����ߵĴʶ���mapping��entity�г���
								//���� Melbourne_,_Florida: Melbourne, Florida �Ǳ���ѡ�ģ��� California_,_USA: Malibu, California����Ϊ��һ����ȷ
								boolean commaTotalRight = true;
								if(originalWord.contains(","))
								{
									String candidateCompactString = originalWord.replace(",","").replace("_", "").toLowerCase();
									String likelyCompactEnt = likelyEnt.replace(",","").replace("_", "");
									if(!candidateCompactString.equals(likelyCompactEnt))
										commaTotalRight = false;
									else
									{
										mWord.name = mWord.name.replace("_,_","_");
										needRemoveCommas = true;
									}
								}
									
								if(commaTotalRight)
								{
									mustSelectedList.add(key);
									if(tmpScore>score)
										score = tmpScore+1;
									for(int preKey: needlessEntList)
									{
										entityMappings.remove(preKey);
										mustSelectedList.remove(Integer.valueOf(preKey));
									}
								}
							}
						}
						
						entityScores.put(key,score);
						//ע��mWord�е�score��û�иģ���Ϊ��û���Ǻ�����������Ƿ�Ӧ�ô������Ļ���
					}
				}
				
				if(mWord.mayEnt || mWord.mayType || mWord.mayLiteral)
					mWordList.add(mWord);
			}
		}
		
		/*������к�ѡƥ�䣬ע��������������������������������֣���������������mWord�������*/
		System.out.println("------- Result ------");
		for(MergedWord mWord: mWordList)
		{
			int key = mWord.st * (words.length+1) + mWord.ed;
			if(mWord.mayEnt)
			{
				System.out.println("Detect entity mapping: "+mWord.name+": "+entityMappings.get(key)+" score:"+entityScores.get(key));
	        	preLog += "++++ Entity detect: "+mWord.name+": "+entityMappings.get(key)+" score:"+entityScores.get(key)+"\n";
			}
			if(mWord.mayType)
			{
				System.out.println("Detect type mapping: "+mWord.name+": "+typeMappings.get(key)+" score:"+typeScores.get(key));
	    		preLog += "++++ Type detect: "+mWord.name+": "+typeMappings.get(key)+" score:"+typeScores.get(key)+"\n";
			}
			if(mWord.mayLiteral)
			{
				System.out.println("Detect literal: "+mWord.name);
				preLog += "++++ Literal detect: "+mWord.name+"\n";
			}
		}
		
		/*
		 * sort by score and remove duplicate
		 * <"video_game" "ent:Video game" "50.0"> <"a_video_game" "ent:Video game" "45.0">, ����ϳɶ��ַ�����ÿ�������ڲ�����ͻ��
		 * ���յ÷���ߵĶ�Ӧʵ��ĵ÷����򣬿����ظ��ĵͷ֡�ע��ʵ���ϲ�û����mWordList��ɾ���κ���Ϣ��
		 * type���жϽ��ϣ���Ϊ����������������
		 * 
		 * 2015-11-28
		 * ����ÿһ����Ҫ���»��ߵĴ����У���Node����ѡ�����ߣ������ǵ�ʶ����Ϣ������ȥ�������ߣ�������������е�ʶ����Ϣ��
		 * ��Ϊtype��ȫƥ����Һ���������������ʶ���type���Ǹ�word�Ǳ�ѡ�ģ�
		 * ��Ϊliteralֻʶ�����֣�����ʶ���literal���Ǹ�wordҲ�Ǳ�ѡ�ģ�
		 * KB��ͬһent��Ӧquery�в�ͬmergedWord�ģ�ȡ�÷ָߵ�
		*/
		// KB��ͬһent��Ӧquery�в�ͬmergedWord�ģ�ȡ�÷ָߵ�
		ByValueComparator bvc = new ByValueComparator(entityScores,words.length+1);
		List<Integer> keys = new ArrayList<Integer>(entityMappings.keySet());
        Collections.sort(keys, bvc);
        for(Integer key : keys)
        {
        	if(!mappingScores.containsKey(entityMappings.get(key)))
        		mappingScores.put(entityMappings.get(key), entityScores.get(key));
        	else
        		entityMappings.remove(key);
        }
        
        // 2015-11-28 ö�ٲ���ͻ�Ĳ���
        selectedList = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> selected = new ArrayList<Integer>();
        
        // �ȷ���϶�Ҫѡ��key
        selected.addAll(mustSelectedList);
        for(Integer key: typeMappings.keySet())
        {
        	//��Ϊtype��ȫƥ����Һ���������������ʶ���type���Ǹ�word�����Ǳ�ѡ�ģ�
        	//ע����ֻ���word sequence����word��type���ܺ������������һ��Ent�����硰Brooklyn Bridge��������type:Bridge��Ӧ�ñ�������
        	int ed = key%(words.length+1), st = key/(words.length+1);
        	if(st+1 < ed)
        	{
        		boolean beCovered = false;
        		//[prime_minister of Spain],һ��ent��ȫ���������type����ʱ���ܻ�ȡent
				for(int preKey: entityMappings.keySet())
				{
					int te=preKey%(words.length+1),ts=preKey/(words.length+1);
					//ent����Ҫ�����type�����㸲��
					if(ts <= st && te >= ed && ed-st < te-ts)
					{
						beCovered = true;
					}
				}
				
				if(!beCovered)
					selected.add(key);
        	}
        	//��type��entity��λ����Ϣ�ϲ��Ա�ͳһ����; 2015-11-29 ��Ϊtype�Ѿ������ȷ��룬�����������ע��
//        	if(!entityMappings.containsKey(key))
//        	{
//        		entityMappings.put(key, typeMappings.get(key));
//        		keys.add(key);
//        	}
        }
//        for(Integer key: literalList)
//        {
//        	//��Ϊliteralֻʶ�����֣�����ʶ���literal���Ǹ�wordҲ�Ǳ�ѡ�ģ�
//        	//��һЩʵ���ǰ������ֵģ����� Chile Route 68������literal������ǰ�̶�
//        	selected.add(key);
//        }
        
        //����֮ǰ�Ĳ������⣬��ѡ�����п��ܳ�ͻ�����ﰴ�ԭ��������ͻ
        ArrayList<Integer> noConflictSelected = new ArrayList<Integer>();
    	
		//select longer one when conflict  || 2015-11-28 ���ԭ�� ��  ��������ֲ��ԡ� �滻  || 2015-12-13 �ڶ��ֲ��Ի����Ͻ����ԭ��
		boolean[] flag = new boolean[words.length];
		ByLenComparator blc = new ByLenComparator(words.length+1);
		Collections.sort(selected,blc);
		  
		for(Integer key : selected) 
		{
			int ed = key%(words.length+1), st = (key-ed)/(words.length+1);
		  	boolean omit = false;
		  	for(int i=st;i<ed;i++)
		  	{
		  		if(flag[i])
		  		{
		  			omit = true;
		  			break;
		  		}
		  	}
		  	if(omit)
		  		continue;
		  	for(int i=st;i<ed;i++)
		  		flag[i]=true;
		  	noConflictSelected.add(key);
		}
        
        dfs(keys,0,noConflictSelected,words.length+1);
        // get score and sort
        ArrayList<NodeSelectedWithScore> nodeSelectedWithScoreList = new ArrayList<NodeSelectedWithScore>();
        for(ArrayList<Integer> select: selectedList)
        {
        	double score = 0;
        	for(Integer key: select)
        	{
        		if(entityScores.containsKey(key))
        			score += entityScores.get(key);
        		if(typeScores.containsKey(key))
        			score += typeScores.get(key);
        	}
        	NodeSelectedWithScore tmp = new NodeSelectedWithScore(select, score);
        	nodeSelectedWithScoreList.add(tmp);
        }
        Collections.sort(nodeSelectedWithScoreList);
        
        
        
        //replace
        int cnt = 0;
        for(int k=0; k<nodeSelectedWithScoreList.size(); k++)
        {
        	if(k >= nodeSelectedWithScoreList.size())
        		break;
        	selected = nodeSelectedWithScoreList.get(k).selected;
   
        	Collections.sort(selected);
	        int j = 0;
	        String res = question;
	        if(selected.size()>0)
	        {
		        res = words[0].originalForm;
		        int tmp = selected.get(j++), st = tmp/(words.length+1), ed = tmp%(words.length+1);
		        for(int i=1;i<words.length;i++)
		        {
		        	if(i>st && i<ed)
		        	{
		        		res = res+"_"+words[i].originalForm;
		        	}
		        	else
		        	{
		        		res = res+" "+words[i].originalForm;
		        	}
		        	if(i >= ed && j<selected.size())
		        	{
		        		tmp = selected.get(j++);
		        		st = tmp/(words.length+1);
		        		ed = tmp%(words.length+1);
		        	}
		        }
	        }
	        else
	        {
	        	res = words[0].originalForm;
		        for(int i=1;i<words.length;i++)
		        {
		        	res = res+" "+words[i].originalForm;
		        }
	        }
	        
	        boolean ok = true;
	        for(String str: fixedQuestionList)
	        	if(str.equals(res))
	        		ok = false;
	        if(!ok)
	        	continue;
	        
	        if(needRemoveCommas)
	        	res = res.replace("_,_","_");
	        
	        System.out.println("Merged: "+res);
	        preLog += "plan "+cnt+": "+res+"\n";
	        fixedQuestionList.add(res);
	        cnt++;
	        if(cnt >= 3)
	        	break;
        }
        long t2 = System.currentTimeMillis();
        preLog += "Total check ent num: "+checkEntCnt+"\n";
        preLog += "Total check time: "+ (t2-t1) + "ms\n";
		System.out.println("Total check time: "+ (t2-t1) + "ms");
		System.out.println("--------- pre entity/type recognition end ---------");
		
		//fixedQuestionList.add(question);
		return fixedQuestionList;
	}
	
	public void dfs(List<Integer> keys,int dep,ArrayList<Integer> selected,int size)
	{
		if(dep == keys.size())
		{
			ArrayList<Integer> tmpList = (ArrayList<Integer>) selected.clone();
			selectedList.add(tmpList);
		}
		else
		{
			//off ��dep��mWord
			dfs(keys,dep+1,selected,size);
			//����ͻ��on
			boolean conflict = false;
			for(int preKey: selected)
			{
				int curKey = keys.get(dep);
				int preEd = preKey%size, preSt = (preKey-preEd)/size;
				int curEd = curKey%size, curSt = (curKey-curEd)/size;
				if(!(preSt<preEd && preEd<=curSt && curSt<curEd) && !(curSt<curEd && curEd<=preSt && preSt<preEd))
					conflict = true;
			}
			if(!conflict)
			{
				selected.add(keys.get(dep));
				dfs(keys,dep+1,selected,size);
				selected.remove(keys.get(dep));
			}
		}
		
	}
	
	public ArrayList<EntityMapping> getEntityIDsAndNamesByStr(String entity, boolean useDblk, int len) 
	{	
		String n = entity;
		ArrayList<EntityMapping> ret= new ArrayList<EntityMapping>();
		
		//��Ҫ����Lucene����Ϊ������С�������������Ը�����ȷ�����
		ret.addAll(EntityFragment.getEntityMappingList(n));
		
		Collections.sort(ret);
		
		if (ret.size() > 0) return ret;
		else return null;
	}
	
	static class ByValueComparator implements Comparator<Integer> {
        HashMap<Integer, Double> base_map;
        int base_size;
        double eps = 1e-8;
        
        int dblcmp(double a,double b)
        {
        	if(a+eps < b)
        		return -1;
        	return b+eps<a ? 1:0;
        }
 
        public ByValueComparator(HashMap<Integer, Double> base_map, Integer size) {
            this.base_map = base_map;
            this.base_size = size;
        }
 
        public int compare(Integer arg0, Integer arg1) {
            if (!base_map.containsKey(arg0) || !base_map.containsKey(arg1)) {
                return 0;
            }
 
            if (dblcmp(base_map.get(arg0),base_map.get(arg1))<0) {
                return 1;
            } 
            else if (dblcmp(base_map.get(arg0),base_map.get(arg1))==0) 
            {
            	int len0 = (arg0%base_size)-arg0/base_size , len1 = (arg1%base_size)-arg1/base_size;
                if (len0 < len1) {
                    return 1;
                } else if (len0 == len1) {
                    return 0;
                } else {
                    return -1;
                }
            } 
            else {
                return -1;
            }
        }
    }
	
	static class ByLenComparator implements Comparator<Integer> {
        int base_size;
 
        public ByLenComparator(int size) {
            this.base_size = size;
        }
 
        public int compare(Integer arg0, Integer arg1) {
        	int len0 = (arg0%base_size)-arg0/base_size , len1 = (arg1%base_size)-arg1/base_size;
            if (len0 < len1) {
                return 1;
            } else if (len0 == len1) {
                return 0;
            } else {
                return -1;
            }
        }
    }
	 
	public boolean isDigit(char ch)
	{
		if(ch>='0' && ch<='9')
			return true;
		return false;
	}
	
	public boolean checkLiteralWord(Word word)
	{
		boolean ok = false;
		//Ŀǰ��ֻ��Ϊ ��ֵ ��literal
		if(word.posTag.equals("CD"))
			ok = true;
		return ok;
	}
	
	public static void main (String[] args) 
	{
		Globals.init();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try 
		{
			EntityRecognition er = new EntityRecognition();
			while (true) 
			{	
				System.out.print("Please input the question: ");
				String question = br.readLine();
				
				er.process(question);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
