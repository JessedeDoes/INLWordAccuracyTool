package org.impact.postcorrection;

import java.util.HashMap;
import java.util.Map;

import org.impact.evaluation.Word;
import org.impact.evaluation.util.ParseUtils;
import org.w3c.dom.Element;

public class hOCRCorrector implements DocumentCorrector 
{
	private Map<String,Element> wordElementMap = new HashMap<String,Element>();
	static int nCorrections = 0;
	
	public void addWord(String id, Element e)
	{
		wordElementMap.put(id,e);
	}
	
	@Override
	public void updateWord(Word w) 
	{
		Element e = wordElementMap.get(w.id);
		nCorrections++;
		if (e != null)
		{
			Element c = ParseUtils.findFirstChild(e, "span");
			if (c != null)
			{
				c.setAttribute("originalText", c.getTextContent());
				c.setTextContent(w.text);
				// System.err.println(nCorrections);
			}
		} else
		{
			System.err.println("could not get word " + w.id);
		}
		// TODO Auto-generated method stub
	}
}
