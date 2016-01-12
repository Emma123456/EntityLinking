package rdf;

import java.util.ArrayList;

import rdf.EntityMapping;

public class MergedWord implements Comparable<MergedWord> 
{
	//ԭ���е���ֹλ�ã���λΪword)
	public int st,ed;
	//merge���λ�ã����δ��ѡ����Ϊ-1
	public int mergedPos = -1;
	public String name;
	public boolean mayLiteral = false;
	public boolean mayEnt = false;
	public boolean mayType = false;
	public ArrayList<EntityMapping> emList = null;
	
	public MergedWord(int s,int e,String n)
	{
		st = s;
		ed = e;
		name = n;
	}
	
	@Override
	//�ɳ�����
	public int compareTo(MergedWord o) 
	{
		int lenDiff = (this.ed-this.st) - (o.ed-o.st);
		
		if (lenDiff > 0) return -1;
		else if (lenDiff < 0) return 1;
		return 0;
	}
	
}
