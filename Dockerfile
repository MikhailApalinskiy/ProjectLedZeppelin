FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY . ./
RUN mvn -q -DskipTests package


RUN set -eux; \
  mkdir -p /app/build-changelog; \
  FOUND=""; \
  if [ -d /app/src/main/resources/db/changelog ]; then \
      cp -R /app/src/main/resources/db/changelog /app/build-changelog/changelog; FOUND=1; \
  fi; \
  if [ -z "$FOUND" ] && [ -d /app/target/classes/db/changelog ]; then \
      cp -R /app/target/classes/db/changelog /app/build-changelog/changelog; FOUND=1; \
  fi; \
  if [ -z "$FOUND" ]; then \
      WAR_CLASSES="$(ls -d /app/target/*/WEB-INF/classes 2>/dev/null | head -n1 || true)"; \
      if [ -n "$WAR_CLASSES" ] && [ -d "$WAR_CLASSES/db/changelog" ]; then \
          cp -R "$WAR_CLASSES/db/changelog" /app/build-changelog/changelog; FOUND=1; \
      fi; \
  fi; \
  if [ -z "$FOUND" ]; then \
      echo '!! db/changelog not found in src or target; check your project layout'; \
      find /app -maxdepth 5 -type d -name changelog -o -name "db*" | sed 's/^/ -> /'; \
      exit 1; \
  fi; \
  echo "== [BUILD] normalized changelog tree =="; ls -la /app/build-changelog/changelog || true

FROM tomcat:10.1-jdk21-temurin
WORKDIR /usr/local/tomcat

COPY conf/server.xml conf/server.xml

RUN rm -rf webapps/ROOT
COPY --from=builder /app/target/project-ledzeppelin-1.0-SNAPSHOT.war webapps/ROOT.war

ARG LIQUIBASE_VERSION=4.30.0
RUN apt-get update -y \
 && apt-get install -y --no-install-recommends curl unzip \
 && rm -rf /var/lib/apt/lists/*
RUN mkdir -p /liquibase \
 && curl -fsSL -o /tmp/liquibase.zip "https://github.com/liquibase/liquibase/releases/download/v${LIQUIBASE_VERSION}/liquibase-${LIQUIBASE_VERSION}.zip" \
 && unzip -q /tmp/liquibase.zip -d /liquibase \
 && rm /tmp/liquibase.zip

COPY --from=builder /app/build-changelog/changelog/ /liquibase/db/changelog/

RUN set -eux; \
  if [ -d /liquibase/db.changelog ]; then \
    mkdir -p /liquibase/db/changelog; \
    cp -R /liquibase/db.changelog/. /liquibase/db/changelog/; \
  fi; \
  ln -snf /liquibase/db/changelog /liquibase/db.changelog; \
  echo "== [RUNTIME] /liquibase =="; ls -la /liquibase || true; \
  echo "== [RUNTIME] /liquibase/db =="; ls -la /liquibase/db || true; \
  echo "== [RUNTIME] /liquibase/db/changelog =="; ls -la /liquibase/db/changelog || true

ARG BUILD_ID=dev
ENV BUILD_ID=${BUILD_ID}

RUN cat >/entrypoint.sh <<'SH'
#!/bin/sh
set -e

echo "[entrypoint] BUILD_ID=${BUILD_ID}"
echo "[entrypoint] starting..."
echo "[entrypoint] listing /liquibase/db/changelog:"; ls -la /liquibase/db/changelog || true

MASTER_FILE=""
MASTER_DIR=""
for p in /liquibase/db/changelog /liquibase/db.changelog; do
  if [ -z "$MASTER_FILE" ] && [ -f "$p/db.changelog-master.yaml" ]; then MASTER_FILE="db.changelog-master.yaml"; MASTER_DIR="$p"; fi
  if [ -z "$MASTER_FILE" ] && [ -f "$p/db.changelog-master.yml"  ]; then MASTER_FILE="db.changelog-master.yml";  MASTER_DIR="$p"; fi
done

if [ -z "$MASTER_FILE" ] || [ -z "$MASTER_DIR" ]; then
  echo "[entrypoint] ERROR: master changelog not found (.yaml/.yml) in /liquibase/db/changelog or /liquibase/db.changelog"
  echo "[entrypoint] Tree under /liquibase:"; ls -R /liquibase || true
  exit 1
fi

echo "[entrypoint] using master: $MASTER_DIR/$MASTER_FILE"
echo "[entrypoint] pwd: $(pwd)"
echo "[entrypoint] stat:"; stat "$MASTER_DIR/$MASTER_FILE" || true

if [ -n "$LIQUIBASE_URL" ] && [ -n "$LIQUIBASE_USERNAME" ] && [ -n "$LIQUIBASE_PASSWORD" ]; then
  echo "[entrypoint] running liquibase update..."
  LIQ_URL="$LIQUIBASE_URL"; LIQ_USER="$LIQUIBASE_USERNAME"; LIQ_PASS="$LIQUIBASE_PASSWORD"
  unset LIQUIBASE_URL LIQUIBASE_USERNAME LIQUIBASE_PASSWORD || true

  cd "$MASTER_DIR"
  echo "[entrypoint] now in: $(pwd)"; ls -la; test -r "$MASTER_FILE"

  /liquibase/liquibase \
    --search-path="$(pwd)" \
    --changelog-file="$MASTER_FILE" \
    --url="$LIQ_URL" --username="$LIQ_USER" --password="$LIQ_PASS" \
    --log-level=info --contexts=prod update
else
  echo "[entrypoint] LIQUIBASE_* env not set; skipping migrations"
fi

PORT_VAL="${PORT:-8080}"
HTTPS_VAL=$(( PORT_VAL + 363 ))

export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dhttp.port=${PORT_VAL} -Dhttps.port=${HTTPS_VAL}"
echo "[entrypoint] http.port=${PORT_VAL} https.port=${HTTPS_VAL}"

cd /usr/local/tomcat
sed -ri "s@port=\"8080\"@port=\"${PORT_VAL}\"@" conf/server.xml
sed -ri "s@redirectPort=\"8443\"@redirectPort=\"${HTTPS_VAL}\"@" conf/server.xml
echo "[entrypoint] patched server.xml:"; grep -n "Connector" conf/server.xml || true

exec catalina.sh run
SH
RUN chmod +x /entrypoint.sh

RUN mkdir -p logs

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
CMD ["/entrypoint.sh"]