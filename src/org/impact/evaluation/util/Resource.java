package org.impact.evaluation.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import java.io.InputStream;
import java.net.URL;

public class Resource

{

	/** Creates a new instance of Resource **/
	
	public Resource()
	{

	}

	public static URL resolveFileName(String s)
	{
		try // first try local filesystem
		{
			File file = new File(s);
			if (file.exists() && file.canRead())
			{
				System.err.println("Using local file system for " + s);
				return file.toURI().toURL();
			}
			// next try from jar
			java.net.URL url = Resource.class.getClassLoader().getResource(s);
			if (url != null)
			{
				System.err.println("Using jar for " + s + ": " + url);
				return url;
			}
			System.err.println("Fall back to whatever for " + s);
			return new java.net.URL(s);
		} catch (Exception e)
		{ 
			e.printStackTrace();
		}
		return null;
	}
	
	public Reader openFile(String s)
	{
		try 
		{
			// first try to read file from local file system
			File file = new File(s);
			if (file.exists())
			{
				return new FileReader(file);
			}
			// next try for files included in jar
			java.net.URL url = getClass().getClassLoader().getResource(s);
			System.err.println(url);
			// or URL from web
			if (url == null) url = new java.net.URL(s);
			java.net.URLConnection site = url.openConnection();
			InputStream is = site.getInputStream();
			return new InputStreamReader(is);
		} catch (IOException ioe)
		{
			System.err.println("Could not open " + s);
			return null;
		}
	}
	
	public static Reader openResourceFile(String s)
	{
		return new  Resource().openFile(s);
	}
}
