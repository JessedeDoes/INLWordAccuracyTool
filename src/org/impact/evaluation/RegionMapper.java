package org.impact.evaluation;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.impact.evaluation.parsers.PageXMLParser;
import org.impact.evaluation.parsers.hOCRParser;
import org.impact.evaluation.util.ParseUtils;
import org.impact.evaluation.util.PermutationGenerator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RegionMapper 
{
	public List<Word> discardedWords = new ArrayList<Word>();
	PrintStream report = System.out;
	static int MAX_REGIONS_FOR_REPERMUTATION=7;
	
	public Page putOCRWordsInGroundTruthRegions(Page gtPage, Page ocrPage, boolean outputAlignedXML)
	{
		Page reArrangedOCRPage = new Page();
		/*
		 * The following leads to mysterious errors while trying to clone within the web service
		 * whereas it works correctly in the standalone version (? different XML libraries)
		 */
		boolean hasCorrectClone = false;

		if (hasCorrectClone)
			reArrangedOCRPage.document = ParseUtils.cloneDocument(gtPage.document);
		else
			reArrangedOCRPage.document= gtPage.document; // thus destroying the original document
		
		//System.err.println(p.document.getDocumentElement());
		
		discardedWords.clear();		
		
		for (TextRegion r1: gtPage.textRegions)
		{
			reArrangedOCRPage.addRegion(r1.cloneShapeOnly());
		}
		
		Map<String,Set<String>> regionIntersectionMap = new HashMap<String,Set<String>>();
		
		/*
		 * The loop below is of course slow if there are many words and many OCR regions....
		 * Could be sped up with a quad tree or whatever, but the procedure runs fine as is
		 * for our current OCR sets, so never mind...
		 */
		
		for (TextRegion r: ocrPage.textRegions)
		{
			for (Word w: r.words)
			{
				boolean foundRegion = false;
				for (TextRegion r1: gtPage.textRegions)
				{
					boolean seenInThisOCRRegion = false;
					if (r1.contains(w))
					{
						if (!seenInThisOCRRegion)
						{
							Set<String> set = regionIntersectionMap.get(r1.id);
							if (set == null)
							{
								set = new HashSet<String>();
								regionIntersectionMap.put(r1.id,set);
							}
							set.add(r.id);
							seenInThisOCRRegion = true;
						}
						reArrangedOCRPage.getRegionById(r1.id).addWord(w);
						foundRegion=true;
						break;
					}
				}
				if (!foundRegion)
				{
					w.status = Word.OCR_ONLY;
					discardedWords.add(w);
				}
			}
		}
		
		
		for (TextRegion r: reArrangedOCRPage.textRegions)
		{
			Set<String> ocrRegionIds = regionIntersectionMap.get(r.id);
			if (ocrRegionIds != null && ocrRegionIds.size() > 1)
			{
				System.err.println("Page " + gtPage.id + ": Multiple OCR regions for GT region " + r.id + ocrRegionIds);
			}
			if (r.hasSuspectReadingOrder()) // this check is not very good for wobbly lines...
			{
				System.err.println("Page " + gtPage.id + ": Possible reading order problem in region: " + 
						r.id + " : " + r.readingOrderProblems);
			}
			if (r.readingOrderProblems > 0 && ocrRegionIds.size() > 1 && 
					ocrRegionIds.size() <= MAX_REGIONS_FOR_REPERMUTATION)
			{
				tryToSolveReadingOrderProblemsByPermutationOfOCRRegions(r, ocrPage, ocrRegionIds);
			}
		}
		
		// now also adapt the XML for the OCR document
		if (outputAlignedXML)
			makeXML(reArrangedOCRPage);
		return reArrangedOCRPage;
	}

	private void makeXML(Page reArrangedOCRPage) 
	{
		try
		{
			List<Element> textRegions = ParseUtils.getElementsByTagname(reArrangedOCRPage.document.getDocumentElement(), "TextRegion", true);
			for (Element e: textRegions)
			{
				String id = e.getAttribute("id");

				TextRegion tr = reArrangedOCRPage.getRegionById(id);
				List<String> l = new ArrayList<String>();
				for (Word w: tr.words)
				{
					l.add(w.text); // this is OK, echo original text.
					// But maybe replace the ligatures here???
				}
				org.w3c.dom.Text t = reArrangedOCRPage.document.createTextNode(ParseUtils.join(l, " "));
				// remove all children..
				NodeList c = e.getChildNodes();
				List<Node> cc = new ArrayList<Node>();
				// is this really necessary ?
				for (int i=0; i < c.getLength(); i++)
				{
					Node x = c.item(i);
					cc.add(x);
				}
				for (Node z: cc) e.removeChild(z);
				
				Element textequivNode =  reArrangedOCRPage.document.createElement("TextEquiv");
				textequivNode.setAttribute("type", "rearrangedOCR");
				Element unicodeNode =  reArrangedOCRPage.document.createElement("Unicode");
				e.appendChild(textequivNode);
				textequivNode.appendChild(unicodeNode);
				unicodeNode.appendChild(t); 
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * This procedure is naive but works reasonably well.
	 * The reading order problem detection (Cf TextRegion) is not good enough..
	 */
	
	public void tryToSolveReadingOrderProblemsByPermutationOfOCRRegions(TextRegion region, 
			Page ocrPage, Set<String> ocrRegionIds)
	{
		int[] indices;
		String[] elements = new String[ocrRegionIds.size()];
		elements = ocrRegionIds.toArray(elements);
		
	
		int bestSoFar = region.readingOrderProblems;
		List<TextRegion> bestPermutation = 
			new ArrayList<TextRegion>();
		boolean rearrangingHelps = false;
		
		PermutationGenerator x = new PermutationGenerator (ocrRegionIds.size());
		
		while (x.hasMore ()) 
		{
			TextRegion testRegion = new TextRegion();
			testRegion.id = "testRegion";
			List<TextRegion> ocrRegions = new ArrayList<TextRegion>();
			indices = x.getNext ();
			for (int i = 0; i < indices.length; i++) 
			{
				ocrRegions.add(ocrPage.getRegionById(elements[indices[i]]));
			};
			for (TextRegion ocrRegion: ocrRegions)
			{
				for (Word w:ocrRegion.words)
				{
					if (region.contains(w)) // inner loop... optimize here please...
						testRegion.addWord(w);
				}
			}
			testRegion.hasSuspectReadingOrder();
			
			if (testRegion.readingOrderProblems < bestSoFar)
			{
				//System.err.println("can do better than " + bestSoFar + " : " + testRegion.readingOrderProblems);
				bestSoFar = testRegion.readingOrderProblems;
				bestPermutation.clear();
				bestPermutation.addAll(ocrRegions);
				rearrangingHelps = true;
			}
		}
		
		if (rearrangingHelps)
		{
			System.err.println("Polished up reading order... \n\tOriginal order "  + region.readingOrderProblems + " words affected");
			int k=0;
			boolean printSomeDebugInfo = false;
			
			if (printSomeDebugInfo) for (Word w: region.words)
			{
				System.err.print(w.text  + " ");
				if (k++ % 10 ==0)
					System.err.println(" ");
				System.err.println("");
			}
			System.err.println("\tNew order: " + bestSoFar + " words affected");
			
			region.words.clear();
			
			for (TextRegion r: bestPermutation)
				for (Word w: r.words)
					if (region.contains(w))
							region.addWord(w);
			k=0;
			if (printSomeDebugInfo) for (Word w: region.words)
			{
				System.err.print(w.text  + " ");
				if (k++ % 10 ==0)
					System.err.println(" ");
			}
			// reset suspect word order flags
			region.hasSuspectReadingOrder();
		}
	}
	
	public boolean alignReadingOrder(String gtURL, String ocrURL, File outputFile)
	{
		PageXMLParser gtParser = new PageXMLParser();
		gtParser.useWordTags = false;
		PageXMLParser ocrParser = new PageXMLParser();
		ocrParser.useWordTags = true;
		System.err.println("ground truth: " + gtURL + " OCR:" + ocrURL);
		Page p1 = gtParser.parsePage(gtURL);
		Page p2 = ocrParser.parsePage(ocrURL);
		return mapRegions(outputFile, p1, p2);
		//return false;
	}
	
	public boolean alignReadingOrderHOCR(String gtURL, String ocrURL, File outputFile)
	{
		PageXMLParser gtParser = new PageXMLParser();
		gtParser.useWordTags = true;
		hOCRParser ocrParser = new hOCRParser();
		//ocrParser.useWordTags = true;
		System.err.println("ground truth: " + gtURL + " OCR:" + ocrURL);
		Page p1 = gtParser.parsePage(gtURL);
		Page p2 = ocrParser.parsePage(ocrURL);
		return mapRegions(outputFile, p1, p2);
		//return false;
	}

	private boolean mapRegions(File outputFile, Page p1, Page p2) 
	{
		Page p3 = (new RegionMapper()).putOCRWordsInGroundTruthRegions(p1, p2,true); // this is not quite "it"
		String xml = ParseUtils.documentToString(p3.document);
		try
		{
			report = new PrintStream(outputFile);
			report.print(xml);
			report.close();
			return true;
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static void main(String [] args)
	{
		RegionMapper rm = new RegionMapper();
		String gtFile = args[0];
		String ocrFile = args[1];
		rm.alignReadingOrderHOCR(gtFile,ocrFile,new File("hiep.xml"));
	}
}
