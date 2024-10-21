# Overview

The platform enables users to create announcements for seeking teams for their projects or to join existing ones. Its purpose is to help users find opportunities to gain teamwork experience and enhance their skills.

Anyone can participate and borrow code, but it's important to respect the license terms.

# Microservices

### Ads Microservice
Manages announcements and their content
### Endpoints:

#### **1. GET** `/card/{id}/get`

- **Description**: Retrieve an ad by its unique ID.
- **Path Parameter**:
    - `id` — The unique identifier of the ad.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Sample Response**:
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
    
- **Response Codes**:
    - `200 OK`: Successful retrieval.
    - `404 Not Found`: If the ad with the given ID does not exist.

---

#### **2. GET** `/card/getAll/{pageNumber}/{limit}`

- **Description**: Retrieve all ads with pagination support.
- **Path Parameters**:
    - `pageNumber` — The page number to retrieve.
    - `limit` — The number of ads per page.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Sample Response**:
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
    
- **Response Codes**:
    - `200 OK`: Successful retrieval of paginated results.
    - `400 Bad Request`: If pagination parameters are invalid.

---

#### **3. POST** `/card/add`

- **Description**: Add a new ad.
- **Request Body**:
    - `cardDto` — Contains details about the ad (title, text, etc.).
    - `files` — List of images to be uploaded and attached to the ad.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response**:
    - `200 OK`: Successfully created a new ad.
    - `400 Bad Request`: If the input data is invalid (e.g., too many images, missing fields).
    - `500 Internal Server Error`: If there is an issue during the ad creation process.

---

#### **4. PATCH** `/card/{id}/patch`

- **Description**: Update an existing ad.
- **Limitations**:
	- max-request-size: 30MB
	- max-file-size: 6MB
	- card-images-count: 6
- **Path Parameter**:
    - `id` — The unique identifier of the ad to be updated.
- **Request Body**:
    - `cardDto` (Optional) — The new data for the ad (title, text).
    - `files` (Optional) — The list of new images to attach.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response**:
    - `200 OK`: Successfully updated the ad.
    - `404 Not Found`: If the ad with the given ID does not exist.
    - `403 Forbidden`: If the user does not have permission to modify the ad.
    - `500 Internal Server Error`: If there is an issue during the ad update process.

---

#### **5. DELETE** `/card/del/{id}`

- **Description**: Delete an ad by its unique ID.
- **Path Parameter**:
    - `id` — The unique identifier of the ad to be deleted.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response**:
    - `200 OK`: Successfully deleted the ad.
    - `404 Not Found`: If the ad with the given ID does not exist.
    - `500 Internal Server Error`: If there is a failure in the deletion process.

---

#### **6. DELETE** `/card/image/del/{cardId}/{imageId}`

- **Description**: Delete a specific image from an ad.
- **Path Parameters**:
    - `cardId` — The ID of the ad.
    - `imageId` — The ID of the image to be deleted.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response**:
    - `200 OK`: Successfully deleted the image from the ad.
    - `404 Not Found`: If the ad or image with the given ID does not exist.
    - `500 Internal Server Error`: If there is a failure during the deletion process.

---

### Authentication Microservice
Manages user authentication and authorization
### Endpoints:
#### **1. POST** `/auth/register`

- **Description**: Register a new user.
- **Request body**:
```json
{
  "name": "johndoe123456789",
  "password": "securePassword!2024",
  "email": "johndoe@example.com",
  "role": ["USER","ADMIN"],
  "firstName": "John",
  "lastName": "Doe",
  "description": "A passionate developer with experience in Java and microservices.",
  "country": "Belarus",
  "roleInCommand": "Lead Developer",
  "skills": "Java, Spring Boot, Microservices, Docker, Kubernetes"
}
```   
- **Response Codes**:
    - `200 OK`: User registered successfully.
    - `400 Bad Request`: If the user already exists.

---

#### **2. POST** `/auth/login`

- **Description**: Authenticate a user and generate access and refresh tokens.
- **Request body**:
```json
{
  "username": "johndoe123456789",
  "password": "securePassword!2024"
}
```   
- **Response Codes**:
    - `200 OK`: Returns access and refresh tokens:
      ```json
      {
        "jwtToken": "token",
        "refreshToken": "token"
      }
      ```
    - `401 Unauthorized`: If the username or password is incorrect.

---

#### **3. POST** `/auth/refresh-token`

- **Description**: Refresh access token.
- **Request body**:
```json
{
    "refreshToken": "token"
}

```   
- **Response Codes**:
    - `200 OK`: Returns a new access token:
      ```json
      {
        "accessToken": "token"
      }
      ```
    - `400 Bad Request`: If the refresh token is `null`.

---

#### **4. GET** `/auth/validate`

- **Description**: Validate the access token.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: Returns the validation result:
      ```json
      {
        "valid": true
      }

      ```
    - `401 Unauthorized`: If the token is invalid.

---

#### **5. GET** `/user/profile/{userName}`

- **Description**: Retrieve user details.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Request Parameters**:
    - `userName` — Name of the user whose data you want to retrieve.
- **Response Codes**:
    - `200 OK`: Returns user details:
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
        "description": "A passionate developer with experience in Java and microservices.",
        "country": "Belarus",
        "roleInCommand": "Lead Developer",
        "skills": "Java, Spring Boot, Microservices, Docker, Kubernetes"
      }
      ```
    - `500 Internal Server Error`: If there is a server error.

---

#### **6. PATCH** `/user/patch`

- **Description**: Update selected fields of the authenticated user.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Request Parameters** *(All fields are optional)*:
    - `name` — New user name. *(If you change this field, you will have to take a new access token)*
    - `email` — New email address.
    - `firstName` — New first name.
    - `lastName` — New last name.
    - `description` — New description for the user.
    - `country` — New country of residence.
    - `roleInCommand` — New role within the command/project.
    - `skills` — New skills of the user.
- **Response Codes**:
    - `200 OK`: Returns success message.
    - `400 Bad Request`: If the request contains invalid data or some of the optional parameters are incorrect.

---

#### **7. DELETE** `/user/del`

- **Description**: Deletes the token owner.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: User deleted successfully.
    - `400 Bad Request`: Trouble with the token.
    - `500 Internal Server Error`: Problems with deleting a user on the server side.

---

### Comment Microservice
Responsible for comment management
### Endpoints:
#### **1. GET** `/comment/get/{cardId}`

- **Description**: Receive all comments on this ad.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
 - **Path Parameters**:
    - `pageNumber` — The page number to retrieve.
    - `limit` — The number of ads per page.
    - `cardId` - ID of the ad.
- **Response Codes**:
    - `200 OK`: Returns card comments:
      ```json
      [
          {
              "id": 29,
              "text": "СУПЕР",
              "createdAt": "2024-10-13T14:28:15.921464",
              "userId": 8
          },
          {
              "id": 30,
              "text": "СУПЕР",
              "createdAt": "2024-10-13T14:28:16.843091",
              "userId": 8
          },
          {
              "id": 31,
              "text": "СУПЕР",
              "createdAt": "2024-10-13T14:28:17.5211",
              "userId": 8
          },
          {
              "id": 32,
              "text": "СУПЕР",
              "createdAt": "2024-10-13T14:28:18.143982",
              "userId": 8
          },
          {
              "id": 33,
              "text": "СУПЕР",
              "createdAt": "2024-10-13T14:28:18.719853",
              "userId": 8
          }
      ]
      ```
    - `400 Bad Request`: If the ad was not found or there was an unexpected error.
    - `401 Unauthorized`: If the token is invalid or user not found.
    - `500 Internal Server Error`: If an internal error occurs.

---

#### **2. POST** `comment/add/{cardId}`

- **Description**: Add a new comment to a specific card by its ID.
- **Path Parameter**:
    - `cardId`: The id of the card to which the comment will be added.
- **Request Body**:
    - `commentDto`: Contains the text of the comment to be added:
    ```json
    {
      "text": "This is a comment"
    }
    ```
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: If the comment is added successfully.
    - `401 Unauthorized`: If the token does not exist or is invalid.
    - `400 Bad Request`: If the card is not found, if the entered data is incorrect, or if an unknown error occurs.

---

#### 3. DELETE `/comment/del/{commentId}`

- **Description**: Delete a specific comment by its unique ID.
- **Path Parameter**:
    - `commentId` — The unique identifier of the comment to be deleted.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: Successfully deleted the comment.
    - `400 Bad Request`: If the comment was not found or the user does not have enough permissions.
    - `401 Unauthorized`: If the provided token does not exist or is invalid.
    - `500 Internal Server Error`: If there is an internal error when deleting the comment.

---

#### 4. PATCH `/comment/{commentId}/patch`

- **Description**: Patch (update) an existing comment.
- **Path Parameter**:
    - `commentId` — The unique identifier of the comment to be patched.
- **Request Body**:
    - `commentDto` — Contains the new data to patch the comment:
    ```json
    {
      "text": "This is a comment"
    }
    ```
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: Successfully patched the comment.
    - `400 Bad Request`: If the comment is not found or if the provided data is invalid.
    - `401 Unauthorized`: If the provided token does not exist or is invalid.
    - `500 Internal Server Error`: If there is an unknown error during the patching process.

---

### Image Microservice
Necessary for working with images
### Endpoints:
#### **1. POST** `/image/addProfileImage`

- **Description**: Adds an image to a profile.
- **Content-Type**: multipart/form-data
- **Path Parameter**:
    - `cardId`: The id of the card to which the comment will be added.
- **Request Body**:
    - `profileImage`: Image to add.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: If the profile picture has been successfully added.
    - `401 Unauthorized`: If the token does not exist or is invalid.
    - `500 Internal Server Error`: If there was an internal problem with saving the image or any other unexpected error.
