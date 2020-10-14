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
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

public class OpenNlpSentenceSplitter extends JCasAnnotator_ImplBase {
    private static final Logger mLogger = LogManager.getLogger( PipelineTestHarness.class );
    private static final String PATH_TO_MODELS = "resources/openNlpModels/";
    private static final String SENTENCE_MODEL_FILE_NAME = "en-sent.bin";
    private SentenceDetectorME sentenceDetector;

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        super.initialize(ctx);
        try {
            SentenceModel smd = new SentenceModel(new File( PATH_TO_MODELS + SENTENCE_MODEL_FILE_NAME ) );
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

        int cnt = 0;
        for( Span sentSpan : sentSpans) {
            addSentence( jCas , cnt , sentSpan );
            cnt++;
        }
    }

    private void addSentence(JCas jCas, int cnt, Span sentSpan) {
        Sentence s = new Sentence( jCas , sentSpan.getStart() , sentSpan.getEnd() );
        s.setSentenceNumber( cnt );
        s.addToIndexes();
    }
}
