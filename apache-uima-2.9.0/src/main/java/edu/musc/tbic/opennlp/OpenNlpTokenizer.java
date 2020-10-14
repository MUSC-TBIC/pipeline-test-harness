package edu.musc.tbic.opennlp;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

import edu.musc.tbic.uima.PipelineTestHarness;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Span;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class OpenNlpTokenizer extends JCasAnnotator_ImplBase {
    private static final Logger mLogger = LogManager.getLogger( PipelineTestHarness.class );
    private static final String PATH_TO_MODELS = "resources/openNlpModels/";
    private static final String TOKENIZER_MODEL_FILE_NAME = "en-token.bin";
    private static final String POS_MODEL_FILE_NAME = "en-pos-maxent.bin";
    private TokenizerME tokenizer;
    private POSTaggerME posTagger;

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        super.initialize(ctx);
        try {
            TokenizerModel tmd = new TokenizerModel(new File( PATH_TO_MODELS + TOKENIZER_MODEL_FILE_NAME ) );
            tokenizer = new TokenizerME(tmd);
            mLogger.debug("OpenNLP tokenizer model loaded");
            POSModel pmd = new POSModel(new File( PATH_TO_MODELS + POS_MODEL_FILE_NAME ) );
            posTagger = new POSTaggerME(pmd);
            mLogger.debug("OpenNLP pos model loaded");
        } catch (IOException e) {
            mLogger.throwing(e);
            throw new ResourceInitializationException(e);
        }
        mLogger.debug("OpenNlpAnnotator Initialized");
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        mLogger.debug("OpenNlpAnnotator Begin");
        
        int tokenNumber = 0;
        FSIterator<?> it = jCas.getAnnotationIndex( Sentence.type ).iterator();
        if( it.hasNext() ){
            while( it.hasNext() ){
                Sentence sentAnnot = (Sentence) it.next();
                String sentence = sentAnnot.getCoveredText();
                tokenNumber += splitIntoTokens( jCas , sentence , 
                        sentAnnot.getBegin() , tokenNumber );
            }
        } else {
            splitIntoTokens( jCas , jCas.getDocumentText() , 0 , 0 );
        }
    }
        
    private int splitIntoTokens( JCas jCas , String spanToSplit , 
                                 int sentStart , int tokenNumber ){
        Span[] tokSpans = tokenizer.tokenizePos( spanToSplit );

        //aggressive tokenization
        ArrayList<Span> nSs = new ArrayList<>();
        for( Span tokSpan : tokSpans) {
            splitTokens(tokSpan, nSs, spanToSplit );
        }

        String[] tokens = new String[nSs.size()];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = nSs.get(i).getCoveredText( spanToSplit ).toString();
        }

        String[] tags = posTagger.tag( tokens );
        addBaseTokens(jCas, tags, nSs, sentStart , tokenNumber );
        
        return tokens.length;
    }

    private void addBaseTokens(JCas jCas, String[] tags, ArrayList<Span> nSs, int b, int tokenNumber ) {
        for (int i = 0; i < nSs.size(); i++) {
            Span tok = nSs.get(i);
            BaseToken t = new BaseToken(jCas, tok.getStart() + b, tok.getEnd() + b);
            t.setTokenNumber( tokenNumber + i );
            t.setPartOfSpeech(tags[i]);
            t.addToIndexes();
        }
    }

    private void splitTokens(Span tok, ArrayList<Span> list, String sentence) {

        String str = tok.getCoveredText(sentence).toString();
        char pCh = str.charAt(0);
        int s = 0;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            boolean ifC = false;

            if (Character.isDigit(ch) && i > 0) {
                if (!Character.isDigit(pCh)) {
                    ifC = true;
                }
            } else if (Character.isLowerCase(ch) && i > 0) { // 0 - A // a  //ALMartial -> AL Martial
                if (!(Character.isLowerCase(pCh) || Character.isUpperCase(pCh))) {
                    ifC = true;
                }
            } else if (Character.isUpperCase(ch) && i > 0) { // 0 a - // A  A
                if (!Character.isUpperCase(pCh) || (i < str.length() - 1 && Character.isLowerCase(str.charAt(i + 1)))) {
                    ifC = true;
                }
            } else if (i > 0) {
                if (Character.isDigit(pCh) || Character.isLowerCase(pCh) || Character.isUpperCase(pCh) || Character.isWhitespace(pCh)) {
                    ifC = true;
                }
            }

            if (ifC && !Character.isWhitespace(ch)) {
                //split s i
                int b = s + tok.getStart();
                int e = i + tok.getStart();
                if (Character.isWhitespace(pCh)) {
                    e = i + tok.getStart() - 1;
                }
                Span sp = new Span(b, e);
                list.add(sp);
                s = i;
            }
            pCh = ch;
        }

        int b = s + tok.getStart();
        int e = tok.getStart() + str.length();
        Span sp = new Span(b, e);
        list.add(sp);
    }
}
