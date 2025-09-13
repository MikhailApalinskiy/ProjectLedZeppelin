FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY . .
RUN mvn -q -DskipTests package

FROM tomcat:10.1-jdk21-temurin
WORKDIR /usr/local/tomcat

RUN rm -rf webapps/ROOT

COPY --from=builder /app/target/project-ledzeppelin-1.0-SNAPSHOT.war webapps/ROOT.war

RUN mkdir -p logs

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
CMD ["catalina.sh", "run"]