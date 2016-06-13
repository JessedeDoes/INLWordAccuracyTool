package org.impact.evaluation.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.impact.evaluation.util.ParseUtils;


public class TabSeparatedMetadataFile implements MetadataProvider 
{
	String[] fieldNames;
	int idField=-1;
	Map<String,Properties> metadataMap = new HashMap<String,Properties>();
	
	public TabSeparatedMetadataFile() // default order for impact
	{
		String[] x = 
			{"title", "id", "institution", "subset1", "subset2", "year", "dictionary", "patterns"};
		setFieldNames(x);
	}
	
	public void setFieldNames(String[] fieldNames)
	{
		this.fieldNames = fieldNames;
		for (int i=0; i < fieldNames.length; i++)
		{
			if (fieldNames[i].equalsIgnoreCase("id"))
			{
				idField=i;
			}
		}
	}
	
	public TabSeparatedMetadataFile(String[] fieldNames)
	{
		setFieldNames(fieldNames);
	}
	@Override
	public boolean readFromFile(String fileName) 
	{
		// TODO Auto-generated method stub
		try 
		{
			BufferedReader b = ParseUtils.openBufferedTextFile(fileName);
			String line;
			while ((line = b.readLine()) != null)
			{
				String[] fields = line.split("\\s*\t\\s*");
				String id = fields[idField];
				Properties p = new Properties();
				metadataMap.put(id,p);
				// sometimes, we removed leading zeros, so put them back again..
				try
				{
					String longId = String.format("%08d", Integer.parseInt(id));
					metadataMap.put(longId,p);
				} catch (NumberFormatException e)
				{
					// just do nothing here if id is not a number
				}
				for (int i=0; i < fields.length; i++)
				{
					if (i < fieldNames.length)
						p.put(fieldNames[i],fields[i]);
				}
			}
		} catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	
	@Override
	public String getMetadata(String id, String fieldName) 
	{
		// TODO Auto-generated method stub
		try
		{
			return metadataMap.get(id).getProperty(fieldName);
		} catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public String[] getFields() 
	{
		// TODO Auto-generated method stub
		return fieldNames;
	}

	@Override
	/*
	 * Only for marshaling to XML
	 */
	public List<KeyValuePair> getMetadata(String id) 
	{
		// TODO Auto-generated method stub
		Properties p = metadataMap.get(id);
		if (p==null)
		{
			System.err.println("Yikes: no metadata found for: " + id);
			return null;
		}
		List<KeyValuePair> keyvals = new ArrayList<KeyValuePair>();
		Set<Object> keys = p.keySet();
		for (String key:  p.stringPropertyNames())
		{
			String value = p.getProperty(key);
			keyvals.add(new KeyValuePair(key,value));
		}
		return keyvals;
	}
}
