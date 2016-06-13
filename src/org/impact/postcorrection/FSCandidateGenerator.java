package org.impact.postcorrection;

import java.util.Set;

public class FSCandidateGenerator implements CandidateGenerator
{
	public  void addCandidates(Set<String> candidates, String w)
	{
		addCandidates(candidates,w,0);
	}
	
	private static void addCandidates(Set<String> candidates, String w, int p)
	{
		if (p >= w.length())
			return;
		if (w.charAt(p) == 'f')
		{
			StringBuffer w1 = new StringBuffer(w);
			w1.setCharAt(p,'s');
			candidates.add(w1.toString());
			addCandidates(candidates,w1.toString(),p+1);
		}
		addCandidates(candidates, w,p+1);
	}
}
