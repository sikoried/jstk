# Introduction #

In this example, we will build a connected digits speech recognizer based on phonetic units (monophones). Though the commands/API functions are described here, the training data (vmdigits) is not provided.

In general, the procedure for a simple speech recognizer is as follows:
**extract the features** get a first estimate of the acoustic codebook(s)
**initialize the phonetic network** train on forced alignments
**decode the test set**


# Feature Extraction #

For this example, we extract basic MFCC features.

```
jstk app.Mfcc --in-list lists/list.all ft/ --dir ssg8/ -f t:ssg/8 -w hamm,25,10 -b 0,4000,-1,.5 --turn-wise-mvn -d "5:1,5:2"
```

# Initialization of Acoustic Codebook #
Use the Initializer and GaussEM programs to produce a Mixture that will be used as shared codebook.

```
sikoried@lme126: ~jstk/vm$ jstk app.Initializer --gmm cb.init -n 256 -s sequential_5 --list lists/list.train --dir ft/
jstk app.GaussEM -i cb.init -o cb.em10 -p 3 -l lists/list.train  -d ft/ -n 10
```

# Initialization of the Phonetic Network #
First, we need a Token alphabet. This might be just words (in case of one-model-per-word), or as in this example, a monophone alphabet:
```
sikoried@lme126: ~/jstk/vm$ cat arch/vm-mono.a 
si	3
ah	2
ao	3
ay	3
eh	2
ey	3
f	2
ih	2
iy	3
k	2
n	2
ow	4
r	2
s	2
t	2
th	2
uw	3
v	2
w	3
y	3
z	3
```

The corresponding Tokenization is straight forward:

```
sikoried@lme126: ~/jstk/vm$ cat arch/vm-mono.l 
sil     si
one     w ah n
two     t uw
three   th r iy
four    f ao r
five    f ay v
six     s ih k s
seven   s eh v ah n
eight   ey t
nine    n ay n
zero    z ih r ow
yes     y eh s
no      n ow
oh      ow
```

Where 'sil' is the silence "word" modeled by the "sound" 'si'. Now, let's produce a Configuration:

```
sikoried@lme126: ~jstk/vm$ jstk arch.Configuration --compile arch/vm-mono.a arch/vm-mono.l --semi cb.em10 --write conf-mono.xml conf-mono.cb.0
```

# Train the Token Hierarchy #
Use a few manual alignments for a first training of the models to obtain robust forced alignments later on. Use the Trainer for this. Then iterate on the whole training set. Note that the first Training pass should be done with Viterbi do allow the densities to settle for specific models.

```
sikoried@lme126: ~/jstk/vm$ jstk app.Trainer conf-mono.xml conf-mono.cb.0 conf-mono.cb.1 lists/list.train.malign.trl ft/ -a manual_linear malign/ -t vt -p 2

sikoried@lme126: ~/jstk/vm$ for i in `seq 2 6`; do
> j=`expr $i - 1`
? jstk app.Trainer conf-mono.xml conf-mono.cb.$j conf-mono.cb.$i lists/list.train.trl ft/ -a forced -t bw -p 3
> done
```

# Test usint the IWRecognizer or Decoder #
You can test the models either using the IWRecognizer which tries to do forced alignments on the files, or the Decoder which does actual speech decoding and allows the recognition of sequences.

```
sikoried@lme126: ~/jstk/vm$ jstk app.IWRecognizer conf-mono.xml conf-mono.cb.$i sil -l lists/list.test ft out.$i -p 3 -n 1
sikoried@lme126: ~/jstk/vm$ jstk app.Decoder conf-mono.xml conf-mono.cb.6 -l lists/list.test ft -bs 1000 -bw 15 -i 0.01 -w 0.01 -q -o outd.6
```

# Example Configuration and Model Files #
You can find the trained configuration and models at http://code.google.com/p/jstk/downloads/detail?name=vm-models.zip