```
git clone https://github.com/making/spring-boot-app-console.git
cd spring-boot-app-console
./mvnw clean package -DskipTests=true
java -jar target/spring-boot-app-console-0.0.1-SNAPSHOT.jar
```


```
cd /tmp
curl https://start.spring.io/starter.tgz -d dependencies=web,actuator | tar -xzvf -
./mvnw clean package -DskipTests=true

export VCAP_APPLICATION_CF_API=http://localhost:9000
export VCAP_SERVICES=
export VCAP_APPLICATION_APPLICATION_ID=demo

java -jar target/demo-0.0.1-SNAPSHOT.jar
```


```
curl -u admin:admin localhost:9000/services/demo/0/cloudfoundryapplication/env
curl -u admin:admin localhost:9000/services/demo/0/cloudfoundryapplication/httptrace
```