package org.impact.evaluation.parsers;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xalan.xsltc.trax.SAX2DOM;
import org.ccil.cowan.tagsoup.Parser;
import org.impact.evaluation.Page;
import org.impact.evaluation.SimpleTokenizer;
import org.impact.evaluation.TextRegion;
import org.impact.evaluation.Word;
import org.impact.evaluation.util.ParseUtils;
import org.impact.evaluation.util.TagSoupParser;
import org.impact.postcorrection.hOCRCorrector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.awt.Point;



/*
 * This does not REALLY parse the information in the hOCR.
 * We mainly only get coordinates for the words, which can be used to match them to ground truth regions,
 * for better alignment with the ground truth.
 * 
 * I would not know how to go from hOCR to page:
 * hOCR goes from print space (ocr_carea) to paragraphs, skipping the Text Region level.
 * PageXML does not have paragraphs.
 */

public class hOCRParser implements PageParser
{
	Document document;
	Page page = new Page();
	static Pattern wordPattern = Pattern.compile("\\w+"); // probleem met negatieve waarden
	
	hOCRCorrector corrector;

	
	public Map<String,String> parseTitle(String title)
	{
		Map<String,String> map = new HashMap<String,String>();
		String[] parts = title.split("\\s*;\\s*");
		for (String p: parts)
		{
			//System.err.println(p);
			Matcher m = wordPattern.matcher(p);
			if (m.find())
			{
				int e = m.end();
				int s = m.start();
				String name = p.substring(s,e);
				String value = p.substring(e+1);
				map.put(name, SimpleTokenizer.trim(value));
			}
		}
		return map;
	}
	
	public List<Point> getBoundingBox(String bbox)
	{
		List<Point> points = new ArrayList<Point>();
		String[] parts = bbox.split("\\s+");
		int x = Integer.parseInt(parts[0]);
		int y = Integer.parseInt(parts[1]);
		int x1 = Integer.parseInt(parts[2]);
		int y1 = Integer.parseInt(parts[3]);
		points.add(new Point(x,y));
		points.add(new Point(x,y1));
		points.add(new Point(x1,y1));
		points.add(new Point(x1,y));
		return points;
	}
	
	public Word getWord(Element we)
	{
		Map<String,String> m = parseTitle(we.getAttribute("title"));
		
		String text = we.getTextContent();
		Word w = new Word(text);
		w.id = we.getAttribute("id");
		corrector.addWord(w.id, we);
		//System.err.println("Hoi:" + text);
		List<Point> bb = getBoundingBox(m.get("bbox"));
		w.setBoundingPolygon(bb);
		return w;
	}
	
	
	public Page parsePage(String url)
	{
		this.document = TagSoupParser.parse2DOM(url);
		page = new Page();
		corrector = new hOCRCorrector();
		page.corrector = corrector;
		page.setDocument(document);
		
		NodeList divs = document.getElementsByTagName("div");
		
		for (int i=0; i < divs.getLength(); i++)
		{
			Element d = (Element) divs.item(i);
			if ((new String("ocr_page")).equals(d.getAttribute("class")))
			{
				Map<String,String> p = parseTitle(d.getAttribute("title"));
				page.id = p.get("image");
				
				List<Element> textRegions = 
					ParseUtils.getElementsByTagnameAndAttribute(d, "div", "class", "ocr_carea", false);
				
				// the above is wrong: ocr_carea is rather something like the 'print space'
				
				for (Element x: textRegions)
				{
					Map<String,String> pp = parseTitle(x.getAttribute("title"));
					TextRegion r = new TextRegion();
					r.id = x.getAttribute("id");
					page.addRegion(r);
					try
					{
						List<Point> regionBounds = getBoundingBox(pp.get("bbox"));
						r.setBoundingPolygon(regionBounds);
					} catch (Exception e)
					{
						System.err.println("Invalid region bounding box in page: " + 
								page.id + ", region " + r.id + ",: " + x.getAttribute("title"));
						e.printStackTrace();
					}
					List<Element> wordElements = 
						ParseUtils.getElementsByTagnameAndAttribute(x, "span", "class", "ocrx_word", false);
					
					for (Element we: wordElements)
					{
						r.addWord(getWord(we));
					}
				}
			}
		}
		return page;
	}
	
	public static void main(String[] args)
	{
		hOCRParser p = new hOCRParser();
		Page page =  p.parsePage(args[0]);
		page.marshal();
	}
}
