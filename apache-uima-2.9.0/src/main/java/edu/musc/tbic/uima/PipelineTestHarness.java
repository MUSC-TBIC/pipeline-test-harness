package edu.musc.tbic.uima;

import static org.apache.uima.fit.factory.ExternalResourceFactory.createDependencyAndBind;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.conceptMapper.ConceptMapper;
import org.apache.uima.conceptMapper.support.dictionaryResource.DictionaryResource_impl;
import org.apache.uima.conceptMapper.support.tokenizer.OffsetTokenizer;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.musc.tbic.opennlp.OpenNlpSentenceSplitter;
import edu.musc.tbic.opennlp.OpenNlpTokenizer;
import edu.musc.tbic.readers.FileSystemCollectionReader;
import edu.musc.tbic.textspans.RegexSentenceSplitter;
import edu.musc.tbic.writers.XmlWriter;

public class PipelineTestHarness extends JCasAnnotator_ImplBase {

    private static final Logger mLogger = LoggerFactory.getLogger( PipelineTestHarness.class );

    private static String pipeline_reader = null;
    private static String pipeline_tokenizer = null;
    private static String pipeline_sbd = null;
    private static ArrayList<String> pipeline_tests = new ArrayList<String>();
    private static ArrayList<String> pipeline_writers = new ArrayList<String>();
    
    public static void main(String[] args) throws ResourceInitializationException, UIMAException, IOException {

        pipeline_reader = "txt";
        pipeline_tokenizer = "whitespace";
        pipeline_sbd = "newline";
        pipeline_writers.add( "xmi" );
        
        // create Options object
        Options options = new Options();
        // add option
        options.addOption( "h" , "help" , false , "Display this help screen" );
        options.addOption( "v" , "version" , false , "Display PipelineTestHarness build version" );
        //
        options.addOption( "r" , "reader" , true , 
                "Pipeline reader (Default: " + pipeline_reader + ")" );
        options.addOption( "sbd" , "sentence-splitter" , true , 
                "Sentence splitting module (Default: " + pipeline_sbd + "; Options: newline, opennlp)" );
        options.addOption( "tok" , "tokenizer" , true , 
                "Tokenizer module (Default: " + pipeline_tokenizer + "; Options: opennlp, symbol, whitespace)" );
        options.addOption( "ts" , "test-symptoms" , false , 
                "Test symptom extraction using ConceptMapper" );
        options.addOption( "w" , "writers" , true , 
                "Pipeline writers (Default: " + String.join( ", " , pipeline_writers ) + ")" );
        // TODO
        //options.addOption( "c" , false , "Display PipelineTestHarness configuration settings" );
        // TODO
        options.addOption( "s" , "soft-load" , false , "Soft-load all resources in all modules and report progress" );
        //
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse( options , args );
            
            if( cmd.hasOption( "help" ) ){
                help( options );
            }   
            
            if( cmd.hasOption( "soft-load" ) ){
                System.out.println( "Configured to run resources for PipelineTestHarness\n" +
                        "\nThis option has not yet been implemented." );
                System.exit(0);
            }
            
            if( cmd.hasOption( "tokenizer" ) ){
                pipeline_tokenizer = cmd.getOptionValue( "tokenizer" );
            }

            if( cmd.hasOption( "sentence-splitter" ) ){
                pipeline_sbd = cmd.getOptionValue( "sentence-splitter" );
            }

            if( cmd.hasOption( "test-symptoms" ) ){
                pipeline_tests.add( "symptoms" );
            }

        } catch( ParseException exp ) {
            // oops, something went wrong
            mLogger.error( "Parsing failed.  Reason: " + exp.getMessage() );
        }

        mLogger.info( "Loading resources for PipelineTestHarness" );
        
        String module_breadcrumbs = "";
        CollectionReaderDescription collectionReader = null;
        ///////////////////////////////////////////////////
        if( pipeline_reader.equals( "txt" ) ){
            ////////////////////////////////////
            // Initialize plain text reader
            String sampleDir = "../data/input/txt";
            sampleDir = "../../ots-clinical-tokenizer/data/input";
            collectionReader = CollectionReaderFactory.createReaderDescription(
                    FileSystemCollectionReader.class ,
                    FileSystemCollectionReader.PARAM_INPUTDIR , sampleDir ,
                    FileSystemCollectionReader.PARAM_SUBDIR , true );
            module_breadcrumbs = "txt";
        }
        
        ///////////////////////////////////////////////////
        AggregateBuilder builder = new AggregateBuilder();

        ///////////////////////////////////////////////////
        // Sentence Splitters
        ///////////////////////////////////////////////////
        if( pipeline_sbd.equals( "opennlp" ) ){
            mLogger.info( "Loading OpenNLP's en-sent model" );
            module_breadcrumbs += "_opennlpSent";
            AnalysisEngineDescription openNlpSentence = AnalysisEngineFactory.createEngineDescription(
                    OpenNlpSentenceSplitter.class );
            builder.add( openNlpSentence );
        } else if( pipeline_sbd.equals( "newline" ) ) {
            module_breadcrumbs += "_newlineSent";
            mLogger.info( "Loading newline sentence splitter" );
            AnalysisEngineDescription newlineSentence = AnalysisEngineFactory.createEngineDescription(
                    RegexSentenceSplitter.class );
            builder.add( newlineSentence );
        } else {
            mLogger.error( "Unrecognized sentence splitter option: " + pipeline_sbd );
        }
        
        ///////////////////////////////////////////////////
        // Tokenizers
        ///////////////////////////////////////////////////
        String tokenizer_type = null;
        File tmpTokenizerDescription = null;
        if( pipeline_tokenizer.equals( "whitespace" ) ){
            mLogger.info( "Loading WhitespaceTokenizer" );
            tokenizer_type = "uima.tt.TokenAnnotation";
            module_breadcrumbs += "_whitespaceTok";
            AnalysisEngineDescription whitespaceTokenizer = AnalysisEngineFactory.createEngineDescription(
                    OffsetTokenizer.class , 
                    OffsetTokenizer.PARAM_CASE_MATCH , "ignoreall" ,
                    OffsetTokenizer.PARAM_TOKEN_DELIM , 
                    " " );
            tmpTokenizerDescription = File.createTempFile("prefix_", "_suffix");
            tmpTokenizerDescription.deleteOnExit();
            try {
                whitespaceTokenizer.toXML(new FileWriter(tmpTokenizerDescription));
            } catch (SAXException e) {
                // TODO - add something here
            }
            builder.add( whitespaceTokenizer );
        } else if( pipeline_tokenizer.equals( "symbol" ) ){
            mLogger.info( "Loading SymbolTokenizer" );
            tokenizer_type = "uima.tt.TokenAnnotation";
            module_breadcrumbs += "_symbolTok";
            AnalysisEngineDescription symbolTokenizer = AnalysisEngineFactory.createEngineDescription(
                    OffsetTokenizer.class , 
                    OffsetTokenizer.PARAM_CASE_MATCH , "ignoreall" ,
                    OffsetTokenizer.PARAM_TOKEN_DELIM , 
                    "/-*&@(){}|[]<>'`\":;,$%+.?! " );
            tmpTokenizerDescription = File.createTempFile("prefix_", "_suffix");
            tmpTokenizerDescription.deleteOnExit();
            try {
                symbolTokenizer.toXML(new FileWriter(tmpTokenizerDescription));
            } catch (SAXException e) {
                // TODO - add something here
            }
            builder.add( symbolTokenizer );
        } else if( pipeline_tokenizer.equals( "opennlp" ) ){
            mLogger.info( "Loading OpenNLP's en-token and en-pos-maxent models" );
            tokenizer_type = "org.apache.ctakes.typesystem.type.syntax.BaseToken";
            module_breadcrumbs += "_opennlpTok";
            AnalysisEngineDescription openNlpTokenizer = AnalysisEngineFactory.createEngineDescription(
                    OpenNlpTokenizer.class );
            tmpTokenizerDescription = File.createTempFile("prefix_", "_suffix");
            tmpTokenizerDescription.deleteOnExit();
            try {
                openNlpTokenizer.toXML(new FileWriter(tmpTokenizerDescription));
            } catch (SAXException e) {
                // TODO - add something here
            }
            builder.add( openNlpTokenizer );
        } else {
            mLogger.error( "Unrecognized tokenizer option: " + pipeline_tokenizer );
        }
        
        ///////////////////////////////////////////////////
        // Test modules
        if( pipeline_tests.contains( "symptoms" ) ){
            module_breadcrumbs += "_symptomsTest";
            String[] conceptFeatureList = new String[]{ "PreferredTerm" ,
                    "ConceptCode","ConceptType" ,
                    "BasicLevelConceptCode","BasicLevelConceptType" };
            String[] conceptAttributeList = new String[]{ "canonical" ,
                    "conceptCode" , "conceptType" ,
                    "basicLevelConceptCode" , "basicLevelConceptType" };
            mLogger.info( "Loading module ConceptMapper for symptoms" );
            AnalysisEngineDescription symptomConceptMapper = AnalysisEngineFactory.createEngineDescription(
                    ConceptMapper.class,
                    "TokenizerDescriptorPath", tmpTokenizerDescription.getAbsolutePath(),
                    "LanguageID", "en",
                    ConceptMapper.PARAM_TOKENANNOTATION, tokenizer_type ,
                    ConceptMapper.PARAM_ANNOTATION_NAME, "org.apache.uima.conceptMapper.UmlsTerm",
                    "SpanFeatureStructure", "uima.tcas.DocumentAnnotation",
                    ConceptMapper.PARAM_FEATURE_LIST, conceptFeatureList ,
                    ConceptMapper.PARAM_ATTRIBUTE_LIST, conceptAttributeList
                    );
            createDependencyAndBind( 
                    symptomConceptMapper , 
                    "DictionaryFile" , 
                    DictionaryResource_impl.class , 
                    "file:dict/conceptMapper_symptoms.xml" );
            builder.add( symptomConceptMapper );
        }

        ///////////////////////////////////////////////////
        // Initialize XMI writer
        if( pipeline_writers.contains( "xmi" ) ){
            // The default values for these output directories are
            // determined by whether it is a test run or a 
            // production run...
            String xml_output_dir = "../data/output/" + module_breadcrumbs + "_xmi";
            String xml_error_dir = "../data/output/" + module_breadcrumbs + "_error";
            // Then we use these values to construct our writer
            AnalysisEngineDescription xmlWriter = AnalysisEngineFactory.createEngineDescription(
                    XmlWriter.class , 
                    XmlWriter.PARAM_OUTPUTDIR , xml_output_dir ,
                    XmlWriter.PARAM_ERRORDIR , xml_error_dir );
            builder.add( xmlWriter );
        }
        
        SimplePipeline.runPipeline( collectionReader , builder.createAggregateDescription() );
    }
    
    private static void help( Options options ) {
        // This prints out some help
        HelpFormatter formater = new HelpFormatter();
        formater.printHelp( "PipelineTestHarness" , options );
            
        System.exit(0);
    }
    
    @Override
    public void process(JCas arg0) throws AnalysisEngineProcessException {
        // TODO Auto-generated method stub

    }

}