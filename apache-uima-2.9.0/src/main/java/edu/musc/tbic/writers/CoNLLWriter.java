package edu.musc.tbic.writers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class CoNLLWriter extends JCasAnnotator_ImplBase {

	private static final Logger mLogger = LoggerFactory.getLogger( CoNLLWriter.class );
	
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
        String outTxtFileName = note_id + ".txt";
        File outTxtFile = new File( mOutputDir, outTxtFileName );
        String outCoNLLFileName = note_id + ".conll";
        File outCoNLLFile = new File( mOutputDir, outCoNLLFileName );
        // TODO - streamline by only creating this path when needed
		File errFile = new File( mErrorDir, outCoNLLFileName );
		
		// walk jCAS and write to output file
		try {
		    writeCoNLL( aJCas.getCas() , outTxtFile , outCoNLLFile, errFile );
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
	 * 
	 *
	 * @param aCas CAS to serialize
     * @param txtName plain text output file
     * @param conllName CoNLL-formatted output file
     * @param error_name error output file
	 * @throws SAXException
	 * @throws Exception
	 * @throws ResourceProcessException
	 */
	private void writeCoNLL( CAS aCas, File txtName , File conllName , File error_name ) throws IOException, SAXException {
	    FileWriter txtWriter = new FileWriter( txtName );
	    FileWriter conllWriter = new FileWriter( conllName );

	    String text = aCas.getDocumentText();
	    AnnotationFS token;
		try {
            // write .txt file
		    txtWriter.write( text );
            // write .conll file
		    TypeSystem typeSystem = aCas.getTypeSystem();
		    org.apache.uima.cas.Type tokenType = typeSystem.getType( "org.apache.ctakes.typesystem.type.syntax.BaseToken" );
            org.apache.uima.cas.Type sentenceType = typeSystem.getType( "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence" );

            FSIndex dbIndex = aCas.getAnnotationIndex( sentenceType );
            FSIterator spanIterator = dbIndex.iterator();

            AnnotationIndex tokenIndex = (AnnotationIndex) aCas.getAnnotationIndex( tokenType );

            while( spanIterator.hasNext() ) {
              ArrayList<AnnotationFS> tokens = new ArrayList<AnnotationFS>(2048);

              Annotation spanAnnotation = (Annotation) spanIterator.next();

              FSIterator tokenIter = tokenIndex.subiterator(spanAnnotation);

              // get all tokens for the specified block
              while (tokenIter.hasNext()) {
                token = (AnnotationFS) tokenIter.next();
                // System.err.print ("--> token: '" + token.getCoveredText()
                conllWriter.append( token.getCoveredText() + "\t" +
                               token.getBegin() + "\t" +
                               token.getEnd() + "\n" );
              }
              conllWriter.append( "\n" );
            }
		} finally {
		    if( txtWriter != null ) {
		        txtWriter.flush();
		        txtWriter.close();
            }
		    if( conllWriter != null ) {
                conllWriter.flush();
                conllWriter.close();
            }
		}
	}

	public void destroy() {
		// TODO - why isn't this run?
		mLogger.info( "Total Notes to Write = " + String.valueOf( mTotalDocs ) + 
					  " , Successful Notes = " + String.valueOf( mGoodDocs ) + 
					  " , Broken Notes = " + String.valueOf( mBadDocs ) );
	}
    
}
