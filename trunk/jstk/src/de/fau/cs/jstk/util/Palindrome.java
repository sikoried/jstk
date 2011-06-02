/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet

	This file is part of the Java Speech Toolkit (JSTK).

	The JSTK is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	The JSTK is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with the JSTK. If not, see <http://www.gnu.org/licenses/>.
*/
package de.fau.cs.jstk.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Use the Palindrome class to find palindromes in a sequence of Objects. It
 * uses the equals method, so make sure it is implemented!
 * 
 * @author sikoried
 */
public final class Palindrome {

	/**
	 * Find the longest palindrome in the sequence and return the position and
	 * length
	 * @param sequence
	 * @return Pair(position, length)
	 */
	public static Pair<Integer, Integer> findLongest(Object [] sequence) {
		List<Integer> all = analyze(sequence);
		Iterator<Integer> it = all.iterator();
		int l = it.next();
		int p = 0;
		int i = 1;
		while (it.hasNext()) {
			int cl = it.next();
			if (cl > l) {
				l = cl;
				p = i / 2;
			}
			i++;
		}
		
		return new Pair<Integer, Integer>(p, l);
	}
	
	/**
	 * Find all palindromes in the sequence and return a list of index-length 
	 * pairs. If desired, the list is sorted longest palindrome first.
	 * @param sequence
	 * @param min
	 * @return
	 */
	public static List<Pair<Integer, Integer>> findAll(Object [] sequence, int min, boolean sort) {
		LinkedList<Pair<Integer, Integer>> li = new LinkedList<Pair<Integer, Integer>>();
		List<Integer> all = analyze(sequence);
		Iterator<Integer> it = all.iterator();
		int i = 0;
		while (it.hasNext()) {
			int len = it.next();
			if (len >= min)
				li.add(new Pair<Integer, Integer>(i/2, len));
			i++;
		}
		
		// sort the list
		if (sort) {
			Collections.sort(li, new Comparator<Pair<Integer, Integer>>() {
				public int compare(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) {
					return p2.b - p1.b;
				}
			});
		}
		
		return li;
	}
	
	/**
	 * Get a list of all palindromes in the sequence. The output list has twice
	 * the size of the input list and contains the length of the palindrome
	 * starting at each index, i.e. findAll().get(0) ~ (even) length of
	 * palindrome at center index 0, findAll().get(1) ~ (uneven) length of 
	 * palindrome at index 0
	 * @param sequence
	 * @return
	 */
	public static List<Integer> analyze(Object [] sequence) {
		// this O(n) implementation is based on
		// http://www.akalin.cx/2007/11/28/finding-the-longest-palindromic-substring-in-linear-time/
		
		LinkedList<Integer> l = new LinkedList<Integer>();
		int i = 0;
		int len = 0;
		while (i < sequence.length) {
			if ((i > len) && (sequence[i - len - 1].equals(sequence[i]))) {
				len += 2;
				i += 1;
				continue;
			}
			
			l.add(len);
			
			int s = l.size() - 2;
			int e = s - len;
			
			boolean found = false;
			for (int j = s; j > e; j--) {
				int d = j - e - 1;
				if ((l.get(j)) == d)	{
					len = d;
					found = true;
					break;
				}
				
				l.add(Math.min(d, l.get(j)));
			}
			
			if (!found) {
				i += 1;
				len = 1;
			}
		}
		
		l.add(len);
	
	    int llen = l.size();
	    int s = llen - 2;
	    int e = s - (2 * sequence.length + 1 - llen);
	    for (i = s; i > e; --i) {
	        int d = i - e - 1;
	        l.add(Math.min(d, l.get(i)));
	    }
	    
	    return l;
	}
	
	public static final String SYNOPSIS =
		"usage: util.Palindrome min-length < words > list-of-palindromes (index:length)";
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		int min = Integer.parseInt(args[0]);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while ((line = br.readLine()) != null) {
			String [] spl = line.split("\\s+");
			if (spl.length < 2)
				continue;
			List<Pair<Integer, Integer>> pl = findAll(spl, min, true);
			for (Pair<Integer, Integer> p : pl)
				System.out.print(p.a + ":" + p.b + " ");
			System.out.println();
		}
	}
}
