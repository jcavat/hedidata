# Therapists management system


Run mongodb with docker-compose:

```
docker-compose up -d
```

Create `db-users` database and `users` collection with mongo-express at [http://localhost:8081](http://localhost:8081)

Run the server with `sbt run` and try:

post a therapist:

```
curl -H "Content-Type: application/json" -H "Authorization: Bearer ABCD" http://localhost:8080/therapists -X POST -d '{"firstName": "Joel", "lastName": "Cavat", "email": "toto@toto.com"}'
```

get all therapists:

```
curl -H "Content-Type: application/json" -H "Authorization: Bearer ABCD" http://localhost:8080/therapists
```
