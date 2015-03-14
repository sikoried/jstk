# What is the JSTK (and what not)
The Jave speech toolkit (JSTK) provides a native implementation of both library/API and applications for speech recognition, speaker verification, speech visualization (including transcription tools), and evaluation of related human rater tasks.
JSTK is *not* a full-blown LVCSR toolkit with pre-trained models, sorry.

# Where is JSTK used?

- Digital sports: http://www5.cs.fau.de/our-team/jensen-ulf/projects/drop-jump-phase-length-calculation-with-hidden-markov-models/
- Automatic Intelligibility Assessment of Speakers After Laryngeal Cancer by Means of Acoustic Modeling
T Bocklet, K Riedhammer, E Nöth, U Eysholdt, T Haderlein. Journal of Voice 26 (3), 390-397, 2012
- Erlangen-CLP: A Large Annotated Corpus of Speech from Children with Cleft Lip and Palate
T Bocklet, A Maier, K Riedhammer, U Eysholdt, E Nöth. Language Resources and Evaluation (LREC) 2014
- LMELECTURES: A Multimedia Corpus of Academic Spoken English. K Riedhammer, M Gropp, T Bocklet, F Hönig, E Nöth, S Steidl, SLAM@ INTERSPEECH, 2013.
- Interactive Approaches to Video Lecture Assessment. K Riedhammer. Logos Verlag Berlin GmbH, 2012.
- A Software Kit for Automatic Voice Descrambling.  K Riedhammer, M Ring, E Nöth, D Kolb.  IEEE Workshop on Security and Forensics in Communication Systems, 2012.
- Compensation of extrinsic variability in speaker verification systems on simulated Skype and HF channel data
K Riedhammer, T Bocklet, E Nöth.  Acoustics, Speech and Signal Processing (ICASSP), 2011.
- Drink and Speak: On the Automatic Classification of Alcohol Intoxication by Acoustic, Prosodic and Text-Based Features. T Bocklet, K Riedhammer, E Nöth. INTERSPEECH, 2011.
- Java Visual Speech Components for Rapid Application Development of GUI Based Speech Processing Applications.
S Steidl, K Riedhammer, T Bocklet, F Hönig, E Nöth.  INTERSPEECH, 2011.
- Demo system for age recognition: http://www.nacht-der-wissenschaften.de/2009/ansicht_sammel.php?id=234#234


# Building JSTK
You can import JSTK as a project to Eclipse, or use the provided gradle file to build the complete jar file (and dependencies) in `build/libs/{jstk-latest,libs/*}.jar`

# Using JSTK
The easiest way to use the command line tools is to make a script like the following:
```
$ cat jstk
#!/bin/bash

CLASSPATH=/home/sikoried/Work/workspace/jstk/bin:/home/sikoried/Work/lib/jtransforms-2.3.jar:/home/sikoried/Work/lib/Jama-1.0.2.jar:/home/sikoried/Work/lib/FJama.jar:/home/sikoried/Work/lib/log4j-1.2.16.jar

java -Xmx7G de.fau.cs.jstk.$@
```

Can be used then as

```
$ ./jstk app.Decoder
```

# Maintenance and Support

The Java Speech Tooklit (JSTK) is developed and maintained by the Speech Group
at the University of Erlangen-Nuremberg. It is designed to provide both API and
applications for the most popular speech tasks such as speech recognition,
speaker verification, speech transcription and annotation and evaluation of
human rater tasks. The current maintainers of the toolkit can be reached at

jstk@speech.informatik.uni-erlangen.de

The JSTK is licensed under GPLv3 and welcomes any contributions in terms of
extensions, bugfixes, feature requests or comments. Note that code maintenance
is (for now) only done at the speech group of Univ. Erlangen. Though this
toolkit is intended to allow full-scale speech applications, special APIs such
as the Google Android Speech API or Java Speech API are not implemented (but may
however be implemented in the future).


# Dependencies (managed through gradle)

JTransforms - http://sites.google.com/site/piotrwendykier/software/jtransforms
> JTransforms provides (discrete) signal processing routines such as FFT, DHT,
> and DCT. It relies on state-of-the-art algorithms and even allows multi-
> threading.

JAMA - http://math.nist.gov/javanumerics/jama/
> The JAMA package provides linear algebra routines such as decompositions
> (e.g. Eigen, LR, QR) and matrix operations (calculus, inversions).
> A single precision copy is maintained at the Speech Group named FJAMA, and
> a snapshot can be found in the download section of this project.

log4j - http://logging.apache.org/log4j
> log4j provides scalable logging capabilities, however, JSTK only uses the
> very basic features of it -- for now. Currently, we use v 1.2.16

jspeex - http://sourceforge.net/projects/jspeex/files/jspeex
> jspeex is used for decoding speex-compressed (`*`.spx) files. speex is a lossy
> codec comparable to MP3 or Ogg Vorbis, but optimized for speech, see http://www.speex.org.
> Currently using version 0.9.7.

junit - http://www.junit.org
> Testing framework for java. currently used only sporadically.
> Currently using version 4.