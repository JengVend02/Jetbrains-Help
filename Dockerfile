FROM maven:3-openjdk-8 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/target/Jetbrains-Help.jar Jetbrains-Help.jar
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
EXPOSE 10768
ENTRYPOINT ["java", "-jar", "Jetbrains-Help.jar"]
