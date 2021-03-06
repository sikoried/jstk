[Ulf Jensen: Jump Phase Length Calculation](http://www5.cs.fau.de/en/our-team/jensen-ulf/projects/drop-jump-phase-length-calculation-with-hidden-markov-models/)
[G Drive folder with data files](https://drive.google.com/folderview?id=0B7jkABFvRH72flBpZEV0SWRxaWtLUmZjdExEdzVmOU1PeTBnZ0RyZk1aRjVZZVprMHRfcVU&usp=sharing)

# Environment Setup #
```
#!/bin/bash

#TODO: set correct paths
export JSTK_ROOT=<Path to jstk>/jstk
export JSTK_LIB_ROOT=<Path to libs>/libs

export CLASSPATH=$JSTK_ROOT/bin:$JSTK_LIB_ROOT/Jama-1.0.3.jar:$JSTK_LIB_ROOT/FJama.jar:$JSTK_LIB_ROOT/junit-4.11.jar:$JSTK_LIB_ROOT/jtransforms-2.4.jar:$JSTK_LIB_ROOT/jspeex-0.9.7-jfcom.jar:$JSTK_LIB_ROOT/log4j-1.2.16.jar
```

# Workflow #
```
#!/bin/bash

source Script_to_set_environment.sh #set classpath

#settings
subs='_17s'
dimens='_6D'
feats='_feat'
array=( dj_01_ dj_04_ dj_05_ dj_06_ dj_07_ dj_08_ dj_09_ dj_12_ dj_22_ dj_24_ dj_25_ dj_26_ dj_27_ dj_28_ dj_29_ dj_30_ dj_31_);
#array=( dj_01_ dj_04_ dj_05_ dj_06_ dj_07_ dj_08_ dj_09_ dj_12_ );
#array=( dj_22_ dj_24_ dj_25_ dj_26_ dj_27_ dj_28_ dj_29_ dj_30_ dj_31_);
#array=( dj_01_ );
#list='list'
list='list_long'
#list='list2'
feat_set=ftns

#parameters
slope_width=3 #was 3
slope_order=1 #was 1
gmm_comp=10 #was 4
em_iter=25 #was 5
vit_iter=20 #was 10

nbr_cores=4

# convert data to correct format
# Comment in to convert features
mkdir ft
#TODO: change data directory here
for i in ../<DataDirectory>/*_fil.txt; do 
	bn=$(basename "$i" _fil.txt); #save basename
	cat $i | sed -e 's:,::g' | ./jstk app.Convert ascii frame > $feat_set/$bn.ft; # remove commas
done

# make alignment files (labels), these are the reference for evaluation
# Comment in to create labels
 mkdir alg
 #TODO: change data directory here
 for i in ../<DataDirectory>/*_label.txt; do
 	bn=`basename $i _label.txt`;
 	awk -v a=n '{ 
 		if($1 != a) { # count the phase lengths
  			if (n > 0) printf "/%s/ %d\n", a, n; 
  				n = 0; a = $1 
  			} n += 1 
  		} END { 
  		printf "/%s/ %d\n", a, n #print them
  	}' $i > alg/$bn.ft
 done
 sed -i -e 's:/0/:/si/:' alg/*.ft; # substitute label 0 with si
 rm alg/*.ft-e


# remove selected feature dimensions
mkdir ftsel
for i in $feat_set/*; do 
	bn=$(basename "$i"); #save basename
	./jstk framed.Selection -s "0-5" < $i > ftsel/$bn	
done
feat_set=ftsel


# normalize features by mean and variance
# uses "list": list of all files
# Comment in to normalize features
mkdir ftn
awk -v dir=${feat_sel} '{printf "%s/%s ftn/%s\n", dir, $1, $1}' $list > tmp
./jstk framed.MVN --in-out-list tmp
rm tmp
feat_set=ftn

# add slope features
mkdir ftns
./jstk framed.Slope $slope_width:$slope_order $list ftns ftn
feat_set=ftns

# Perform cross-validation
for (( i = 0 ; i < ${#array[@]} ; i++ )) do

#training files
cp $list list.tr
awk "!/${array[$i]}/" list.tr > temp && mv temp list.tr

#test files
cp $list list.te
awk "/${array[$i]}/" list.te > temp && mv temp list.te

#alignment files
 
#test files
cp list.te list.alg.te
paste list.alg.te | awk '/dj_2|dj_3/ {print $0 " undef jump undef jump undef jump undef jump undef jump undef jump undef"}' > temp
paste list.alg.te | awk '/dj_0|dj_1/ {print $0 " undef jump undef jump undef jump undef jump undef jump undef"}' >> temp
mv temp list.alg.te 
 
#training files
cp list.tr list.alg.tr
paste list.alg.tr | awk '/dj_2|dj_3/ {print $0 " undef jump undef jump undef jump undef jump undef jump undef jump undef"}' > temp
paste list.alg.tr | awk '/dj_0|dj_1/ {print $0 " undef jump undef jump undef jump undef jump undef jump undef"}' >> temp
mv temp list.alg.tr

# init codebook - initialize the state pdfs and save to cb.i
# - n: n-component gaussian mixture model
# - s: initialization strategy
# --list: list of files to use
# --dir: feature directory 
# uses "list.tr": list of training files
# Comment in to initialize codebook
./jstk app.Initializer --gmm cb.i -n $gmm_comp -s sequential_500 --list list.tr --dir $feat_set
#add -f for full-covariance

# train state pdfs with labeled data (Expectation Maximization algorithm)
# - n: number of iterations
# - p: parallelized on number of cores
# uses "cb.i": initialized codebook
# uses "list.tr": list of training files
# Comment in to train codebook
./jstk app.GaussEM -i cb.i -o cb.$em_iter -n $em_iter -l list.tr -d $feat_set/ -p $nbr_cores

# create context configuration
#set desired token context:
#	- 0: max possible context (limited by word boundaries)
#   - 1: no context
#   - 2: left context, e.g. h/a/  as in  #/hans/#
#   - 3: left and right context, e.g. h/a/n  as in #/hans/#
# - cont: context to use from codebook
# uses "dj.a": number of hidden states for each emission
# uses "dj.l": sequence information for events
# Comment in to create configuration
./jstk arch.Configuration --compile dj.a dj.l 0 --cont cb.$em_iter --write conf.xml conf.cb.0

# train the state transitions from labeled data, one iteration
# - manual_linear: manual alignment where possible, else: linear alignment
# -t: viterbi algorithm
# uses "list.alg.tr": sequence of word occurrences in training files
# uses "conf.cb.0": initial configuration
# Comment in to train state transitions
for j in `seq 1 $vit_iter`; do 
 	echo "iteration $j"
 	./jstk app.Trainer conf.xml conf.cb.$[$j-1] conf.cb.$j list.alg.tr $feat_set/ -a manual_linear alg/ -t vt -p $nbr_cores
 done

# test disjoint sequence
mkdir falg
# uses "list.alg.te": sequence of word occurrences in test files
./jstk app.Aligner conf.xml conf.cb.$vit_iter -l list.alg.te $feat_set/ falg -p $nbr_cores

rm list.*
rm cb.*
rm conf.*
done

#create result files
mkdir res$subs$dimens$feats
for i in falg/*; do 
	bn=$(basename "$i"); #save basename
	#Print result-reference result reference
	paste alg/$bn falg/$bn | awk '/\/1\// {print $4-$2 " " $3 $4 " " $1 $2}' > res$subs$dimens$feats/$bn
done
mv falg falg$subs$dimens$feats
```