# Overview

The platform enables users to create announcements for seeking teams for their projects or to join existing ones. Its purpose is to help users find opportunities to gain teamwork experience and enhance their skills.

Anyone can participate and borrow code, but it's important to respect the license terms.

<details>
  <summary><strong>Currently Implemented:</strong></summary>

  ### Ad Management
  - **Create, edit, and delete ads.**
  - **View ad listings.**
  - **Add or remove ads from favorites.**
  - **Integration with elastic search engine.**

  ### Comment and Reply Management
  - **Create, edit, and delete comments on ads.**
  - **Add and manage reply comments.**

  ### User Profile and Account Management
  - **User registration and login.**
  - **Edit user profile information.**
  - **Update profile avatar.**
  - **View user profile.**
  - **Delete user account.**

  ### Access Control and Security
  - **Access token validation.**
  - **Issue a new access token using a refresh token.**

  ### Admin Tools and Complaint Management
  - **Submit complaints about users or ads.**
    - **Admin privileges include:**
      - Viewing the list of complaints.
      - Removing complaints.
  - **User Moderation:**
    - Ban or unban users.
    - Adjust user roles (promote or demote).
</details>

---
### Settings

| Environment           | Description                                                                                                |
|:---------------------:|:----------------------------------------------------------------------------------------------------------:|
| X_API_KEY             |   The key for connecting internal APIs to each other, which should not be accessible to an outsider        |
| ELASTIC_PASSWORD      |   The password from your elasticSearch user                                                                |
| POSTGRES_DB           |   The name of your database                                                                                |
| POSTGRES_USER         |   Login from your postgres                                                                                 |
| POSTGRES_PASSWORD     |   The password for your postgres                                                                           |
| MINIO_ROOT_USER       |   Login from minIO                                                                                         |
| MINIO_ROOT_PASSWORD   |   The password for your minIO                                                                              |

---

# Microservices

### Ads Microservice
Manages announcements and their content, including user complaints
<details>
  <summary><strong>Endpoints:</strong></summary>

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

#### **3. GET** `/complaint/get`

- **Description**: Retrieve a list of complaints with optional filtering.
- **Query Parameters**:
    - `limit` (optional, default: 5) — The maximum number of complaints to return.
    - `page` (optional, default: 0) — The page number for pagination.
    - `complaintType` (optional, default: all) — The type of complaint to filter by:
        - `card` — Retrieve complaints related to ads.
        - `user` — Retrieve complaints related to users.
        - If not specified or invalid, it will return all complaints.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Sample Response**:
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

- **Response Codes**:
    - `200 OK`: Successfully retrieved the list of complaints.
    - `400 Bad Request`: If the token is missing or invalid.
    - `403 Forbidden`: If the user does not have sufficient permissions to view complaints.
    - `500 Internal Server Error`: If there is a failure during the retrieval process.

---

#### **4. POST** `/card/add`

- **Description**: Add a new ad.
- **Request Body**:
    - `cardDto` — Contains details about the ad (title, text).
    - `files` — List of images to be uploaded and attached to the ad.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response**:
    - `200 OK`: Successfully created a new ad.
    - `400 Bad Request`: If the input data is invalid (e.g., too many images, missing fields).
    - `500 Internal Server Error`: If there is an issue during the ad creation process.

---

#### **5. POST** `/complaint/create`

- **Description**: Submit a complaint about a specific user or ad.
- **Request Body**:
    - `complaintDto` — The complaint details.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Consumes**: `multipart/form-data`
- **Request body**:
```json
{
    "targetType":"USER or CARD", 
    "reason": "reason",
    "complaintTargetId":"If you are complaining about a user, enter the user ID. If you are complaining about an advertisement, enter the advertisement ID."
}
```   
- **Response Codes**:
    - `200 OK`: Complaint successfully submitted.
    - `400 Bad Request`: If the token is missing or invalid.
    - `404 Not Found`: If the user or card related to the complaint is not found.

---

#### **6. PATCH** `/card/{id}/patch`

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

#### **7. DELETE** `/card/del/{id}`

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

#### **8. DELETE** `/card/image/del/{cardId}/{imageId}`

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

#### **9. DELETE** `/complaint/delOne/{complaintId}`

- **Description**: Delete a specific complaint by its unique ID.
- **Path Parameter**:
    - `complaintId` — The unique identifier of the complaint to be deleted.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: Successfully deleted the complaint.
    - `400 Bad Request`: If the token is missing or invalid.
    - `404 Not Found`: If the token owner does not exist.
    - `403 Forbidden`: If the user does not have permission to delete the complaint.

---

#### **10. GET** `/card/search`

- **Description**: Search for desired maps by query.
- **Path Parameter**:
  - `limit` (optional, default: 5) — The maximum number of complaints to return.
  - `page` (optional, default: 0) — The page number for pagination.
  - `query` — Information you need.
  - `createTime` (optional) — search filter from this date.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Sample Response**:
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
    - `200 OK`: Successful retrieval.
    - `401 Unauthorized`: If the token is invalid.
    - `500 Internal Server Error`: If there is a failure during the deletion process.
</details>

---

### Authentication Microservice
Manages user authentication and authorization
<details>
  <summary><strong>Endpoints:</strong></summary>
	
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

#### **8. POST** `/user/toggle/favoriteCard/{cardId}`

- **Description**: Adds a card to your favorites the first time you access it and removes it from there the second time you access it..
- **Path Parameter**:
    - `cardId` — Unique identifier of the card.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: Successfully added or removed the card from favorites.
    - `400 Bad Request`: If the user is not found.

---

#### **9. GET** `/user/favoriteCard/get`

- **Description**: Retrieves the list of favorite cards for the current user.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: returns the id of your favorite ads.
      ```json
      [
	    2,
	    3
      ]
      ```
    - `400 Bad Request`: If the user is not found.
    - `500 Internal Server Error`: If there is an error processing the request.

---

#### **10. PATCH** `/user/block/{userName}`

- **Description**: Blocks the user with the specified username.
- **Path Parameter**:
    - `userName` — The username of the user to be blocked.
- **Request Parameters**:
    - `year`, `month`, `dayOfMonth`, `hours`, `minutes`, `seconds` — Date when the user will be unlocked.
    - `reason` — Reason for blocking.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: User successfully blocked.
    - `400 Bad Request`: If the user is not found.

---

#### **11. PATCH** `/user/unblock/{userName}`

- **Description**: Unblocks the user with the specified username.
- **Path Parameter**:
    - `userName` — The username of the user to be unblocked.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: User successfully unblocked.
    - `400 Bad Request`: If the user is not found.

---

#### **12. PATCH** `/user/toggle/authorities/{userName}`

- **Description**: Adds the ADMIN role (if there was none) or deletes it (if there was).
- **Path Parameter**:
    - `userName` — The username for which to change roles.
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: Successfully toggled the user's roles.
    - `400 Bad Request`: If the user is not found or if there are insufficient permissions to change roles.
</details>

---

### Comment Microservice
Responsible for comment management
<details>
  <summary><strong>Endpoints:</strong></summary>
	
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

#### **5. POST** `/comment/reply/{commentId}`

- **Description**: Add a reply to a specific comment by its ID.
- **Path Parameter**:
    - `commentId` — The ID of the comment to which the reply will be added.
- **Request Body**:
    - `commentDto`: Contains the text of the reply to be added:
    ```json
    {
      "text": "This is a reply"
    }
    ```
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: If the reply is added successfully.
    - `401 Unauthorized`: If the token does not exist or is invalid.
    - `500 Internal Server Error`: If the comment is not found, if the entered data is incorrect, or if an unknown error occurs.

---

#### **6. GET** `/comment/reply/get/{commentId}`

- **Description**: Retrieve all replies to a specific comment by its ID.
- **Path Parameter**:
    - `commentId` — The ID of the comment for which replies are being retrieved.
- **Request Parameters**:
    - `page` — The page number to retrieve (optional, default is 0).
    - `limit` — The number of replies per page (optional, default is 10).
- **Request Header**:
    - `Authorization` — The JWT token for authentication.
- **Response Codes**:
    - `200 OK`: Returns a list of replies to the specified comment:
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
    - `401 Unauthorized`: If the token is invalid or user not found.
    - `400 Bad Request`: If the comment is not found.
    - `500 Internal Server Error`: If an internal error occurs.
</details>

---

### Image Microservice
Necessary for working with images
<details>
  <summary><strong>Endpoints:</strong></summary>
	
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
</details>

