> ⚠️ **Предупреждение:** Эта версия README на русском языке может содержать неточности или быть неполной. За самой актуальной и достоверной информацией рекомендуется обращаться к [основной версии README](README.md) на английском.

# Обзор

Платформа позволяет пользователям создавать объявления о поиске команд для своих проектов или присоединяться к уже существующим. Цель платформы - помочь пользователям найти возможность получить опыт командной работы и повысить свои навыки.

Каждый может участвовать и заимствовать код, но важно соблюдать условия лицензии.

<details>
  <summary><strong>В настоящее время реализовано:</strong></summary>

  ### Управление объявлениями
  - **Создание, редактирование и удаление объявлений.**
  - **Просмотр списка объявлений.**
  - **Добавление или удаление объявлений из избранного.**
  - **Интеграция с поисковой системой Elasticsearch.**

  ### Управление комментариями и ответами
  - **Создание, редактирование и удаление комментариев к объявлениям.**
  - **Добавление и управление ответными комментариями.**

  ### Управление профилем пользователя и аккаунтом
  - **Регистрация и вход пользователя.**
  - **Редактирование информации профиля пользователя.**
  - **Обновление аватара профиля.**
  - **Просмотр профиля пользователя.**
  - **Удаление учетной записи пользователя.**

  ### Контроль доступа и безопасность
  - **Проверка токена доступа.**
  - **Выдача нового токена доступа с использованием обновляющего токена.**

  ### Инструменты администратора и управление жалобами
  - **Подавать жалобы на пользователей или объявления.**
    - **Привилегии администратора включают:**
      - Просмотр списка жалоб.
      - Удаление жалоб.
  - **Модерация пользователей:**
    - Банить или разбанивать пользователей.
    - Корректировать роли пользователей (повышение или понижение).  
</details>

---
### Настройки

| Окружение            | Описание                                                                                                   |
|:---------------------:|:---------------------------------------------------------------------------------------------------------:|
| X_API_KEY             | Ключ для подключения внутренних API друг к другу, который не должен быть доступен извне                   |
| ELASTIC_PASSWORD      | Пароль от пользователя elasticSearch                                                                      |
| POSTGRES_DB           | Название вашей базы данных                                                                                |
| POSTGRES_USER         | Логин от вашего postgres                                                                                  |
| POSTGRES_PASSWORD     | Пароль для вашего postgres                                                                                |
| MINIO_ROOT_USER       | Логин от minIO                                                                                            |
| MINIO_ROOT_PASSWORD   | Пароль для вашего minIO                                                                                   |

---

# Микросервисы

### Микросервис объявлений
Управляет объявлениями и их содержимым, включая жалобы пользователей.
<details>
  <summary><strong>Эндпоинты:</strong></summary>

#### **1. GET** `/card/{id}/get`

- **Описание**: Получить объявление по его уникальному идентификатору.
- **Параметр пути**:
    - `id` — Уникальный идентификатор объявления.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Пример ответа**:
    ```json
    {
       "id": 15,
       "title": "2-я карточка",
       "text": "описание 2-й карточки",
       "createTime": "2024-10-11T11:13:21.96246",
       "images": [
           {
               "id": 55,
               "imageBucket": "images",
               "imageName": "image-name1.jpg"
           },
           {
               "id": 50,
               "imageBucket": "images",
               "imageName": "image-name2.jpg"
           },
           {
               "id": 51,
               "imageBucket": "images",
               "imageName": "image-name3.jpg"
           },
           {
               "id": 52,
               "imageBucket": "images",
               "imageName": "image-name4.jpg"
           },
           {
               "id": 53,
               "imageBucket": "images",
               "imageName": "image-name5.jpg"
           }
       ],
       "authorName": "johndoe123456789"
   }
    ```
    
- **Коды ответа**:
    - `200 OK`: Успешное получение.
    - `404 Not Found`: Если объявление с указанным идентификатором не существует.

---

#### **2. GET** `/card/getAll/{pageNumber}/{limit}`

- **Описание**: Получить все объявления с поддержкой пагинации.
- **Параметры пути**:
    - `pageNumber` — Номер страницы для получения.
    - `limit` — Количество объявлений на странице.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Пример ответа**:
    ```json
    {
       "cards": [
           {
               "id": 15,
               "title": "2-я карточка",
               "text": "описание 2-й карточки",
               "createTime": "2024-10-11T11:13:21.96246",
               "images": [
                   {
                       "id": 55,
                       "imageBucket": "images",
                       "imageName": "image-name1.jpg"
                   },
                   {
                       "id": 50,
                       "imageBucket": "images",
                       "imageName": "image-name2.jpg"
                   },
                   {
                       "id": 51,
                       "imageBucket": "images",
                       "imageName": "image-name3.jpg"
                   },
                   {
                       "id": 52,
                       "imageBucket": "images",
                       "imageName": "image-name4.jpg"
                   },
                   {
                       "id": 53,
                       "imageBucket": "images",
                       "imageName": "image-name5.jpg"
                   }
               ],
               "authorName": "johndoe123456789"
           }
       ],
       "last": true,
       "totalPages": 1,
       "totalElements": 1,
       "first": true,
       "numberOfElements": 1
  }
  ```
    
- **Коды ответа**:
    - `200 OK`: Успешное получение.
    - `400 Bad Request`: Если предоставлены некорректные данные.

---

#### **3. GET** `/complaint/get`

- **Описание**: Получить список жалоб.
- **Параметры запроса**:
    - `limit` (опционально, по умолчанию: 5) — Максимальное количество жалоб для возврата.
    - `page` (опционально, по умолчанию: 0) — Номер страницы для постраничной навигации.
    - `complaintType` (опционально, по умолчанию: `all`) — Тип жалобы для фильтрации:
        - `card` — Получить жалобы, связанные с объявлениями.
        - `user` — Получить жалобы, связанные с пользователями.
        - Если не указано или недействительно, будут возвращены все жалобы.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Пример ответа**:
    ```json
    {
	    "complaints": [
	        {
	            "type": "card",
	            "complaintId": 6,
	            "reason": "reason",
	            "complaintAuthorName": "venik6",
	            "cardId": 2
	        },
	        {
	            "type": "user",
	            "complaintId": 7,
	            "reason": "reason",
	            "complaintAuthorName": "venik6",
	            "userName": "venik3"
	        }
	    ],
	    "last": false,
	    "totalPages": 2,
	    "totalElements": 3,
	    "first": true,
	    "numberOfElements": 2
    }
    ```

- **Коды ответа**:
    - `200 OK`: Успешно получен список жалоб.
    - `400 Bad Request`: Если токен отсутствует или недействителен.
    - `403 Forbidden`: Если у пользователя недостаточно прав для просмотра жалоб.
    - `500 Internal Server Error`: Если произошла ошибка во время процесса получения.

---

#### **4. POST** `/card/add`

- **Описание**: Добавить новое объявление.
- **Тело запроса**:
    - `cardDto` — Содержит детали об объявлении (название, текст).
    - `files` — Список изображений для загрузки и прикрепления к объявлению.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Ответ**:
    - `200 OK`: Успешно создано новое объявление.
    - `400 Bad Request`: Если входные данные недействительны (например, слишком много изображений, отсутствуют обязательные поля).
    - `500 Internal Server Error`: Если возникла проблема во время процесса создания объявления.

---

#### **5. POST** `/complaint/create`

- **Описание**: Подать жалобу на конкретного пользователя или объявление.
- **Тело запроса**:
    - `complaintDto` — Детали жалобы.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Поддерживаемые типы**: `multipart/form-data`
- **Тело запроса**:
```json
{
    "targetType":"USER или CARD", 
    "reason": "reason",
    "complaintTargetId":"Если вы жалуетесь на пользователя, введите ID пользователя. Если вы жалуетесь на объявление, введите ID объявления."
}
```   
- **Коды ответа**:
    - `200 OK`: Жалоба успешно подана.
    - `400 Bad Request`: Если токен отсутствует или недействителен.
    - `404 Not Found`: Если пользователь или карточка, на которые подана жалоба, не найдены.

---

#### **6. PATCH** `/card/{id}/patch`

- **Описание**: Обновление существующего объявления.
- **Ограничения**:
	- max-request-size: 30MB
	- max-file-size: 6MB
	- card-images-count: 6
- **Path Parameter**:
    - `id` — Уникальный идентификатор объявления для обновления.
- **Request Body**:
    - `cardDto` (Необязательно) — Новые данные для объявления (заголовок, текст).
    - `files` (Необязательно) — Список новых изображений для прикрепления.
- **Request Header**:
    - `Authorization` — JWT токен для аутентификации.
- **Response**:
    - `200 OK`: Объявление успешно обновлено.
    - `404 Not Found`: Если объявление с указанным ID не найдено.
    - `403 Forbidden`: Если у пользователя нет прав на изменение объявления.
    - `500 Internal Server Error`: В случае ошибки во время обновления объявления.

---

#### **7. DELETE** `/card/del/{id}`

- **Описание**: Удаление объявления по его уникальному ID.
- **Path Parameter**:
    - `id` — Уникальный идентификатор объявления для удаления.
- **Request Header**:
    - `Authorization` — JWT токен для аутентификации.
- **Response**:
    - `200 OK`: Объявление успешно удалено.
    - `404 Not Found`: Если объявление с указанным ID не найдено.
    - `500 Internal Server Error`: В случае ошибки во время процесса удаления.

---

#### **8. DELETE** `/card/image/del/{cardId}/{imageId}`

- **Описание**: Удаление конкретного изображения из объявления.
- **Path Parameters**:
    - `cardId` — ID объявления.
    - `imageId` — ID изображения для удаления.
- **Request Header**:
    - `Authorization` — JWT токен для аутентификации.
- **Response**:
    - `200 OK`: Изображение успешно удалено из объявления.
    - `404 Not Found`: Если объявление или изображение с указанным ID не найдено.
    - `500 Internal Server Error`: В случае ошибки во время процесса удаления.

---

#### **9. DELETE** `/complaint/delOne/{complaintId}`

- **Описание**: Удаление конкретной жалобы по её уникальному ID.
- **Path Parameter**:
    - `complaintId` — Уникальный идентификатор жалобы для удаления.
- **Request Header**:
    - `Authorization` — JWT токен для аутентификации.
- **Response Codes**:
    - `200 OK`: Жалоба успешно удалена.
    - `400 Bad Request`: Если токен отсутствует или недействителен.
    - `404 Not Found`: Если владелец токена не существует.
    - `403 Forbidden`: Если у пользователя нет прав на удаление жалобы.

---

#### **10. GET** `/card/search`

- **Описание**: Поиск нужных объявлений по запросу.
- **Path Parameter**:
  - `limit` (необязательно, по умолчанию: 5) — Максимальное количество жалоб для возврата.
  - `page` (необязательно, по умолчанию: 0) — Номер страницы для пагинации.
  - `query` — Информация, которую нужно найти.
  - `createTime` (необязательно) — фильтр поиска от этой даты.
- **Request Header**:
    - `Authorization` — JWT токен для аутентификации.
- **Пример ответа**:
    ```json
	{
	    "cards": [
	        {
	            "id": 3,
	            "title": "1-я карточка",
	            "text": "описание 1-й карточки",
	            "createTime": "2024-11-01",
	            "images": [
	                {
	                    "id": 9,
	                    "imageBucket": "images",
	                    "imageName": "01427c90-c59f-4f51-9792-83520bd335e6-R.jpg"
	                },
	                {
	                    "id": 10,
	                    "imageBucket": "images",
	                    "imageName": "01427c90-c59f-4f51-9792-83520bd335e6-ojpu5betwgy0zqsnlq87xhouqtiydlwk.jpg"
	                },
	                {
	                    "id": 11,
	                    "imageBucket": "images",
	                    "imageName": "01427c90-c59f-4f51-9792-83520bd335e6-ojpu5betwgy0zqsnlq87xhouqtiydlwk (1).jpg"
	                },
	                {
	                    "id": 12,
	                    "imageBucket": "images",
	                    "imageName": "01427c90-c59f-4f51-9792-83520bd335e6-R (1).jpg"
	                }
	            ],
	            "authorName": "venik6"
	        }
	    ],
	    "last": true,
	    "totalPages": 1,
	    "totalElements": 1,
	    "first": true,
	    "numberOfElements": 1
	}
    ```
    
- **Response Codes**:
    - `200 OK`: Успешное получение данных.
    - `401 Unauthorized`: Если токен недействителен.
    - `500 Internal Server Error`: В случае ошибки во время удаления.
</details>
 
---

### Микросервис Аутентификации
Управляет аутентификацией и авторизацией пользователей.
<details>
  <summary><strong>Эндпоинты:</strong></summary>
	
#### **1. POST** `/auth/register`

- **Описание**: Регистрация нового пользователя.
- **Тело запроса**:
```json
{
  "name": "johndoe123456789",
  "password": "securePassword!2024",
  "email": "johndoe@example.com",
  "role": ["USER","ADMIN"],
  "firstName": "John",
  "lastName": "Doe",
  "description": "Увлеченный разработчик с опытом в Java и микросервисах.",
  "country": "Беларусь",
  "roleInCommand": "Ведущий разработчик",
  "skills": "Java, Spring Boot, Microservices, Docker, Kubernetes"
}
```   
- **Коды ответа**:
    - `200 OK`: Пользователь успешно зарегистрирован.
    - `400 Bad Request`: Если пользователь уже существует.

---

#### **2. POST** `/auth/login`

- **Описание**: Аутентификация пользователя и генерация access и refresh токенов.
- **Тело запроса**:
```json
{
  "username": "johndoe123456789",
  "password": "securePassword!2024"
}
```   
- **Коды ответа**:
    - `200 OK`: Возвращает access и refresh токены:
      ```json
      {
        "jwtToken": "token",
        "refreshToken": "token"
      }
      ```
    - `401 Unauthorized`: Если имя пользователя или пароль неверны.

---

#### **3. POST** `/auth/refresh-token`

- **Описание**: Обновление access токена.
- **Тело запроса**:
```json
{
    "refreshToken": "token"
}
```   
- **Коды ответа**:
    - `200 OK`: Возвращает новый access токен:
      ```json
      {
        "accessToken": "token"
      }
      ```
    - `400 Bad Request`: Если refresh токен равен `null`.

---

#### **4. GET** `/auth/validate`

- **Описание**: Проверка валидности access токена.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Возвращает результат валидации:
      ```json
      {
        "valid": true
      }
      ```
    - `401 Unauthorized`: Если токен недействителен.

---

#### **5. GET** `/user/profile/{userName}`

- **Описание**: Получение данных о пользователе.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Параметры запроса**:
    - `userName` — Имя пользователя, данные которого нужно получить.
- **Коды ответа**:
    - `200 OK`: Возвращает данные пользователя:
      ```json
      {
        "id": 7,
        "name": "johndoe123456789",
        "email": "johndoe@example.com",
        "role": [
            "USER",
            "ADMIN"
        ],
        "firstName": "John",
        "lastName": "Doe",
        "description": "Увлеченный разработчик с опытом в Java и микросервисах.",
        "country": "Беларусь",
        "roleInCommand": "Ведущий разработчик",
        "skills": "Java, Spring Boot, Microservices, Docker, Kubernetes"
      }
      ```
    - `500 Internal Server Error`: В случае ошибки сервера.

---

#### **6. PATCH** `/user/patch`

- **Описание**: Обновление выбранных полей аутентифицированного пользователя.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Параметры запроса** *(Все поля необязательны)*:
    - `name` — Новое имя пользователя. *(При изменении этого поля потребуется получить новый access токен)*
    - `email` — Новый адрес электронной почты.
    - `firstName` — Новое имя.
    - `lastName` — Новая фамилия.
    - `description` — Новое описание пользователя.
    - `country` — Новая страна проживания.
    - `roleInCommand` — Новая роль в команде/проекте.
    - `skills` — Новые навыки пользователя.
- **Коды ответа**:
    - `200 OK`: Возвращает сообщение об успешном обновлении.
    - `400 Bad Request`: Если запрос содержит некорректные данные или некоторые из необязательных параметров неверны.

---

#### **7. DELETE** `/user/del`

- **Описание**: Удаляет владельца токена.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Пользователь успешно удален.
    - `400 Bad Request`: Проблема с токеном.
    - `500 Internal Server Error`: Проблема с удалением пользователя на сервере.

---

#### **8. POST** `/user/toggle/favoriteCard/{cardId}`

- **Описание**: Добавляет карточку в избранное при первом обращении и удаляет её из избранного при повторном.
- **Параметр пути**:
    - `cardId` — Уникальный идентификатор карточки.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Успешное добавление или удаление карточки из избранного.
    - `400 Bad Request`: Если пользователь не найден.

---

#### **9. GET** `/user/favoriteCard/get`

- **Описание**: Получает список id избранных карточек текущего пользователя.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: возвращает id избранных объявлений.
      ```json
      [
	    2,
	    3
      ]
      ```
    - `400 Bad Request`: Если пользователь не найден.
    - `500 Internal Server Error`: Если произошла ошибка при обработке запроса.

---

#### **10. PATCH** `/user/block/{userName}`

- **Описание**: Блокирует пользователя с указанным именем пользователя.
- **Параметр пути**:
    - `userName` — Имя пользователя, которого нужно заблокировать.
- **Параметры запроса**:
    - `year`, `month`, `dayOfMonth`, `hours`, `minutes`, `seconds` — Дата, когда пользователь будет разблокирован.
    - `reason` — Причина блокировки.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Пользователь успешно заблокирован.
    - `400 Bad Request`: Если пользователь не найден.

---

#### **11. PATCH** `/user/unblock/{userName}`

- **Описание**: Разблокирует пользователя с указанным именем пользователя.
- **Параметр пути**:
    - `userName` — Имя пользователя, которого нужно разблокировать.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Пользователь успешно разблокирован.
    - `400 Bad Request`: Если пользователь не найден.

---

#### **12. PATCH** `/user/toggle/authorities/{userName}`

- **Описание**: Добавляет роль ADMIN (если её не было) или удаляет её (если она была).
- **Параметр пути**:
    - `userName` — Имя пользователя, для которого нужно изменить роли.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Роли пользователя успешно изменены.
    - `400 Bad Request`: Если пользователь не найден или недостаточно прав для изменения ролей.
</details>

---

### Микросервис Комментариев
Отвечает за управление комментариями.
<details>
  <summary><strong>Эндпоинты:</strong></summary>
	
#### **1. GET** `/comment/get/{cardId}`

- **Описание**: Получить все комментарии к этому объявлению.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
 - **Параметры пути**:
    - `pageNumber` — Номер страницы для получения.
    - `limit` — Количество объявлений на странице.
    - `cardId` - ID объявления.
- **Коды ответа**:
    - `200 OK`: Возвращает комментарии к объявлению:
      ```json
		[
		    {
			"commentId": 1,
			"text": "СУПЕР",
			"createdAt": "2024-10-26T20:52:21.048719",
			"commentAuthorDto": {
			    "name": "venik5"
			},
			"replyQuantity": 0
		    },
		    {
			"commentId": 2,
			"text": "СУПЕР",
			"createdAt": "2024-10-26T20:52:22.74424",
			"commentAuthorDto": {
			    "name": "venik5"
			},
			"replyQuantity": 2
		    },
		    {
			"commentId": 3,
			"text": "СУПЕР",
			"createdAt": "2024-10-26T20:52:24.309763",
			"commentAuthorDto": {
			    "name": "venik5"
			},
			"replyQuantity": 1
		    }
		]
      ```
    - `400 Bad Request`: Если объявление не найдено или возникла непредвиденная ошибка.
    - `401 Unauthorized`: Если токен недействителен или пользователь не найден.
    - `500 Internal Server Error`: Если произошла внутренняя ошибка.

---

#### **2. POST** `comment/add/{cardId}`

- **Описание**: Добавить новый комментарий к конкретному объявлению по его ID.
- **Параметр пути**:
    - `cardId`: ID объявления, к которому будет добавлен комментарий.
- **Тело запроса**:
    - `commentDto`: Содержит текст комментария:
    ```json
    {
      "text": "Это комментарий"
    }
    ```
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Если комментарий успешно добавлен.
    - `401 Unauthorized`: Если токен не существует или недействителен.
    - `400 Bad Request`: Если объявление не найдено, данные некорректны или произошла неизвестная ошибка.

---

#### **3. DELETE** `/comment/del/{commentId}`

- **Описание**: Удалить конкретный комментарий по его уникальному ID.
- **Параметр пути**:
    - `commentId` — Уникальный идентификатор удаляемого комментария.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Комментарий успешно удален.
    - `400 Bad Request`: Если комментарий не найден или у пользователя недостаточно прав.
    - `401 Unauthorized`: Если предоставленный токен не существует или недействителен.
    - `500 Internal Server Error`: Если произошла внутренняя ошибка при удалении комментария.

---

#### **4. PATCH** `/comment/{commentId}/patch`

- **Описание**: Обновить (отредактировать) существующий комментарий.
- **Параметр пути**:
    - `commentId` — Уникальный идентификатор редактируемого комментария.
- **Тело запроса**:
    - `commentDto` — Содержит новые данные для редактирования комментария:
    ```json
    {
      "text": "Это обновленный комментарий"
    }
    ```
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Комментарий успешно обновлен.
    - `400 Bad Request`: Если комментарий не найден или данные некорректны.
    - `401 Unauthorized`: Если предоставленный токен не существует или недействителен.
    - `500 Internal Server Error`: Если произошла неизвестная ошибка во время обновления.

---

#### **5. POST** `/comment/reply/{commentId}`

- **Описание**: Добавить ответ к конкретному комментарию по его ID.
- **Параметр пути**:
    - `commentId` — ID комментария, к которому будет добавлен ответ.
- **Тело запроса**:
    - `commentDto`: Содержит текст ответа:
    ```json
    {
      "text": "Это ответ"
    }
    ```
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Если ответ успешно добавлен.
    - `401 Unauthorized`: Если токен не существует или недействителен.
    - `500 Internal Server Error`: Если комментарий не найден, данные некорректны или произошла неизвестная ошибка.

---

#### **6. GET** `/comment/reply/get/{commentId}`

- **Описание**: Получить все ответы на конкретный комментарий по его ID.
- **Параметр пути**:
    - `commentId` — ID комментария, для которого запрашиваются ответы.
- **Параметры запроса**:
    - `page` — Номер страницы для получения (опционально, по умолчанию 0).
    - `limit` — Количество ответов на странице (опционально, по умолчанию 10).
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Возвращает список ответов на указанный комментарий:
      ```json
		[
		    {
		        "commentId": 4,
		        "text": "СУПЕР",
		        "createdAt": "2024-10-26T20:54:09.6585",
		        "commentAuthorDto": {
		            "name": "venik5"
		        },
		        "replyQuantity": 0
		    },
		    {
		        "commentId": 5,
		        "text": "СУПЕР",
		        "createdAt": "2024-10-26T20:54:10.891731",
		        "commentAuthorDto": {
		            "name": "venik5"
		        },
		        "replyQuantity": 0
		    },
		    {
		        "commentId": 6,
		        "text": "СУПЕР",
		        "createdAt": "2024-10-26T20:54:11.652032",
		        "commentAuthorDto": {
		            "name": "venik5"
		        },
		        "replyQuantity": 0
		    }
		]
      ```
    - `401 Unauthorized`: Если токен недействителен или пользователь не найден.
    - `400 Bad Request`: Если комментарий не найден.
    - `500 Internal Server Error`: Если произошла внутренняя ошибка.
</details>

---

### Микросервис Изображений
Необходим для работы с изображениями
<details>
  <summary><strong>Эндпоинты:</strong></summary>
	
#### **1. POST** `/image/addProfileImage`

- **Описание**: Добавляет изображение в профиль.
- **Content-Type**: multipart/form-data
- **Параметр пути**:
    - `cardId`: Идентификатор карточки, к которой будет добавлен комментарий.
- **Тело запроса**:
    - `profileImage`: Изображение для добавления.
- **Заголовок запроса**:
    - `Authorization` — JWT токен для аутентификации.
- **Коды ответа**:
    - `200 OK`: Если изображение профиля успешно добавлено.
    - `401 Unauthorized`: Если токен отсутствует или недействителен.
    - `500 Internal Server Error`: Если произошла внутренняя ошибка при сохранении изображения или любая другая неожиданная ошибка.
</details>