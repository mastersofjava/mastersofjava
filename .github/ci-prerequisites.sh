# Reclaim disk space, otherwise we have too little free space at the start of a job
#
time sudo docker image prune --all --force || true