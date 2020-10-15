#!/usr/bin/python
# This scripts loads a pretrained model and a input file in CoNLL format (each line a token, sentences separated by an empty line).
# The input sentences are passed to the model for tagging. Prints the tokens and the tags in a CoNLL format to stdout
# Usage: python RunModel_ConLL_Format.py modelPath inputPathToConllFile
# For pretrained models see docs/
import warnings
warnings.filterwarnings( "ignore" )

from util.preprocessing import readCoNLL, createMatrices, addCharInformation, addCasingInformation
from neuralnets.BiLSTM import BiLSTM
import os
import sys
import logging

import re
import glob
from tqdm import tqdm

def clearTag( full_path , annot_count , tag_type , beginOffset , endOffset , coveredText ):
    with open( full_path , 'a' ) as fp:
        fp.write( 'T{}\t{} {} {}\t{}\n'.format( annot_count ,
                                                tag_type , 
                                                beginOffset ,
                                                endOffset ,
                                                coveredText ) )

if len(sys.argv) < 3:
    print("Usage: python RunModel_CoNLL_Format.py modelPath inputPathToConllFile")
    exit()

modelPath = sys.argv[1]
inputPath = sys.argv[2]
inputColumns = { 0: "tokens" ,
                 1: "beginOffset" ,
                 2: "endOffset" }

# :: Load the model ::
lstmModel = BiLSTM.loadModel(modelPath)

##########################
file_list = set([os.path.basename(x) for x in glob.glob( os.path.join( inputPath ,
                                                                       "*.conll" ) ) ] )
##for this_filename in tqdm( sorted( file_list ) ):
for this_filename in sorted( file_list ):
    output_filename = re.sub( ".conll$" ,
                              ".ann" ,
                              this_filename )
    output_fullpath = os.path.join( inputPath , output_filename )
    ##print( '{}'.format( output_fullpath ) )
    with open( output_fullpath , 'w' ) as fp:
        pass
    # :: Prepare the input ::
    sentences = readCoNLL( os.path.join( inputPath , this_filename ) ,
                           inputColumns )
    addCharInformation( sentences )
    addCasingInformation( sentences )
    ##
    dataMatrix = createMatrices( sentences , lstmModel.mappings , True )
    # :: Tag the input ::
    tags = lstmModel.tagSentences( dataMatrix )
    # :: Output to stdout ::
    annot_count = 0
    coveredText = []
    firstBegin = 0
    lastEnd = 0
    for sentenceIdx in range(len(sentences)):
        tokens = sentences[sentenceIdx]['tokens']
        ##
        for tokenIdx in range(len(tokens)):
            tokenTags = []
            for modelName in sorted( tags.keys() ):
                tokenTags.append( tags[modelName][sentenceIdx][tokenIdx] )
            if( len( tokenTags ) > 0 ):
                if( len( tokenTags ) > 1 ):
                    print( 'Multiple tags found:  {}'.format( ', '.join( tokenTags ) ) )
                firstTag = tokenTags[ 0 ]
                if( firstTag == 'O' ):
                    if( len( coveredText ) > 0 ):
                        annot_count += 1
                        clearTag( output_fullpath ,
                                  annot_count , tag_type ,
                                  firstBegin , lastEnd ,
                                  ' '.join( coveredText ) )
                        coveredText = []
                    ##
                else:
                    bio_tag , tag_type = firstTag.split( '-' )
                    if( bio_tag == 'B' ):
                        annot_count += 1
                        if( len( coveredText ) > 0 ):
                            clearTag( output_fullpath ,
                                      annot_count , tag_type ,
                                      firstBegin , lastEnd ,
                                      ' '.join( coveredText ) )
                            coveredText = []
                        ##
                        coveredText.append( tokens[ tokenIdx ] )
                        firstBegin = sentences[ sentenceIdx ][ 'beginOffset' ][ tokenIdx ]
                        lastEnd = sentences[ sentenceIdx ][ 'endOffset' ][ tokenIdx ]
                    elif( bio_tag == 'I' ):
                        coveredText.append( tokens[ tokenIdx ] )
                        lastEnd = sentences[ sentenceIdx ][ 'endOffset' ][ tokenIdx ]
                    else:
                        print( 'Unknown bio_tag found:  {}'.format( firstTag ) )
