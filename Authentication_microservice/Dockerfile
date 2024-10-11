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
COPY --from=build /Finder/Authentication_microservice/target/Authentication_microservice-0.0.1-SNAPSHOT.jar /Finder/authentication_microservice.jar

# Запускаем приложение
CMD ["java", "-jar", "authentication_microservice.jar"]

# Открываем порт
EXPOSE 8083
