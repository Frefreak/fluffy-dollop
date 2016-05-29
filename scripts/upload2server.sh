#!/usr/bin/env bash
# usage: ./upload2server.sh username@serverip server_directory

if [[ -z $1 ]] || [[ -z $2 ]]
then
	echo Usage: $0 username@serverip server_directory
else
	cd $2
	stack install --local-bin-path .
	gzip -f ./cliperServer
	tar czvf static.tar.gz static
	scp ./cliperServer.gz $1:
	scp ./static.tar.gz $1:
	ssh $1 'killall cliperServer'
	ssh $1 'gzip -df ./cliperServer.gz' 
	ssh $1 './cliperServer > log &'
	ssh $1 'tar xf static.tar.gz'
	ssh $1 'rm -f static.tar.gz'
	rm -f ./cliperServer.gz ./static.tar.gz
	cd -
fi
