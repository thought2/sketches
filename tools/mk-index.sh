dir=$1
file=index.txt

cd $dir;
for i in *; do echo $i >> $file; done;
