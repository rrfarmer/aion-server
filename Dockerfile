ARG MODULE

FROM maven:3.9.11-eclipse-temurin-25 AS build
ARG MODULE
WORKDIR /workspace

COPY pom.xml ./
COPY commons/pom.xml commons/pom.xml
COPY chat-server/pom.xml chat-server/pom.xml
COPY game-server/pom.xml game-server/pom.xml
COPY login-server/pom.xml login-server/pom.xml

COPY commons commons
COPY chat-server chat-server
COPY game-server game-server
COPY login-server login-server

RUN mvn -B -Dmaven.source.skip=true -Dmaven.test.skip=true -pl :${MODULE} -am package
RUN mkdir -p /runtime && cd /runtime && jar xf /workspace/${MODULE}/target/${MODULE}.zip
RUN find /runtime -name '*.sh' -exec sed -i 's/\r$//' {} +

FROM eclipse-temurin:25-jdk-jammy AS runtime
ARG MODULE
WORKDIR /app

COPY --from=build /runtime/ ./

WORKDIR /app/${MODULE}
RUN chmod +x start.sh

CMD ["./start.sh"]