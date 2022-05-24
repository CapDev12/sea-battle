#bin/bash

for i in {1..100}
do
  sleep .5;scripts/test1.sh &
done
