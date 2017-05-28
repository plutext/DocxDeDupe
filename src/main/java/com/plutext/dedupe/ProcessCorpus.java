package com.plutext.dedupe;

import info.debatty.java.lsh.MinHash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.exceptions.Docx4JException;

/**
 * Process a bunch of Word documents, identifying duplicates
 * and other problems
 * 
 * Only handles docx files, not binary .doc.
 * 
 * But that's ok, coz we can only do cool stuff with docx anyway.
 * 
 * TODO: command line
 * 
 * 
 * @author jharrop
 *
 */
public class ProcessCorpus {

	/* i */ public  static String DIR_IN = "C:\\Users\\jharrop\\Documents\\Florian\\corpusDDD";
	/* o */ public  static String DIR_OUT = "C:\\Users\\jharrop\\Documents\\Florian\\corpus-exceptions";
	
	public final static String DIR_OUT_MAIN = "main";
	public final static String DIR_OUT_DUPLICATES = "duplicates";
	public final static String DIR_OUT_LARGE = "large"; // download will be too big, so just move for now
	public final static String DIR_OUT_SHORT_TEXT = "short";
	public final static String DIR_OUT_SHORT_TEXT_LARGE_FILE = "short-but-large";
	public final static String DIR_OUT_BINARY = "binary";
	public final static String DIR_OUT_PPTX = "pptx";
	public final static String DIR_OUT_XLSX = "xlsx";
	public final static String DIR_OUT_PARSE = "issue-parse";
	public final static String DIR_OUT_ZIP = "issue-zip";
	public final static String DIR_OUT_UNKNOWN = "issue-unknown";
	
	public final static int BIG_FILE = 6*1024*1024; // 6MB
	public final static int SHORT_TEXT_LARGE_FILE = 150*1024; // 150 KB
	
	public final boolean MOVE = true;
	
	public static void main(String[] args) throws IOException {
		
		OptionParser parser = new OptionParser();
		parser.accepts( "i" ).withRequiredArg().ofType( String.class );
		parser.accepts( "o" ).withRequiredArg().ofType( String.class );
		OptionSet options = parser.parse(args);
		
		DIR_IN =  (String)options.valueOf("i");
		DIR_OUT =  (String)options.valueOf("o");
		
		// TODO Auto-generated method stub

        File dir_out = new File( DIR_OUT );
        if (!dir_out.exists()) {
        	dir_out.mkdirs();
        }
        // On Windows at least, these are created as required, but do it explicitly
        new File( DIR_OUT + File.separator + DIR_OUT_MAIN).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_DUPLICATES).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_LARGE).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_SHORT_TEXT).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_SHORT_TEXT_LARGE_FILE).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_BINARY).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_PPTX).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_XLSX).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_PARSE).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_ZIP).mkdirs();
        new File( DIR_OUT + File.separator + DIR_OUT_UNKNOWN).mkdirs();
		
		ProcessCorpus pc = new ProcessCorpus();
		pc.walk(DIR_IN);

		pc.pairs();

	}
	
	
	int docNum = 1;
	
    public void walk( String path ) throws IOException {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;
        
        System.out.println(list.length + " files...");

        for ( File f : list ) {
        	
        	docNum++;
        	
            if ( f.isDirectory() ) {
//                walk( f.getAbsolutePath() );
                //System.out.println( "Dir:" + f.getAbsoluteFile() );
            }
            else {
//                System.out.println( "File:" + f.getAbsoluteFile() );
                
            	FileInputStream fis = null;
//                if (f.getName().endsWith("doc")
//                		|| f.getName().endsWith("bin")
//                		|| f.getName().endsWith("docx")
//                		|| f.getName().endsWith("docm")) {
                	
                	try {
                		
                		if (f.length()>BIG_FILE ) {
                			
							if (MOVE) {
								FileUtils.moveFile(f, new File(DIR_OUT+ File.separator + DIR_OUT_LARGE + File.separator + f.getName()));
							}
                			continue;
                		} 
                		
                		fis = new FileInputStream(f);  // so we can close it, so move is possible
						handle(f, fis) ;
	                	
					} catch (SmallFileException e) {

						if (MOVE) {
	                		if (f.length()>SHORT_TEXT_LARGE_FILE ) {
	                			FileUtils.moveFile(f, new File(DIR_OUT+ File.separator + DIR_OUT_SHORT_TEXT_LARGE_FILE + File.separator + f.getName()));
	                		} else {
	                			FileUtils.moveFile(f, new File(DIR_OUT+ File.separator + DIR_OUT_SHORT_TEXT + File.separator + f.getName()));	                			
	                		}
						}
						
					} catch (Exception e) {
						
						fis.close(); // so we can delete
						
						//System.out.println(e.getMessage());
						
						if (e.getMessage()!=null
								&& e.getMessage().startsWith("This file seems to be a binary doc")) {
							
							if (MOVE) {
								FileUtils.moveFile(f, new File(DIR_OUT+ File.separator + DIR_OUT_BINARY + File.separator + f.getName()));
							}

						} else if (e.getMessage()!=null
								&& e.getMessage().startsWith("Couldn't load xml from stream")) {
							
							if (MOVE) {
								FileUtils.moveFile(f, new File(DIR_OUT+ File.separator + DIR_OUT_PARSE + File.separator + f.getName()));
							}

						} else if (e.getMessage()!=null
								&& e.getMessage().contains("PresentationMLPackage")) {
							
							if (MOVE) {
								FileUtils.moveFile(f, new File(DIR_OUT+ File.separator + DIR_OUT_PPTX + File.separator + f.getName()));
							}

						} else if (e.getMessage()!=null
								&& e.getMessage().contains("SpreadsheetMLPackage")) {
							
							if (MOVE) {
								FileUtils.moveFile(f, new File(DIR_OUT+ File.separator + DIR_OUT_XLSX + File.separator + f.getName()));
							}

						} else if (e.getMessage()!=null
								&& e.getMessage().startsWith("Error processing zip file")) {
							
							if (MOVE) {
								FileUtils.moveFile(f, new File(DIR_OUT+ File.separator + DIR_OUT_PPTX + File.separator + f.getName()));
							}
							
						} else {
							// eg No relationship of type officeDocument
							
							e.printStackTrace();
							if (MOVE) {
								FileUtils.moveFile(f, new File(DIR_OUT+ File.separator + DIR_OUT_UNKNOWN + File.separator + f.getName()));
							}
						}
					} 
                	
                
            }
        }
    }		
    
	private void handle(File fIn, FileInputStream fis) throws Docx4JException, SmallFileException {

		
		
		if (docNum % 25 == 0)  System.out.println("\n\n " + docNum + " Processing " + fIn.getName() );

		try {
			DocxToVector dv = new DocxToVector(Docx4J.load(fis));
			dv.findStrings();
			
			docxFileData.add(
					new FileData(fIn, 
							dv.hashStrings()));
			
		} catch (ClassCastException e) {
			// eg dodgy docx: CustomXmlDataStoragePart cannot be cast to org.docx4j.openpackaging.parts.CustomXmlDataStoragePropertiesPart
			throw new Docx4JException(e.getMessage(), e);
		}
	}
	
	List<FileData> docxFileData = new ArrayList<FileData>();
	
	
	class FileData {
		
		FileData(File file, Set<Integer> vector) {
			this.file = file;
			this.vector = vector;
		}

		
		File file;		
		Set<Integer> vector;
		
		int duplicateOfFileNo = -1;
		boolean moved = false;
	}

	void pairs() {

		// Now we have List<FileData> docxFileData
		int count = docxFileData.size();
		int duplicateCount = 0;
		System.out.println("We have " + count);
		
		// Square array
		double[][] jaccards = new double[count][count];
		
		for( int i=0; i<count-1; i++) {
			
			if (i % 10 == 0) System.out.println(i);
			
			Set<Integer> s1 = docxFileData.get(i).vector;
			
			for( int j=i+1; j<=count-1; j++) {
				
				jaccards[i][j]=MinHash.jaccardIndex(s1, docxFileData.get(j).vector);
//				if (jaccards[i][j]>0.0) {
//					System.out.println(i + ", " + j + ": " + jaccards[i][j]);
//				}
				if (jaccards[i][j]==1.0) {
					duplicateCount++;
					if (docxFileData.get(j).duplicateOfFileNo>=0) {
						System.out.println(j + " is already noted as a duplicate of " + docxFileData.get(j).duplicateOfFileNo); 
					} else {
						docxFileData.get(j).duplicateOfFileNo=i;
						System.out.println(i + ", " + j + ": " + jaccards[i][j]);	
						
						if (MOVE  ) {
							String ext = FilenameUtils.getExtension(docxFileData.get(j).file.getName());
							try {
								FileUtils.moveFile(docxFileData.get(j).file, 
										new File(DIR_OUT+ File.separator + DIR_OUT_DUPLICATES + File.separator + i + "_dupe_" + j + "." + ext));
								docxFileData.get(j).moved=true;
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
					}
					
				}
			}
			
			if (MOVE && !docxFileData.get(i).moved /* might have moved earlier, if its a dupe */ ) {
				
				String ext = FilenameUtils.getExtension(docxFileData.get(i).file.getName());
				
				try {
					FileUtils.moveFile(docxFileData.get(i).file, 
							new File(DIR_OUT+ File.separator + DIR_OUT_MAIN + File.separator + i + "." + ext));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
		}
		
		System.out.println(duplicateCount + " duplicated detected");
		
	}
	
}
