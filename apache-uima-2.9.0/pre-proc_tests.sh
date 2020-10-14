#!/bin/bash

## Variables you will need to update based on the build
export VERSION=20.42.0
## Variables you will need to customize to your set-up
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home
export UIMA_HOME=/Users/pmh/bin/apache-uima-2.9.0
export CONCEPTMAPPER_HOME="/Users/pmh/bin/ConceptMapper-2.10.2"
export OPENNLP_HOME="/Users/pmh/bin/apache-opennlp-1.8.0"
export PIPELINE_ROOT=/Users/pmh/git/pipeline-test-harness/apache-uima-2.9.0
export ETUDE_DIR=/Users/pmh/git/etude
export CONFIG_DIR=/Users/pmh/git/etude-engine-configs
## TODO - we can't release this data so we need to make a releasable demo version
export REF_ROOT=/Users/pmh/git/COVID-19_NLP_Screening/data/triage_samples/webanno_eval_set

## Variables that
export PATH=$PATH:$UIMA_HOME/bin

export UIMA_CLASSPATH=${PIPELINE_ROOT}/target/classes
export UIMA_CLASSPATH=$UIMA_CLASSPATH:${PIPELINE_ROOT}/lib
export UIMA_CLASSPATH=${UIMA_CLASSPATH}:${CONCEPTMAPPER_HOME}/lib
export UIMA_CLASSPATH=${UIMA_CLASSPATH}:${CONCEPTMAPPER_HOME}/src
export UIMA_CLASSPATH=${UIMA_CLASSPATH}:${OPENNLP_HOME}/lib
export UIMA_CLASSPATH=${UIMA_CLASSPATH}:${PIPELINE_ROOT}/resources

export UIMA_DATAPATH=${PIPELINE_ROOT}/resources

export UIMA_JVM_OPTS="-Xms128M -Xmx2G"

export SYS_ROOT=../

for SBD in newline opennlp;do \
    for TOKENIZER in opennlp symbol whitespace;do \
        java -cp \
             resources:target/pipeline-test-harness-${VERSION}-SNAPSHOT-jar-with-dependencies.jar:${CONCEPTMAPPER_HOME}/lib:${CONCEPTMAPPER_HOME}/bin \
             edu.musc.tbic.uima.PipelineTestHarness \
             --sentence-splitter ${SBD} \
             --tokenizer ${TOKENIZER} \
             --test-symptoms; \
        ##
        export SYS_DIR=${SYS_ROOT}/data/output/txt_${SBD}Sent_${TOKENIZER}Tok_xmi
        python3 ${ETUDE_DIR}/etude.py \
                --progressbar-output none \
                --reference-conf ${CONFIG_DIR}/uima/covid-nlp_xmi.conf \
                --reference-input ${REF_ROOT}/v8b_curated \
                --test-conf ${CONFIG_DIR}/uima/covid-nlp_pipeline_xmi.conf \
                --test-input ${SYS_DIR} \
                --file-suffix ".xmi" \
                --score-value "Symptom" \
                --fuzzy-match-flags exact partial \
                -m TP FP FN Precision Recall F1; \
            done; \
            done
