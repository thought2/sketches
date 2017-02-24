dir=$1
file=index.txt

cd $dir;
rm $dir/$file
for i in *; do echo $i >> $file; done;
