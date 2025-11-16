After up cassandra:

```
docker exec -it evalink-cassandra cqlsh -e "CREATE KEYSPACE IF NOT EXISTS notification_keyspace WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};"
```
