# Build the WAR with Maven, then run it on Tomcat 10.1 (Jakarta EE 10: Servlet 6, JSP 3.1).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM tomcat:10.1-jre21-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/harvesta.war /usr/local/tomcat/webapps/ROOT.war
ENV APP_ENV=production
EXPOSE 8080
# Free hosts (Render, Koyeb, ...) pass the port in $PORT.
CMD sed -i "s/port=\"8080\"/port=\"${PORT:-8080}\"/" conf/server.xml && exec catalina.sh run
