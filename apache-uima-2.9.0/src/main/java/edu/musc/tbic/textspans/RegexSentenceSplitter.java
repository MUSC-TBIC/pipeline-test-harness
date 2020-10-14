package edu.musc.tbic.textspans;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

import edu.musc.tbic.uima.PipelineTestHarness;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSentenceSplitter extends JCasAnnotator_ImplBase {
    private static final Logger mLogger = LogManager.getLogger( PipelineTestHarness.class );

    private static String mSentenceBoundaryRegex = null;
    
    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        super.initialize(ctx);
        
        mSentenceBoundaryRegex = "\\s*\\n+\\s*";
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        String text = jCas.getDocumentText();

        Pattern pa = Pattern.compile( mSentenceBoundaryRegex );       
        Matcher m = pa.matcher( text );
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
            addSentence( jCas , cnt , sentStart , sentEnd );
            sentStart = m.end();
            cnt++;
        }
    }

    private void addSentence(JCas jCas, int cnt, int sentStart , int sentEnd ) {
        Sentence s = new Sentence(jCas, sentStart , sentEnd );
        s.setSentenceNumber( cnt );
        s.addToIndexes();
    }
}
