package org.impact.evaluation.parsers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.impact.evaluation.Page;
import org.impact.evaluation.TextRegion;
import org.impact.evaluation.Word;
import org.impact.evaluation.util.ParseUtils;
import org.impact.evaluation.util.TagSoupParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class NETaggedPlainTextParser 
{
	Document document;
	static int BIG_ENOUGH = 1000000;
	
	public Page parsePage(String url)
	{
		Page p = new Page();
		p.id = url;
		this.document = TagSoupParser.parse2DOM(url);
		p.setDocument(this.document);
		
		Element rootElement = document.getDocumentElement();
		TextRegion region = new TextRegion();
		
		// add a bogus bounding box, so everything will always be inside
		
		List<Point> points = new ArrayList<Point>();
			points.add(new Point(0,0));
			points.add(new Point(0,BIG_ENOUGH));
			points.add(new Point(BIG_ENOUGH,BIG_ENOUGH));
			points.add(new Point(BIG_ENOUGH,0));
			
		region.setBoundingPolygon(points);
		
		region.id="r0";
	
		List<Node> textNodes = ParseUtils.getTextNodesBelow((Node) rootElement);
		int nWords = 0;
		
		for (Node t: textNodes)
		{
			String text = t.getTextContent();
			Element neNode = ParseUtils.findAncestor(t,"NE");
			boolean inNE = (neNode != null);
			String[] words = text.split("\\s+");
			for (String s: words)
			{
				Word w = new Word(s);
				w.type = inNE ? Word.TYPE_NE:Word.TYPE_GENERAL;
				
				if (w.text.length() > 0)
				{
					region.addWord(w);
					//System.err.println("Piep: " + nWords++ + " : " + w.text + " --> " + w.tokenizedText);
				}
			}
		}
		return p;
	}
	
	public static void main(String[] args)
	{
		NETaggedPlainTextParser p = new NETaggedPlainTextParser();
		Page page =  p.parsePage("N:\\\\Corpora\\NESample.txt");
		System.err.println(ParseUtils.documentToString(page.getDocument()));
	}
}
