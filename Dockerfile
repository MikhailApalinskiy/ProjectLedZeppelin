FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY . ./
RUN mvn -q -DskipTests package

FROM tomcat:10.1-jdk21-temurin
WORKDIR /usr/local/tomcat

COPY conf/server.xml conf/server.xml

RUN rm -rf webapps/ROOT
COPY --from=builder /app/target/project-ledzeppelin-1.0-SNAPSHOT.war webapps/ROOT.war

ENV LIQUIBASE_VERSION=4.30.0
RUN apt-get update -y && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*
RUN mkdir -p /liquibase && \
    curl -fsSL -o /tmp/liquibase.zip "https://github.com/liquibase/liquibase/releases/download/v${LIQUIBASE_VERSION}/liquibase-${LIQUIBASE_VERSION}.zip" && \
    unzip -q /tmp/liquibase.zip -d /liquibase && rm /tmp/liquibase.zip

COPY --from=builder /root/.m2/repository/org/postgresql/postgresql/*/postgresql-*.jar /liquibase/lib/

COPY --from=builder /app/src/main/resources/db/changelog /liquibase/db/changelog

RUN printf '%s\n' \
  'classpath=/liquibase/lib' \
  'changeLogFile=/liquibase/db/changelog/db.changelog-master.yaml' \
  > /liquibase/liquibase.properties

COPY --from=builder /bin/sh /bin/sh
RUN printf '%s\n' '#!/bin/sh' \
  'set -e' \
  'echo "[entrypoint] starting..."' \
  'if [ -n "$LIQUIBASE_URL" ] && [ -n "$LIQUIBASE_USERNAME" ] && [ -n "$LIQUIBASE_PASSWORD" ]; then' \
  '  echo "[entrypoint] running liquibase update...";' \
  '  /liquibase/liquibase \\' \
  '    --defaultsFile=/liquibase/liquibase.properties \\' \
  '    --url="$LIQUIBASE_URL" \\' \
  '    --username="$LIQUIBASE_USERNAME" \\' \
  '    --password="$LIQUIBASE_PASSWORD" \\' \
  '    --log-level=info \\' \
  '    --contexts=prod \\' \
  '    update;' \
  'else' \
  '  echo "[entrypoint] LIQUIBASE_* env not set; skipping migrations";' \
  'fi' \
  'exec catalina.sh run' \
  > /entrypoint.sh && chmod +x /entrypoint.sh

RUN mkdir -p logs
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

CMD ["/entrypoint.sh"]