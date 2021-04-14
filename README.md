
Installation and Packaging
==========================


Running Experiments on Downstream Tasks
=======================================

1. Compile the `PipelineTestHarness` Java program as described in the
previous section.

2. Make a copy of `pre-proc_tests.sh.TEMPLATE` as `pre-proc_tests.sh`.
(The version ending in `.TEMPLATE` may be changed and updated by
future code releases. The version ending in `.sh` is for your local
configurations.)

2. Read and localize the `pre-proc_tests.sh` shell script in the
`scripts` folders to include appropriate paths and environment
variables for your given architecture.


Preparing Notes for WebAnno/INCEpTION
=====================================

1. Compile the `PipelineTestHarness` Java program as described in the
previous section.

2. Make a copy of `webanno_note_prep.sh.TEMPLATE` as
`webanno_note_prep.sh`. (The version ending in `.TEMPLATE` may be
changed and updated by future code releases. The version ending in
`.sh` is for your local configurations.)

3. Read and localize the `webanno_note_prep.sh` shell script in the
`scripts` folders to include appropriate paths and environment
variables for your given architecture.

This script with split notes (plain text) into sentences exactly as
your pipeline would. The output CAS XMI files can be directly uploaded
to WebAnno and/or INCEpTION using `CAS XMI (v1.0)` and the sentence
boundaries will be appropriately maintained, matching your clinical
note expectations. 
