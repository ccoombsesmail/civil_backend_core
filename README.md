docker-compose up --build

docker exec -it broker bash

kafka-topics \
  --bootstrap-server localhost:9092 \
  --topic notifications \
  --create