package com.plutext.dedupe;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.docx4j.TextUtils;
import org.docx4j.TraversalUtil.CallbackImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;

import com.bitlove.fnv.FNV;

import info.debatty.java.lsh.MinHash;

public class DocxToVector {
	
	/*
	 * What's a good short fingerprint for a docx?
	 * 
	 * algorithm:  get first and last 5 non-empty paragraphs.  compute hash of each to make vector.
			otherwise:
			if < 10 words, put in bad bucket
			else get words, divide into 10 strings

	 * 
	 * 
	 */
	
	WordprocessingMLPackage pkg;
	
	private final static int MIN_P_LENGTH = 5;
	private final static int HEAD_LENGTH = 5;

	public DocxToVector(WordprocessingMLPackage pkg) {
		
		this.pkg = pkg;
		
	}
	
	
	List<String> contents  = new ArrayList<String>();
	
	void findStrings() throws SmallFileException {
		
		if (pkg.getMainDocumentPart()==null
				|| pkg.getMainDocumentPart().getContent().size()<HEAD_LENGTH) {

			// the bad bucket we can just compare the XML I guess
			throw new SmallFileException("Not enough content");
		}
		
		// get first 5 paragraphs - optimistic approach
		int i=0;
		do {
			Object o = pkg.getMainDocumentPart().getContent().get(i);
			if (o instanceof P) {
				
				StringWriter sw = new StringWriter(); 
				try {
					TextUtils.extractText(o, sw);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String pText = sw.toString();
				if (pText!=null
						&& pText.length()>MIN_P_LENGTH) {
					// good, add it
					contents.add(pText);
				} else {
					// ignore
				}
			} else if (o instanceof Tbl) {
				
				PFinder pFinder = new PFinder(HEAD_LENGTH-contents.size());
				pFinder.walkJAXBElements(o);
				
				contents.addAll(pFinder.contents);
			}
			i++;
		} while (contents.size()<HEAD_LENGTH 
				&& i<pkg.getMainDocumentPart().getContent().size());
		
		// Did we get 5?  If not, put in bad bucket
		if (contents.size()>=HEAD_LENGTH) {
			//System.out.println("ok..");
		} else {
			// the bad bucket we can just compare the XML I guess
			throw new SmallFileException("Not enough content");			
		}
		
		// Now get some from the end.  But don't go back so far that we overlap!
		
	}
	
	Set<Integer> hashStrings() {
		
		// Now convert the strings to vectors
		// https://softwareengineering.stackexchange.com/questions/49550/which-hashing-algorithm-is-best-for-uniqueness-and-speed/145633#145633
		FNV fnv = new FNV();
		
		Set<Integer> set = new HashSet<Integer>();
		
		for (String s : contents) {
			
			try {
				set.add(fnv.fnv1a_32(s.getBytes("UTF-8")).intValue());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		return set;
	}
	
	
	
	public class  PFinder extends CallbackImpl {
		
		int numSought;
		
		PFinder(int numSought) {
			this.numSought = numSought;
		}
		
		List<String> contents = new ArrayList<String>();
		
		boolean done = false;
				
		@Override
		public List<Object> apply(Object o) {
			
			if (o instanceof P ) {
				
				StringWriter sw = new StringWriter(); 
				try {
					TextUtils.extractText(o, sw);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String pText = sw.toString();
				if (pText!=null
						&& pText.length()>MIN_P_LENGTH) {
					// good, add it
					contents.add(pText);
					
					if (contents.size()>=numSought) {
						done = true;
					}
				} else {
					// ignore
				}
			}			
			return null; 
		}
		
		@Override
		public boolean shouldTraverse(Object o) {
			
			return (!done );
		}
	}	
	
}
