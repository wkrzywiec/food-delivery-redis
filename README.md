# Food Delivery app

This is a very simple food delivery, event-driven distributed system. It allows to search for food and then order them with a delivery.

![image-1](/docs/image-1.png)
![image-2](/docs/image-2.png)
![image-3](/docs/image-3.png)
![image-4](/docs/image-4.png)

The entire system is made of 5 microservices (1 React, 4 Java/Spring):

* *ui* - React application, used by customers to place orders, manage them and track deliveries,
* *bff* - backend for frontend service, used to provide REST endpoint for *ui*,
* *food* - service that handles adding available meals to Redis,
* *ordering* - core service for managing orders,
* *delivery* - core service for managing deliveries.

# Overview video (Optional)

Here's a short video that explains the project and how it uses Redis:

[Insert your own video here, and remove the one below]

[![Embed your YouTube video](https://i.ytimg.com/vi/vyxdC1qK4NE/maxresdefault.jpg)](https://www.youtube.com/watch?v=vyxdC1qK4NE)

## How it works

Here is the overview of a system architecture with used Redis modules:

![architecture](/docs/architecture.png)

### How the data is stored:

Most communication is based on events, stored in a common `orders` stream.

Refer to [this example](https://github.com/redis-developer/basic-analytics-dashboard-redis-bitmaps-nodejs#how-the-data-is-stored) for a more detailed example of what you need for this section.

### How the data is accessed:

Refer to [this example](https://github.com/redis-developer/basic-analytics-dashboard-redis-bitmaps-nodejs#how-the-data-is-accessed) for a more detailed example of what you need for this section.

## How to run it locally?

### Prerequisites

* Docker

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
docker-compose down --rmi 
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