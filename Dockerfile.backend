FROM openjdk:17-slim as build
WORKDIR /app
COPY ./ ./
RUN ./gradlew clean assemble --info


FROM openjdk:17-alpine as main
ARG app_name
WORKDIR /app
COPY --from=build /app/$app_name/build/libs/$app_name-0.0.1-SNAPSHOT.jar ./app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod,redis"]




