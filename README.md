# IWA-Microservices
An insecure Microservices Application for use in Fortify demonstrations.

Infrastructure
--------------

Add the following aliases to your `/etc/hosts` file for `localhost`:

```
127.0.0.1   localhost   nosql-db    rabbitmq
```

```
docker compose -f 'docker-compose.yml' up -d --build 'nosql-db'
docker compose -f 'docker-compose.yml' up -d --build 'rabbitmq'
```

If you are interested, you can login to RabbitMQ management console at `http://localhost:1567` using `guest/guest`.

Users Service
-------------

```
cd user-service
npm install
npm run dev
```