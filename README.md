# Food Delivery app

This is a very simple food delivery distributed system. It allows to search for food and then order them with a delivery.

![image-1](/docs/image-1.png)
![image-2](/docs/image-2.png)
![image-3](/docs/image-3.png)
![image-4](/docs/image-4.png)

The entire system is made of 5 microservices (1 React, 4 Java/Spring):

* *ui* - React application, used by customers to place orders, manage them and track deliveries, url: http://localhost:80,
* *bff* - backend for frontend service, used to provide REST endpoint for *ui*, url: http://localhost:8081/swagger-ui.html,
* *food* - service that handles adding available meals to Redis, url: http://localhost:8084/swagger-ui.html,
* *ordering* - core service for managing orders,
* *delivery* - core service for managing deliveries.

# Overview video (Optional)

Here's a short video that explains the project and how it uses Redis:

[![Watch the video](https://img.youtube.com/vi/j2OgyTJFc14/default.jpg)](https://youtu.be/j2OgyTJFc14)

## How it works

Here is the overview of a system architecture with used Redis modules:

![architecture](/docs/architecture.png)

Most of the communication is based on commands and events. E.g. in order to place an order a proper command needs to be pushed to `orders` Redis Stream. It's then read and processed by the `ordering` service and result in an event which also added to the central `orders` stream, so other services, like `delivery` can read and process further. 

Also `bff` is reading from the `orders` stream to create its own read model (Command Query Responsibility Segregation, CQRS) of all deliveries and store it in Redis Hash. These are used to serve a current state of a delivery on a frontend.

Both `ordering` and `delivery` services have their own event stores in which they store relevant events, so they can event source them to make a projection of an order or a delivery.

All requests that are coming from a frontend are first queued in two Redis Task queues - `ordering-inbox` and `delivery-inbox`. These inboxes are used to store all incoming REST requests to `bff` before converting them to relevant commands and publishing to the `orders` stream. 

Finally the `food` service stores all available meals in the `food` RedisJSON store. It also has an index created which enables a full-text search of all meals on a frontend. 

### How the data is stored:

There are several Redis modules that are used to store data:

* `orders` - Redis Stream, used to store commands and events as JSON. It stores all events that are happening across the entire system. `bff` is publishing commands into it. `ordering` & `delivery` are publishing events. Exemplary event:
```json
{
   "header": {
      "messageId":"c065e910-1806-4ab5-b1c9-8c8a105323f6",
      "channel":"orders",
      "type":"FoodDelivered",
      "itemId":"order-2",
      "createdAt":"2022-08-28T12:14:10.557171900Z"
   },
   "body":{
      "orderId":"order-2"
   }
}
```
![orders stream](/docs/orders-stream.png)

* `ordering::[orderId]` & `delivery::[orderId]` - Redis Streams, used to store only events relevant events for each service as JSON. Each order/delivery has its own stream. They hold the same events as it's in the `orders` stream.

![delivery stream](/docs/delivery-stream.png)

* `delivery-view` - Redis Hash, used to store delivery read models used for a frontend. `field` in the hash stores an orderId and `value` stores a projection of a delivery. Data is populated here by the `bff` service. Exemplary delivery view:

```json
{
   "orderId":"order-2",
   "customerId":"Pam Beesly",
   "restaurantId":"Ristorante Da Aldo",
   "deliveryManId":"nicest-guy",
   "status":"FOOD_DELIVERED",
   "address":"Cottage Avenue 11",
   "items":[
      {
         "name":"Lemony Asparagus Penne",
         "amount":1,
         "pricePerItem":9.49
      },{
         "name":"Tea",
         "amount":1,
         "pricePerItem":1.99
      }
   ],
   "deliveryCharge":1.99,
   "tip":4.59,
   "total":18.06
}
```

![delivery view](/docs/delivery-view.png)

* `__rq` - Redis task queue, two inboxes (`ordering-inbox` & `delivery-inbox`) used to store incoming REST request and queue their processing. Their entire lifecycle is managed by the library [sonus21/rqueue](https://github.com/sonus21/rqueue). Data is here populated and consumed by the `bff` service.

![redis task queue](/docs/rqueue.png)

* `food:[foodId]` - RedisJSON document, stores information about meals. Data are populated by the `food` service.

![food json](/docs/food-json.png)

* `food-idx` - RedisJSON index, index for `food:[foodId]` RedisJson document used to enable full-text search of meal name.

![food idx](/docs/food-idx.png)

### How the data is accessed:

* `orders` - `bff`, `ordering` & `delivery` are consumers of this stream. They're using standard Spring Boot `StreamListener`.
* `ordering::[orderId]` & `delivery::[orderId]` - `ordering` & `delivery` services are the producers and consumers for these event stores. They're using the Spring Boot `RedisTemplate` to achieve it.
* `delivery-view` - `bff` is adding and fetching data from this hash using the Spring Boot `RedisTemplate`.
* `__rq` - `bff` is queueing and consuming data from the task queue using the [sonus21/rqueue](https://github.com/sonus21/rqueue) library.
* `food:[foodId]` & `food-idx` - `bff` is using the RedisLab's `StatefulRediSearchConnection` to full-text search available meals.

## How to run it locally?

### Prerequisites

* Docker - v20.10.17 (tested on, but earlier should work too)
* Docker Compose - v2.7.0

### Local installation

#### Vanilla

Run all commands in a terminal:

1. Start redis
```bash
docker-compose up -d redis
```

2. Create `food-idx`
```bash
docker run --rm -it --network food-delivery-redis_default redis redis-cli -h redis FT.CREATE food-idx ON JSON PREFIX 1 "food:" SCHEMA $.name AS name TEXT
```

3. Start all services
```bash
docker-compose up -d bff ordering delivery food ui
```

4. Run script to populate Redis with initial data
```bash
bash init-data.sh
```

##### Shutting down

1. Stop all containers
```bash
docker-compose down --rmi local
```
2. Clean data:
```bash
docker volume rm food-delivery-redis_redis-data 
```

#### With Taskfile

In case you've got installed [Taskfile](https://taskfile.dev) just run the command:

```bash
task init
```

##### Shutting down

Run a command:

```bash
task infra-clean
```

## More Information about Redis Stack

Here some resources to help you quickly get started using Redis Stack. If you still have questions, feel free to ask them in the [Redis Discord](https://discord.gg/redis) or on [Twitter](https://twitter.com/redisinc).

### Getting Started

1. Sign up for a [free Redis Cloud account using this link](https://redis.info/try-free-dev-to) and use the [Redis Stack database in the cloud](https://developer.redis.com/create/rediscloud).
1. Based on the language/framework you want to use, you will find the following client libraries:
    - [Redis OM .NET (C#)](https://github.com/redis/redis-om-dotnet)
        - Watch this [getting started video](https://www.youtube.com/watch?v=ZHPXKrJCYNA)
        - Follow this [getting started guide](https://redis.io/docs/stack/get-started/tutorials/stack-dotnet/)
    - [Redis OM Node (JS)](https://github.com/redis/redis-om-node)
        - Watch this [getting started video](https://www.youtube.com/watch?v=KUfufrwpBkM)
        - Follow this [getting started guide](https://redis.io/docs/stack/get-started/tutorials/stack-node/)
    - [Redis OM Python](https://github.com/redis/redis-om-python)
        - Watch this [getting started video](https://www.youtube.com/watch?v=PPT1FElAS84)
        - Follow this [getting started guide](https://redis.io/docs/stack/get-started/tutorials/stack-python/)
    - [Redis OM Spring (Java)](https://github.com/redis/redis-om-spring)
        - Watch this [getting started video](https://www.youtube.com/watch?v=YhQX8pHy3hk)
        - Follow this [getting started guide](https://redis.io/docs/stack/get-started/tutorials/stack-spring/)

The above videos and guides should be enough to get you started in your desired language/framework. From there you can expand and develop your app. Use the resources below to help guide you further:

1. [Developer Hub](https://redis.info/devhub) - The main developer page for Redis, where you can find information on building using Redis with sample projects, guides, and tutorials.
1. [Redis Stack getting started page](https://redis.io/docs/stack/) - Lists all the Redis Stack features. From there you can find relevant docs and tutorials for all the capabilities of Redis Stack.
1. [Redis Rediscover](https://redis.com/rediscover/) - Provides use-cases for Redis as well as real-world examples and educational material
1. [RedisInsight - Desktop GUI tool](https://redis.info/redisinsight) - Use this to connect to Redis to visually see the data. It also has a CLI inside it that lets you send Redis CLI commands. It also has a profiler so you can see commands that are run on your Redis instance in real-time
1. Youtube Videos
    - [Official Redis Youtube channel](https://redis.info/youtube)
    - [Redis Stack videos](https://www.youtube.com/watch?v=LaiQFZ5bXaM&list=PL83Wfqi-zYZFIQyTMUU6X7rPW2kVV-Ppb) - Help you get started modeling data, using Redis OM, and exploring Redis Stack
    - [Redis Stack Real-Time Stock App](https://www.youtube.com/watch?v=mUNFvyrsl8Q) from Ahmad Bazzi
    - [Build a Fullstack Next.js app](https://www.youtube.com/watch?v=DOIWQddRD5M) with Fireship.io
    - [Microservices with Redis Course](https://www.youtube.com/watch?v=Cy9fAvsXGZA) by Scalable Scripts on freeCodeCamp