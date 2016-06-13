package org.impact.evaluation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.impact.evaluation.external.ExternalEvaluation;
import org.impact.evaluation.external.ExternalReport;
import org.impact.evaluation.metadata.MetadataProvider;
import org.impact.evaluation.metadata.TabSeparatedMetadataFile;
import org.impact.evaluation.parsers.PageParser;
import org.impact.evaluation.parsers.PageXMLParser;
import org.impact.evaluation.util.Options;
import org.impact.evaluation.util.XSLTTransformer;
import org.incava.util.diff.*;
import org.w3c.dom.*;

import java.util.Map;
import java.util.HashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
Simple word accuracy evaluator.
Probably character accuracy would be slow..
To do for more fair evaluation:
	- Fix 'tokenization errors' in OCR (superfluous white space before punctuation mark)
	- Delete some of the more obvious and frequent nonword errors from the 'large' dictionary

Add: 
-  collect statistics of frequent errors (words, characters) using alignment classes from spellingvar stuff
 */

@XmlRootElement
public class PageEvaluator 
{
	boolean outputAlignedXML = false;
	boolean printReport = false;
	@XmlElement
	String groundTruthLocation;
	@XmlElement
	String ocrLocation;
	WordList dictionary = null;
	@XmlElement
	WordList realWordErrors = new WordList();
	boolean printRealWordErrors = true;
	DocumentType ocrDocumentType=null;
	DocumentType gtDocumentType=null;
	@XmlAnyElement
	List<PageReport> pageReports = new ArrayList<PageReport>();
	@XmlElement
	PageReport Results = new PageReport();
	@XmlElement
	List<AggregateReport> aggregateReports = new ArrayList<AggregateReport>();

	boolean hasCustomPeriods = false;
	@XmlElement
	int tWords = 0;
	@XmlElement
	int tIncludedWords = 0;
	@XmlElement
	int tDiscardedWords = 0;
	@XmlElement
	int tRealWordErrors = 0;
	@XmlElement
	int tDictionaryWordsInGT = 0;
	@XmlElement
	double dictionaryCoverage = 0;
	@XmlElement
	int tWordsInGT = 0;
	@XmlElement
	int tMissedDictionaryWords = 0;
	@XmlElement
	int tMissedGTWords = 0;
	@XmlElement
	int tNEsInGT = 0;
	@XmlElement
	int tNEsFound = 0;
	@XmlElement
	double NERecall = 0;
	@XmlElement
	double dictionaryWordRecall = 0;
	@XmlElement
	int tCorrect = 0;
	@XmlElement
	int tWrong = 0;
	@XmlElement
	int nPages = 0;
	@XmlElement
	double precision;
	@XmlElement
	double strictPrecision;
	@XmlElement
	double recall;
	@XmlElement
	boolean caseSensitive=false;
	
	@XmlElement
	String compareTo="";
	
	MetadataProvider metadata = null;
	PrintStream report = null;
	String stylesheetLocation = "evaluation.xsl";
	
	private boolean writeHTML = true;
	
	ExternalReport externalReport = null;
	ExternalEvaluation externalEvaluation = null;
	
	public void addToReport(String s)
	{
		if (printReport)
		{
			report.println(s);
		}
	}


	public PageEvaluator()
	{
		try 
		{
			report = new PrintStream(System.out,true, "UTF8");
		} catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}
		String d = System.getenv("DICTIONARY");
		if (d != null)
		{
			this.dictionary = new WordList(d);
		}

		if (Options.getOption("externalEvaluation") != null)
		{
			try 
			{
				Class c = Class.forName(Options.getOption("externalEvaluation"));
				this.externalEvaluation = (ExternalEvaluation) c.newInstance();
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		
		gtDocumentType = new DocumentType();
		PageXMLParser p = new PageXMLParser();
		p.useWordTags = false;
		gtDocumentType.parser = p;
		gtDocumentType.extension = ".xml";

		ocrDocumentType = new DocumentType();
		p = new PageXMLParser();
		p.useWordTags = true;
		ocrDocumentType.parser = p;
		ocrDocumentType.extension = ".xml";
	}


	public class WordComparator implements Comparator<Word>
	{

		@Override
		public int compare(Word o1, Word o2) 
		{
			if (caseSensitive)
				return o1.tokenizedText.compareTo(o2.tokenizedText);
			else
				return o1.tokenizedText.compareToIgnoreCase(o2.tokenizedText);
		}
	}

	WordComparator wordComparator = new WordComparator();




	public void compareCharactersInPages(Page gtPage, Page ocrPage)
	{

	}	


	public boolean makeEvaluationReport(String gtURL, String ocrURL, File outputFile)
	{
		PageXMLParser gtParser = new PageXMLParser();
		gtParser.useWordTags = false;
		PageXMLParser ocrParser = new PageXMLParser();
		ocrParser.useWordTags = true;
		Page p1 = gtParser.parsePage(gtURL);
		Page p2 = ocrParser.parsePage(ocrURL);
		try
		{
			this.report = new PrintStream(outputFile);
			comparePages(p1,p2);
			report.close();
			return true;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public void setOutputFile(String fileName)
	{
		File f = new File(fileName);
		try 
		{
			FileOutputStream of = new FileOutputStream(f);
			report = new PrintStream(of,true,"UTF-8");
		} catch (Exception e) 
		{
			e.printStackTrace();
		}	
	}
	
	/*
	 * Region-by-region word-based diff
	 */
	
	public void comparePages(Page gtPage, Page ocrPage)
	{
		if (gtPage == null || ocrPage == null)
		{
			System.err.println("cannot compare null page!");
			return;
		}
		RegionMapper mapper = new RegionMapper();
		Page rearrangedOCR = mapper.putOCRWordsInGroundTruthRegions(gtPage, ocrPage,false);
		PageReport currentPageReport = new PageReport();

		// some awful hacks with the id's, mainly for KB data...
		
		currentPageReport.id = gtPage.id;
		currentPageReport.id = currentPageReport.id.replace(".tif","");
		
		if (currentPageReport.id.contains("dpo_") && !(currentPageReport.id.contains("_master")))
		{
			currentPageReport.id += "_master";
		}
		
		currentPageReport.imageFilename=ocrPage.id;
		currentPageReport.metadata = this.metadata;
		pageReports.add(currentPageReport);


		currentPageReport.nDiscardedWords = mapper.discardedWords.size();
		currentPageReport.discardedWords =  mapper.discardedWords;
		for (TextRegion r: ocrPage.textRegions)
		{
			for (Word w: r.words) 
			{ 
				if (w.tokenizedText.length() > 0) // do not count loose punctuation tokens
					currentPageReport.nWordsInOCR++; 
			};
		}

		for (TextRegion r: gtPage.textRegions)
		{
			for (Word w: r.words) 
			{ 
				if (w.tokenizedText.length() > 0)
					currentPageReport.nWordsInGT++; 
			};
			//currentPageReport.nWordsInGT += r.words.size(); // STOP: telt loshangende interpunctie mee 
		}

		for (TextRegion r: rearrangedOCR.textRegions)
		{
			TextRegion gtRegion = gtPage.getRegionById(r.id);
			boolean suspect = r.hasSuspectReadingOrder();
			Diff<Word> wordDiff = new Diff<Word>(r.words, gtRegion.words, wordComparator);
			List<Difference> diffs = wordDiff.diff();

			// int p1=0, p2=0;
			// addition means second argument to DIFF constructor has the extra elements, etc

			for (Word w: r.words) 
			{
				w.status = Word.CORRECT;
				if (dictionary != null)
				{
					w.inDictionary = dictionary.getFrequency(w.tokenizedText) > 0;
				}
			}

			if (dictionary != null)
			{
				for (Word w: gtRegion.words)
				{
					w.inDictionary = dictionary.getFrequency(w.tokenizedText) > 0;
					if (w.inDictionary)
						currentPageReport.nDictionaryWordsInGT++;
				}
			}

			/*
			 * The loop over differences is not enough. Alignment is necessary
			 */

			for (Difference d: diffs)
			{
				int de, ds, ae, as;
				

				/*
				 * Deleted means only in OCR
				 * Added means only in GT
				 */ 

				if ((de = d.getDeletedEnd()) >= 0 || d.getAddedEnd() >= 0)
				{
					Error error = new Error();
					error.regionId = r.id;
					currentPageReport.wordErrors.add(error);
					ds = d.getDeletedStart();
					ae = d.getAddedEnd();

					if (ae >= 0)
					{
						as = d.getAddedStart();
						for (int j=as; j <= ae; j++)
						{
							Word w = gtRegion.words.get(j);
							error.truth.add(w);
							if (w.tokenizedText.equals("")) // punctuation, not interesting...
							{
								continue;
							}
							w.status = Word.GROUNDTRUTH_ONLY;
							currentPageReport.nMissedGTWords ++;
							
							if (w.inDictionary)
							{
								currentPageReport.nMissedDictionaryWords++;
							}
						}
					}

					for (int i=ds; i <= de; i++)
					{
						Word w = r.words.get(i);
						error.ocr.add(w);
						if (w.tokenizedText.equals("")) // punctuation, not interesting...
						{
							continue;
						}
						w.status = Word.OCR_ONLY;

						if (w.inDictionary)
						{
							this.realWordErrors.incrementFrequency(w.tokenizedText, 1);
							currentPageReport.nRealWordErrors++;
						}
					}
				}
			}

			for (Word w: r.words) 
			{ 
				if (w.tokenizedText.length() > 0)
				{
					currentPageReport.nIncludedWords++;
					if (w.status==Word.CORRECT) 
						currentPageReport.nCorrect++; 
					else currentPageReport.nWrong++;
				}
				//System.err.println(w.text + ": " + (w.status==Word.CORRECT?"OK":"WRONG")); 
			};

			for (Word w: gtRegion.words)
			{
				if (w.type==Word.TYPE_NE)
				{
					currentPageReport.nNEsInGT++;
					if (w.status != Word.GROUNDTRUTH_ONLY)
						currentPageReport.nNEsFound++;
				}
			}
		}


		currentPageReport.calculateScores();
		Results.incorporate(currentPageReport);
		
		tWords += currentPageReport.nWordsInOCR;
		tIncludedWords += currentPageReport.nIncludedWords;
		tDiscardedWords += currentPageReport.nDiscardedWords;
		tRealWordErrors += currentPageReport.nRealWordErrors;
		tDictionaryWordsInGT += currentPageReport.nDictionaryWordsInGT;
		tWordsInGT += currentPageReport.nWordsInGT;
		tMissedGTWords += currentPageReport.nMissedGTWords;
		tMissedDictionaryWords += currentPageReport.nMissedDictionaryWords;
		tCorrect += currentPageReport.nCorrect;
		tWrong  += currentPageReport.nWrong;
		tNEsInGT += currentPageReport.nNEsInGT;
		tNEsFound += currentPageReport.nNEsFound;
		
		if (this.externalEvaluation != null)
			currentPageReport.externalReport = externalEvaluation.evaluate(gtPage, ocrPage);
	}

	public void compareFolders(String gtDirName, String ocrDirName)
	{
		File gtDir = new File(gtDirName);
		
		groundTruthLocation = gtDirName;
		ocrLocation = ocrDirName;
		
		PageParser gtParser = this.gtDocumentType.parser;
		PageParser ocrParser = this.ocrDocumentType.parser;

		// set options here...

		if (!gtDir.isDirectory()) // just compare files
		{
			try
			{
				comparePages(gtParser.parsePage(gtDirName), ocrParser.parsePage(ocrDirName));	
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			return;
		}
		File[] gtList = gtDir.listFiles();
		if (gtList != null)
		{
			nPages=0;
			for (File f: gtList)
			{
				String gtFileName = f.getName();
				String ocrFileName = gtFileName.replace(gtDocumentType.extension, ocrDocumentType.extension);
				String gtFile = gtDirName + "/" + gtFileName; 
				String ocrFile = ocrDirName + "/" + ocrFileName;
				File ocrFileTest = new File(ocrFile);

				if (ocrFileTest.exists())
				{
					try
					{
						comparePages(gtParser.parsePage(gtFile), ocrParser.parsePage(ocrFile));
					} catch (Exception e)
					{
						e.printStackTrace();
					}
					nPages++;
				} else
				{
					System.err.println("No OCR found at " + ocrFile);
				}
			}
			Results.calculateScores();
			try
			{
				setupAggregateReports();
				for (AggregateReport r: aggregateReports)
				{
					for (PageReport p: pageReports)
					{
						r.incorporate(p);
					}
					r.calculateScores();
				}
			} catch (Exception e) // should be due to missing metadata
			{
				e.printStackTrace();
			}
			calculateScores();
		} else
		{
		}
	}

	/*
	 * Make summaries for each century 
	 * and each separate title
	 */
	
	private void setupAggregateReports() 
	{
		Set<String> titles = new HashSet<String>();
		for (PageReport p: pageReports)
			titles.add(p.metadata.getMetadata(p.id,"title"));
		
		if (!hasCustomPeriods)
		{
			final String[] centuries = {"15", "16", "17", "18", "19"};

			for (String c: centuries)
			{
				final String cc = c;
				AggregateReport r  = new AggregateReport()
				{
					public boolean filter(PageReport p)
					{
						try
						{
							return p.metadata.getMetadata(p.id,"year").startsWith(cc);
						} catch (Exception e)
						{
							return false;
						}
					}
					public String getFilter()
					{
						return "century=" + cc;
					}
				};	
				aggregateReports.add(r);
			}
		}
		
		for (String t: titles)
		{
			final String s = t;
			if (s != null)
			{
				AggregateReport r  = new AggregateReport()
				{
					public boolean filter(PageReport p)
					{
						return s.equals(p.metadata.getMetadata(p.id,"title"));
					}
					
					public String getFilter()
					{
						return "title=" + s;
					}
				};
				this.aggregateReports.add(r);
			}
		}
	}


	protected void calculateScores() 
	{
		double dTotal = tWords;
		strictPrecision = ((double) tCorrect) / tWords;
		precision = ((double) tCorrect) / tIncludedWords;
		double dGTWords = tWordsInGT;
		recall = (tWordsInGT - tMissedGTWords) / dGTWords;

		double t = tNEsInGT;
		NERecall = (tNEsFound / t);

		dictionaryCoverage = tDictionaryWordsInGT / (double) this.tWordsInGT;	
		
		if (this.tDictionaryWordsInGT > 0)
			dictionaryWordRecall = (this.tDictionaryWordsInGT - this.tMissedDictionaryWords) 
									/ (double) this.tDictionaryWordsInGT;
	}

	/*
	 * Use JAXB to generate an XML dump of the evaluation results
	 */
	
	public void marshal()
	{
		try
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(new Class[] {
					org.impact.evaluation.PageEvaluator.class,
					org.impact.evaluation.PageReport.class,
					org.impact.evaluation.TextRegion.class,
					org.impact.evaluation.Word.class,
					org.impact.evaluation.WordList.class,
					org.impact.evaluation.WordList.TypeFrequency.class,
					org.impact.evaluation.Error.class,
					org.impact.evaluation.metadata.KeyValuePair.class,
					org.impact.evaluation.external.ExternalReport.class});


			Marshaller marshaller=jaxbContext.createMarshaller();
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_ENCODING,"UTF-8");
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true);
			
			report.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			report.println("<?xml-stylesheet type=\"text/xsl\" href=\"" + stylesheetLocation 
					+ "\"?>");
			
			marshaller.marshal( this, report);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		/*
		 * It is more convenient for distribution to directyly produce HTML as well
		 */
		if (writeHTML)
		{
			try
			{
				XSLTTransformer t 
				= new XSLTTransformer(
						org.impact.evaluation.util.Resource.resolveFileName("XSL/evaluation.xsl").toString());
				if (t != null)
				{
					String xmlFile = Options.getOption("outputFilename");
					t.transformFile(xmlFile, xmlFile + ".html");
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	protected void processOptions()
	{
		if (Options.getOption("dictionary") != null)
		{
			String dictFile = Options.getOption("dictionary") ;
			System.err.println("Using dictionary " + dictFile);
			if (!dictFile.equalsIgnoreCase("NONE"))
			{
				WordList list = new WordList(dictFile);
				this.dictionary = list;
			}
		}
		if (Options.getOption("stylesheetLocation") != null)
		{
			this.stylesheetLocation = Options.getOption("stylesheetLocation");
		}
		if (Options.getOption("metadataFile") != null)
		{
			this.metadata = new TabSeparatedMetadataFile();
			this.metadata.readFromFile(Options.getOption("metadataFile"));
		}
		if (Options.getOption("outputFilename") != null)
		{
			this.setOutputFile(Options.getOption("outputFilename"));
		}
		if (Options.getOption("compareTo") != null)
		{
			String c = Options.getOption("compareTo");
			if (!c.equalsIgnoreCase("none"))
			{
				this.compareTo = c;
			}
		}
		if (Options.getOption("periodization") != null)
		{
			String p = Options.getOption("periodization");
			if (!p.equalsIgnoreCase("default"))
			{
				hasCustomPeriods = true;
				try
				{
					String[] parts = p.split(",");
					for (String part: parts)
					{
						String[] ul = part.split("-");
						final String l = ul[0];
						final String u = ul[1];
						AggregateReport r  = new AggregateReport()
						{
							public boolean filter(PageReport p)
							{
								try
								{
									String year = p.metadata.getMetadata(p.id,"year");
									return (year.compareTo(l) >= 0 && year.compareTo(u) <= 0);
								} catch (Exception e)
								{
									return false;
								}
							}
							public String getFilter()
							{
								return "period=" + l + "-" + u;
							}
						};	
						aggregateReports.add(r);
					} 
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String [] args)
	{
		Options options = new Options(args);
		args = options.commandLine.getArgs();
		PageEvaluator pc = new PageEvaluator();
		
		String gtFile = args[0];
		String ocrFile = args[1];
		pc.processOptions();

		
		pc.compareFolders(gtFile,ocrFile);
		pc.calculateScores();		
		pc.marshal();
	}
}
