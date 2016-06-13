package org.impact.postcorrection;

import java.util.Set;

public interface CandidateGenerator 
{
	public   void addCandidates(Set<String> candidates, String w);
}
