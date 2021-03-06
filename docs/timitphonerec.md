# Introduction #
This example shows how to train and evaluate a phoneme recognizer with JSTK. To follow this example, you need the TIMIT Corpus (1) (ideally in CD distribution form), SRILM (2) and NIST sclite (3).


(1) http://www.ldc.upenn.edu/Catalog/CatalogEntry.jsp?catalogId=LDC93S1
(2) http://www.speech.sri.com/projects/srilm/
(3) http://www.itl.nist.gov/iad/mig/tools/

# Alphabet and Lexicon
The used alphabet and lexicon refers to the reduced phoneme set (39 distinct phones), as with most systems. The lexicon is basically the same as the alphabet, as the phones are already the words.
```
$ cat timit.a
aa      3
ae      3
ah      3
aw      3
ay      3
b       2
ch      3
d       2
dh      3
dx      2
eh      3
er      3
ey      3
f       2
g       2
hh      2
ih      3
iy      3
jh      2
k       2
l       3
m       3
n       3
ng      2
ow      3
oy      3
p       2
r       2
s       2
sh      3
sil     3
t       2
th      3
uh      3
uw      3
v       3
w       3
y       3
z       3
```

```
$ cat timit.l
aa aa
ae ae
ah ah
aw aw
ay ay
b b
ch ch
d d
dh dh
dx dx
eh eh
er er
ey ey
f f
g g
hh hh
ih ih
iy iy
jh jh
k k
l l
m m
n n
ng ng
ow ow
oy oy
p p
r r
s s
sh sh
sil sil
t t
th th
uh uh
uw uw
v v
w w
y y
z z
```

# Training Script
```
#/bin/bash

exit 1

# compile the timit data
for j in train test; do

        # build train/test list
        fgrep $j list.orig.all > list.orig.$j

        # remove old lists
        rm -f list.$j.trl list.$j.spk

        for i in `cat list.orig.$j`; do
                spk1=`dirname $i`
            spk=`basename $spk1`
                fn=`basename $i`

                echo $spk >> $$.$j.spk

                # audio file
                #cp $i.ssg16 ssg16/${spk}-${fn}

                # alignment 
                cat $i.phn | prep/mapphn.pl | prep/timit39alg.pl > malign/${spk}-${fn}

                # transcription
                echo -n "${spk}-${fn} " >> list.$j.trl
                cat malign/${spk}-${fn} | prep/totrl.pl >> list.$j.trl
        done

        # finish speaker list
        sort -u $$.$j.spk > list.$j.spk
        rm $$.$j.spk
done

cat list.{train,test}.spk > list.spk

# features, normalize per speaker
for i in `cat list.spk`; do
        fgrep $i list.all > $$.ftlist
        jstk app.Mfcc --in-list $$.ftlist ft/ --dir ssg16/ -f t:ssg/16 -w hamm,25,10 -b 188,6071,-1,.5 -d "5:1,5:2" --generate-mvn-file $$.mvn
        jstk app.Mfcc --in-list $$.ftlist ft/ --dir ssg16/ -f t:ssg/16 -w hamm,25,10 -b 188,6071,-1,.5 -d "5:1,5:2" -m $$.mvn --novar
        rm $$.ftlist $$.mvn
done

# estimate bi-gram model using srilm
cut -d' ' -f 2- list.train.trl > train.txt
ngram-count -order 2 -text train.txt -lm bigram.lm -no-sos -no-eos 

# alphabet and lexicon
mkdir -p conf/
cut -d' ' -f1 malign/* | sed -e "s/\///g" | sort -u > conf/timit.a  #*/
# now manally add number of states!
awk '{print $1, $1}' conf/timit.a > conf/timit.l


# for multithreading
numgauss=512
numem=5
numvt=30
nump=8

# init codebook
jstk app.Initializer --gmm cb.init -f -n $numgauss -s sequential_50 --list list.train.small --dir ft/
jstk app.GaussEM -i cb.init -o cb.em -l list.train -d ft/ -n $numem -p $nump

# compile configuration
jstk arch.Configuration --compile conf/timit.a conf/timit.l --semi cb.em --write conf.xml conf.cb.0

# first pass (linear alignment)
jstk app.Trainer conf.xml conf.cb.0 conf.cb.1 list.train.trl ft/ -a manual_linear malign/ -t vt -p $nump

# do initial forced aligment within word boundaries
for i in `seq 2 $numvt`; do
        echo iteration $i
    jstk app.Trainer conf.xml conf.cb.$[$i-1] conf.cb.$i list.train.trl ft/ -a manual malign/ -t vt -p $nump > logs/trainer.out.$i

        #mkdir -p align-$i
        # jstk app.Aligner conf.xml conf.cb.$i -l list.test.trl ft/ align-$i/ -p $nump -b "sil" > logs/aligner-test.$i.log
        #jstk app.Aligner conf.xml conf.cb.$i -l list.train.trl ft/ align-$i/ -p $nump -b "sil" > logs/aligner-train.$i.log
        #fgrep score logs/aligner-test.$i.log | cut -d' ' -f 8- | sort | ./eval.pl > logs/align-test.$i
        #fgrep score logs/aligner-train.$i.log | cut -d' ' -f 8- | sort | ./eval.pl > logs/align-train.$i
done

# generate turn list for sclite
cut -d' ' -f1 list.train.trl | sed -e "s/-/_/g" -e "s/^/(/" -e "s/$/)/" > list.train.sclite
cut -d' ' -f1 list.test.trl | sed -e "s/-/_/g" -e "s/^/(/" -e "s/$/)/" > list.test.sclite

cut -d' ' -f2- list.train.trl | paste -d' ' - list.train.sclite | sed -e "s/^ //" > train.ref
cut -d' ' -f2- list.test.trl | paste -d' ' - list.test.sclite | sed -e "s/^ //" > test.ref

sed -e "s/sil//g" -e "s/ \+/ /g" -e "s/^ //" train.ref > train-nosil.ref
sed -e "s/sil//g" -e "s/ \+/ /g" -e "s/^ //" test.ref > test-nosil.ref

# decode single utterance to adjust params
jstk app.Decoder conf.xml conf.cb.20 bigram.lm -f ft/faks0-sa1 -bs 300 -n 1 -w 1 -i 0.05 -m ma -o faks0-sa1.alg

# decode test set
jstk app.Decoder conf.xml conf.cb.30 bigram.lm -l list.test ft/ -bs 300 -n 1 -w 1 -i 1 -q -o rec

# convert to sclite
paste -d' ' rec list.test.sclite | sed -e "s/^ //" > rec.hyp
sed -e "s/sil//g" -e "s/ \+/ /g" -e "s/^ //" rec.hyp > rec-nosil.hyp

# score
sclite -r test.ref -h rec.hyp -i rm
sclite -r test-nosil.ref -h rec-nosil.hyp -i rm
```

# Results #
Using a bi-gram LM estimated on the training data, the sclite shows an average WER (in this case: PER) of 38.2%
```
| SPKR   | # Snt # Wrd | Corr    Sub    Del    Ins    Err  S.Err |
| Sum/Avg| 1680  62901 | 66.2   22.3   11.5    4.4   38.2  100.0 |
|================================================================|
|  Mean  | 10.0  374.4 | 66.2   22.3   11.5    4.4   38.2  100.0 |
|  S.D.  |  0.0   16.3 |  4.4    3.4    2.6    2.1    4.4    0.0 |
| Median | 10.0  375.0 | 66.2   21.9   11.3    4.0   37.9  100.0 |
`----------------------------------------------------------------'
```

Word insertion penalty, beam size and LM weight may not be chosen optimal, other codebook settings may produce better results, esp. continuous density HMM.