#!/bin/bash

function create_order() {
  request_body=$1
  curl -d "$(cat "$request_body")" -H "Content-Type: application/json" -X POST http://localhost:8081/orders
}

function add_tip() {
  order_id=$1
  curl -d '{"tip": 4.59}' -H "Content-Type: application/json" -X POST http://localhost:8081/orders/$order_id/tip
}

function assign_deliveryman() {
  order_id=$1
  delivery_man=$2
  curl -d "{\"deliveryManId\": \"$delivery_man\"}" -H "Content-Type: application/json" -X POST http://localhost:8081/deliveries/$order_id/delivery-man
}

function cancel_order() {
  order_id=$1
  reason=$2
  curl -d "{\"reason\": \"$reason\"}" -H "Content-Type: application/json" -X PATCH http://localhost:8081/orders/$order_id/status/cancel
}

function prepare_food() {
  order_id=$1
  curl -d '{"status": "prepareFood"}' -H "Content-Type: application/json" -X PATCH http://localhost:8081/deliveries/$order_id/
}

function food_ready() {
  order_id=$1
  curl -d '{"status": "foodReady"}' -H "Content-Type: application/json" -X PATCH http://localhost:8081/deliveries/$order_id/
}

function food_picked() {
  order_id=$1
  curl -d '{"status": "pickUpFood"}' -H "Content-Type: application/json" -X PATCH http://localhost:8081/deliveries/$order_id/
}

function food_delivered() {
  order_id=$1
  curl -d '{"status": "deliverFood"}' -H "Content-Type: application/json" -X PATCH http://localhost:8081/deliveries/$order_id/
}

echo 'Populating init data...'

x=0
while [ $x -le 10 ]
do
  printf "\nChecking availability of the 'food' service...\n"
  status_code=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost:8084/actuator/health)

  if [[ $status_code -eq "200" ]] ; then
    printf "'food' service is up. Inserting meals data..."
    curl -d "$(cat food-data.json)" -H "Content-Type: application/json" -X POST http://localhost:8084/foods
    printf "\n\nMeals data has been inserted\n"
    break
  else
    printf "'food' service is not ready yet"
    ((x++))
    sleep 8
  fi
done

printf "\nCreating initial orders..."

while [ $x -le 10 ]
do
  printf "\nChecking availability of the 'bff', 'ordering' and 'delivery' services.."
  bff_code=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost:8081/actuator/health)
  ordering_code=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost:8082/actuator/health)
  delivery_code=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost:8083/actuator/health)

  if [[ $bff_code -eq "200" && $ordering_code -eq "200" && $delivery_code -eq "200" ]] ; then
    printf "\n'bff', 'ordering' and 'delivery' services are up. Inserting initial orders...\n"
    sleep 10

    printf "\nAdding orders...\n"
    create_order "order-1.json"
    sleep 1
    create_order "order-2.json"
    create_order "order-3.json"
    create_order "order-4.json"

    printf "\nAdding tips...\n"
    sleep 1
    add_tip "order-1"
    add_tip "order-2"
    add_tip "order-3"

    printf "\nAssigning delivery men...\n"
    assign_deliveryman "order-1" "cheapest-guy"
    assign_deliveryman "order-2" "nicest-guy"
    assign_deliveryman "order-3" "fastest-guy"

    printf "\nProcessing deliveries...\n"
    cancel_order "order-1" "party won't start, therefore I don't need it"

    sleep 1
    prepare_food "order-2"
    prepare_food "order-3"

    sleep 1
    food_ready "order-2"
    sleep 1
    food_picked "order-2"
    sleep 1
    food_delivered "order-2"

    printf "\n\nOrders data has been inserted"
    break
  else
    printf "\n'bff', 'ordering' or 'delivery' service is not ready yet"
    ((x++))
    sleep 8
  fi
done