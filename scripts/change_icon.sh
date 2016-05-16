#!/usr/bin/env bash

# usage: ./change_icon.sh -i input_file -o output_dir

INPUT_FILE=""
OUTPUT_DIR=""
while [[ $# > 1 ]]
do
	key="$1"
	case $key in
	-i)
		INPUT_FILE="$2"
		shift
		;;
	-o)
		OUTPUT_DIR="$2"
		shift
		;;
	*)
		;;
	esac
	shift
done

if [[ -z $INPUT_FILE ]] || [[ -z $OUTPUT_DIR ]]
then
	echo Usage: $0 -i input_file -o output_dir
else
	declare -A dict
	dict=(["hdpi"]=72 ["mdpi"]=48 ["xhdpi"]=96 ["xxhdpi"]=144
	["xxxhdpi"]=192)
	for res in "${!dict[@]}"
	do
		n=${dict[$res]}
		ffmpeg -i $INPUT_FILE -vf scale=$n:$n $OUTPUT_DIR/mipmap-$res/ic_launcher.png -y
	done
fi
