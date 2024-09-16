# Используем образ Maven для сборки
FROM maven:latest AS build

# Указываем рабочую папку в контейнере
WORKDIR /Finder

# Копируем все файлы проекта
COPY . /Finder

# Строим проект
RUN mvn clean package -DskipTests

# Используем легкий образ JDK для выполнения
FROM openjdk:17-jdk-alpine

# Указываем рабочую папку в контейнере
WORKDIR /Finder

# Копируем собранный JAR-файл из предыдущего этапа
COPY --from=build /Finder/Ads_microservice/target/Ads_microservice-0.0.1-SNAPSHOT.jar /Finder/ads_microservice.jar

# Запускаем приложение
CMD ["java", "-jar", "ads_microservice.jar"]

# Открываем порт
EXPOSE 8080
