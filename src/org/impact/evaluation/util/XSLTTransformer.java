package org.impact.evaluation.util;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.Properties;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Wrapper voor de xsltc engine. Deze klasse zet met behulp van de XSLT engine (o.a. Transformer.java) een XML
 * artikel om in HTML.
 */

public class XSLTTransformer
{
	/** our Transformer object */
	private Transformer transformer = null;
	private TransformerFactory tFactory;
	private String xslInUri;
	private boolean useSaxon = true;
	
	public XSLTTransformer(String xslInUri)
	{
		String key = "javax.xml.transform.TransformerFactory";
		// N.B. Aangepast ivm nieuwe Javaversie (1.5 of 1.6), was vroeger:
		// com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl
		String value = "org.apache.xalan.processor.TransformerFactoryImpl";
		if (useSaxon)
		{
			value = "net.sf.saxon.TransformerFactoryImpl";
		}
		Properties props = System.getProperties();
		props.put(key, value);
		System.setProperties(props);
		tFactory = TransformerFactory.newInstance();
		this.xslInUri = xslInUri;
		try 
		{
			this.transformer = tFactory.newTransformer(new StreamSource(xslInUri));
		} catch (TransformerConfigurationException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public XSLTTransformer(Transformer transformer)
	{
		this.transformer = transformer;
	}

	/**
	 * Voert de transformatie uit. De input bestaat uit een String met de XML code.
	 * 
	 * @param instring
	 *            De input string
	 * @param out
	 *            java.io.Writer object Het resultaat van het transformeren van de XML code.
	 * @param licenseAccepted 
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 * @throws IOException
	 */
	
	public void transformString(String instring, Writer out)
		throws TransformerConfigurationException, TransformerException, IOException
	{
		StreamSource source = new StreamSource(new StringReader(instring));
		StreamResult result = new StreamResult(out);
		transformer.transform(source, result);
		out.flush();
	}
	
	public void transformFile(String inFileName, String outFileName)
	{
		try 
		{
			BufferedReader br = 
				org.impact.evaluation.util.ParseUtils.openBufferedTextFile(inFileName);
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outFileName),"UTF-8");
			StreamSource source = new StreamSource(br);
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);
			br.close();
			out.close();
		} catch (Exception e) 
		{
			System.err.println("failed to open " + inFileName);
			e.printStackTrace();
		}
	}

	/**
	 * Releases the transformer object to the free pool, to be reused next time.
	 * 
	 */
	
	public void release()
	{
	}
}

