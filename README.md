[![Build Status](https://travis-ci.org/Web-tree/mystuff.svg?branch=master)](https://travis-ci.org/Web-tree/mystuff)

# Build
## Backend (back subfolder)
### Maven
* Create jars
```
mnvw package
```
* Build docker image
```
mvnw dockerfile:build
``` 
## Frontend (*web subfolder*)
### Maven
* Build docker image
```
mvnw dockerfile:build
```
### Command line
* Preparations
    * Install npm ([instruction](https://www.npmjs.com/get-npm)).
    * Install angular cli. You can install it by execute command:
    ```
    npm install -g @angular/cli
    ```
* Build 
    * Installed npm modules
    ```
    npm i
    ```

    
# Run application
## Docker
### Web+back+neo4j
```
docker-compose up
```
## Backend
### From IDE
Run org.webtree.mystuff.boot.App
Require neo4j. You can [download it here](https://neo4j.com/download-center/). 
If you want to run App with embedded neo4j you should set property spring.data.neo4j.driver to org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver and spring.data.neo4j.uri as empty string. 
For more information about setting spring boot properties please read [spring documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)
## Frontend 
### Command line (require build from command line)
* For run just execute command
```
ng serve
```
* For run tests:
```
ng test
```

#
