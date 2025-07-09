kiwi-console stop
rm -rf $HOME/develop/kiwi-console
unzip -d $HOME/develop dist/target/kiwi-console.zip
cp -f /etc/kiwi/kiwi-console.yml $HOME/develop/kiwi-console/config
kiwi-console start
