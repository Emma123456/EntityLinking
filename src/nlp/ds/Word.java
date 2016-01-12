package nlp.ds;

import java.util.ArrayList;

import rdf.EntityMapping;

public class Word implements Comparable<Word> {
	
	//type��ent��ƥ����Ϣ
	public boolean mayLiteral = false;
	public boolean mayEnt = false;
	public boolean mayType = false;
	public ArrayList<EntityMapping> emList = null;
	
	public String baseForm = null;
	public String originalForm = null;
	public String posTag = null;
	public int position = -1;	// �����е�һ���ʵ�position��1. ��ɲμ�������﷨��������[]�е�����.
	public String key = null;
	
	public boolean isCovered = false;
	public boolean isIgnored = false;
	
	public String ner = null;	// ��¼ner�Ľ��������ne��Ϊnull
	public Word nnNext = null;	// ��¼nn���δʵĺ�һ���ʣ���ֹ��null����
	public Word nnPrev = null;	// ��¼nn���δʵ�ǰһ���ʣ���ֹ��null����
	public Word crr	= null;		// ��¼ָ�������ָ��ֻ��¼�ڶ����head�ϣ�ָ����һ�������head
	
	public Word represent = null; // ��¼��word���ĸ�word������"which book is ..."��"which"
	public boolean omitNode = false; // ������word�����Ϊnode
	public Word modifiedWord = null; //��¼��word�����ĸ�word
	
	public Word (String base, String original, String pos, int posi) {
		baseForm = base;
		originalForm = original;
		posTag = pos;
		position = posi;		
		key = new String(originalForm+"["+position+"]");
	}
	
	@Override
	public String toString() {
		return key;
	}

	public int compareTo(Word another) {
		return this.position-another.position;
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return (o instanceof Word) 
			&& originalForm.equals(((Word)o).originalForm)
			&& position == ((Word)o).position;
	}
	
	// С��NnHead����nn�ṹ�Ķ���
	public Word getNnHead() {
		Word w = this;
		// ��ent/type������word����Դ���������⣬��˸ɴ�ֱ�Ӷ����ر������������е�nn��Ϣ
		
//		// ��Ԥ����׶�ʶ���ent��type���ۼ���һ��word��parser�ְ����word����ǰ���word���������Ϊ��һ������word����ʱ���ǲ�����parser����ֱ�ӷ��ظ�word
//		if(w.mayEnt || w.mayType)
//			return w;
//		
//		while (w.nnPrev != null) {
//			w = w.nnPrev;
//		}
		
		return w;
	}
	
	public String getFullEntityName() {
		Word w = this.getNnHead();
		// ��ent/type������word����Դ���������⣬��˸ɴ�ֱ�Ӷ����ر������������е�nn��Ϣ
		return w.originalForm;
		
//		// ��Ԥ����׶�ʶ���ent��type���ۼ���һ��word��parser�ְ����word����ǰ���word���������Ϊ��һ������word����ʱ���ǲ�����parser����ֱ�ӷ��ظ�word
//		if(w.mayEnt || w.mayType)
//			return w.originalForm;
//		
//		StringBuilder sb = new StringBuilder("");
//		while (w != null) {
//			sb.append(w.originalForm);			
//			sb.append(' ');
//			w = w.nnNext;
//		}
//		sb.deleteCharAt(sb.length()-1);
//		return sb.toString();
	}
	
	public String getBaseFormEntityName() {
		Word w = this.getNnHead();
		// ��Ԥ����׶�ʶ���ent��type���ۼ���һ��word��parser�ְ����word����ǰ���word���������Ϊ��һ������word����ʱ���ǲ�����parser����ֱ�ӷ��ظ�word
		if(w.mayEnt || w.mayType)
			return w.baseForm;
				
		StringBuilder sb = new StringBuilder("");
		while (w != null) {
			sb.append(w.baseForm);
			sb.append(' ');
			w = w.nnNext;
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	public String isNER () {
		return this.getNnHead().ner;
	}
	
	public void setIsCovered () {
		Word w = this.getNnHead();
		while (w != null) {
			w.isCovered = true;
			w = w.nnNext;
		}
	}	
}
