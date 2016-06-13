package org.impact.evaluation.metadata;

import java.util.List;

public interface MetadataProvider 
{
	public boolean readFromFile(String fileName);
	public String getMetadata(String id, String fieldName);
	public String[] getFields();
	public List<KeyValuePair> getMetadata(String id);
}