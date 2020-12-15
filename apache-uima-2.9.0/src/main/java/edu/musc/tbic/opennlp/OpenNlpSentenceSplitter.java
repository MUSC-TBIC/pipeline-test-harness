package edu.musc.tbic.opennlp;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

import edu.musc.tbic.uima.PipelineTestHarness;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenNlpSentenceSplitter extends JCasAnnotator_ImplBase {
    private static final Logger mLogger = LogManager.getLogger( PipelineTestHarness.class );
    
    /**
     * Name of directory containing all models. The default value of the
     * empty string can be used if each model needs to have a different
     * base directory
     */
    public static final String PARAM_MODELPATH = "mModelPath";
    @ConfigurationParameter( name = PARAM_MODELPATH , 
                             description = "Path containing all models to load" , 
                             mandatory = false )
    private String mModelPath;
    /**
     * Name of model file used for sentence splitting
     */
    public static final String PARAM_SENTENCIZERMODEL = "mSentencizerModel";
    @ConfigurationParameter( name = PARAM_SENTENCIZERMODEL , 
                             description = "Sentencizer model filename to load" , 
                             mandatory = true )
    private String mSentencizerModel;

    public static final String PARAM_SENTENCIZERPATCH = "mSentencizerPatch";
    @ConfigurationParameter( name = PARAM_SENTENCIZERPATCH , 
                             description = "A patch function to run after the sentence splittermodel" , 
                             mandatory = false )
    private String mSentencizerPatch;

    private SentenceDetectorME sentenceDetector;
    
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize( context );
        //
        if( context.getConfigParameterValue( "mModelPath" ) == null ){
            mModelPath = "";
        } else {
            mModelPath = (String) context.getConfigParameterValue( "mModelPath" );
            // Add a final slash to the directory ending in case it wasn't provided
            if( ! mModelPath.substring( mModelPath.length() - 1 ).equals( "/" ) ){
                mModelPath = mModelPath + "/";
            }
        }
        //
        mSentencizerModel = (String) context.getConfigParameterValue( "mSentencizerModel" );
        //
        if( context.getConfigParameterValue( "mSentencizerPatch" ) == null ){
            mSentencizerPatch = "";
        } else {
            mSentencizerPatch = (String) context.getConfigParameterValue( "mSentencizerPatch" );
        }
        
        try {
            SentenceModel smd = new SentenceModel(new File( mModelPath + mSentencizerModel ) );
            sentenceDetector = new SentenceDetectorME(smd);
            mLogger.debug("OpenNLP sentence model loaded");
        } catch (IOException e) {
            mLogger.throwing(e);
            throw new ResourceInitializationException(e);
        }
        mLogger.debug("OpenNlpAnnotator Initialized");
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        mLogger.debug("OpenNlpAnnotator Begin");
        String text = jCas.getDocumentText();

        Span[] sentSpans = sentenceDetector.sentPosDetect( text );
        ArrayList<Span> sentSpanList = new ArrayList<>();
        if( mSentencizerPatch.equals( "multi-space" ) ){
            // split on long stretches of whitespace
            for( Span sentSpan : sentSpans ) {
                multispaceSentenceSplitter( text.substring( sentSpan.getStart() ,
                                                            sentSpan.getEnd() ) , 
                                            sentSpan , sentSpanList );
            }
        } else {
            // Default is to convert the array of spans to an
            // ArrayList of spans.  Not super interesting in
            // the NOOP instance but a useful conversion if you
            // need to support additional splitting and merging
            for( Span sentSpan : sentSpans ) {
                noopSentenceTemplate( sentSpan , sentSpanList );
            }            
        }
        
        addSentencesToJcas( jCas , sentSpanList );
        
    }

    private void addSentencesToJcas( JCas jCas , ArrayList<Span> sentSpanList ) {
        for( int i = 0; i < sentSpanList.size(); i++ ) {
            Span sentSpan = sentSpanList.get( i );
            Sentence s = new Sentence( jCas , sentSpan.getStart() , sentSpan.getEnd() );
            s.setSentenceNumber( i );
            s.addToIndexes();
        }
    }

    private void noopSentenceTemplate( Span sentSpan , ArrayList<Span> sentSpanList ) {
        sentSpanList.add( sentSpan );
    }

    private void multispaceSentenceSplitter( String original_span , Span sentSpan , ArrayList<Span> sentSpanList ) {
        Pattern pa = Pattern.compile( "\\s{3,}" );
        Matcher m = pa.matcher( original_span );
        int sentStart = 0;
        int sentEnd = 0;
        int cnt = 0;
        while( m.find( sentStart ) ) {
            sentEnd = m.start();
            // If we match on the first character,
            // then the first sentence doesn't start for a bit
            // so we can just skip this match.
            if( cnt == 0 && 
                sentEnd == 0 ){
                sentStart = m.end();
                continue;
            }
            if( sentStart == sentEnd ){
                break;
            }
            
            int b = sentSpan.getStart() + sentStart;
            int e = sentSpan.getStart() + sentEnd;
            
            Span sp = new Span( b , e );
            sentSpanList.add( sp );
            sentStart = m.end();
        }
        
        // If no multi-space strings were found, sentEnd will
        // still be zero, as initialized so this will match
        // the start of the original span.
        // Otherwise, sentEnd will be the end of the last matched
        // span that we found, which is to say, just after a 
        // series of multiple spaces.
        int b = sentSpan.getStart() + sentEnd;
        Span sp = new Span( b , sentSpan.getEnd() );
        sentSpanList.add( sp );
    }
}
