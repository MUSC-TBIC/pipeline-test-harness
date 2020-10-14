package edu.musc.tbic.writers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import edu.musc.tbic.uima.PipelineTestHarness;

public class XmlWriter extends JCasAnnotator_ImplBase {

	private static final Logger mLogger = LoggerFactory.getLogger( PipelineTestHarness.class );
	
	/**
	 * Name of configuration parameter that must be set to the path of a directory into which the
	 * output files will be written.
	 */
	public static final String PARAM_OUTPUTDIR = "OutputDirectory";
	@ConfigurationParameter(name = PARAM_OUTPUTDIR, description = "Output directory to write xmi files", mandatory = true)
	private String mOutputDir;
	/**
	 * Name of configuration parameter that must be set to the path of a directory into which the
	 * output files containing some sort of unrecoverable error will be written. These files are
	 * to be considered outside the pipeline due to some deep parsing, processing, or writing
	 * failure and can be manually reviewed at a later date.
	 */
	public static final String PARAM_ERRORDIR = "ErrorDirectory";
	@ConfigurationParameter(name = PARAM_ERRORDIR, description = "Output directory to write broken files", mandatory = true)
	private String mErrorDir;

	private int mTotalDocs;
	private int mGoodDocs;
	private int mBadDocs;
    
    public void initialize( UimaContext context ) throws ResourceInitializationException {
    	mTotalDocs = 0;
		mGoodDocs = 0;
		mBadDocs = 0;
		
		mOutputDir = (String) context.getConfigParameterValue( "OutputDirectory" );
		File outputDirectory = new File( mOutputDir );
		if ( !outputDirectory.exists() ) {
			outputDirectory.mkdirs();
		}
		mErrorDir = (String) context.getConfigParameterValue( "ErrorDirectory" );
		
    }

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
    	mTotalDocs++;
    	String modelFileName = null;
    	
		String note_id = "";
		FSIterator<?> it = aJCas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
		if( it.hasNext() ){
		    SourceDocumentInformation fileLoc = (SourceDocumentInformation) it.next();
		    note_id = fileLoc.getUri().toString();
		}
		if( note_id.endsWith( ".txt" ) ){
		    note_id = note_id.substring( 0 , note_id.length() - 4 );
        }
        mLogger.debug( "Writing note_id '" + note_id + "' to disk" );
    	String outFileName = note_id + ".xmi";
		File outFile = new File( mOutputDir, outFileName );
		// TODO - streamline by only creating this path when needed
		File errFile = new File( mErrorDir, outFileName );
		
		// serialize XCAS and write to output file
		try {
			writeXmi( aJCas.getCas() , outFile, errFile , modelFileName );
			mGoodDocs++;
		} catch ( IOException e ) {
			mBadDocs++;
			throw new AnalysisEngineProcessException( e );
		} catch ( SAXParseException e ) {
			mBadDocs++;
            throw new AnalysisEngineProcessException( e );
        } catch ( SAXException e ) {
			mBadDocs++;
            throw new AnalysisEngineProcessException( e );
        }
    }

    /**
	 * Serialize a CAS to a file in XMI format
	 *
	 * @param aCas CAS to serialize
	 * @param name output file
	 * @throws SAXException
	 * @throws Exception
	 * @throws ResourceProcessException
	 */
	private void writeXmi( CAS aCas, File name ) throws IOException, SAXException {
		FileOutputStream out = null;

		try {
			// write XMI
			out = new FileOutputStream( name );
			XmiCasSerializer ser = new XmiCasSerializer( aCas.getTypeSystem(),null, true );
			XMLSerializer xmlSer = new XMLSerializer( out, true );
			ser.serialize( aCas, xmlSer.getContentHandler() );
		} finally {
			if ( out != null ) {
				out.close();
			}
		}
	}
	
	/**
	 * Serialize a CAS to a file in XMI format
	 *
	 * @param aCas CAS to serialize
	 * @param name output file
	 * @throws SAXException
	 * @throws AnalysisEngineProcessException 
	 * @throws Exception
	 * @throws ResourceProcessException
	 */
	private void writeXmi( CAS aCas, File name, File error_name, String modelFileName ) throws IOException, SAXException, AnalysisEngineProcessException {
		FileOutputStream out = null;
		BufferedWriter error_out = null;

		try {
			// write XMI
			out = new FileOutputStream( name );
			XmiCasSerializer ser = new XmiCasSerializer( aCas.getTypeSystem() );
			XMLSerializer xmlSer = new XMLSerializer( out, true );
			ser.serialize( aCas, xmlSer.getContentHandler() );
		} catch ( SAXParseException e ) {
			mBadDocs++;
			// TODO - this can be made much more efficient rather than repeating
			//        the extraction twice in case of error
			JCas jcas;
			try {
				jcas = aCas.getJCas();
			} catch ( CASException e1 ) {
				e1.printStackTrace();
				throw new AnalysisEngineProcessException( e1 );
			}
			String note_id = "";
	        FSIterator<?> it = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
	        if( it.hasNext() ){
	            SourceDocumentInformation fileLoc = (SourceDocumentInformation) it.next();
	            note_id = fileLoc.getUri().toString();
	        }
	        if( note_id.endsWith( ".txt" ) ){
	            note_id = note_id.substring( 0 , note_id.length() - 4 );
	        }
			mLogger.error( "SAXParseError when trying to write CAS for note_id '" + note_id + "' to file. " +
							"Check fs.error_directory for details." );
			// We only need to create the error file directory *if* we run 
			// across a bum file
			File errorDirectory = new File( mErrorDir );
			if ( !errorDirectory.exists() ) {
				errorDirectory.mkdirs();
			}
			error_out = new BufferedWriter( new FileWriter( error_name ) );
//			error_out.write( "Note Source Value:  " + note_source_value + "\n\n" );
			error_out.append( "Caught SAXParseException:  " + e + "\n\n" );
        } finally {
			if ( out != null ) {
				out.close();
			}
			if ( error_out != null ) {
				error_out.close();
			}
		}
	}

	public void close() throws IOException {
		// TODO - why isn't this run?
		mLogger.info( "Total Notes to Write = " + String.valueOf( mTotalDocs ) + 
					  " , Successful Notes = " + String.valueOf( mGoodDocs ) + 
					  " , Broken Notes = " + String.valueOf( mBadDocs ) );
	}
    
}
