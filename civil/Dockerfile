FROM openjdk:11

WORKDIR /

ARG path="target/scala-2.13"


ADD ./${path}/civil-assembly-0.2.0.jar /
ADD ./${path}/civil-0.1-SNAPSHOT.jar /
ADD ./src/main/resources/application.conf /


EXPOSE 8090

CMD java -jar ./civil-0.1-SNAPSHOT.jar && java -jar ./civil-assembly-0.2.0.jar
