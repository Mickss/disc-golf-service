# Disc Golf Service

This the main service for managing Disc Golf sports events.

## Application architecture

This application is part of microservices. It is running with Axion services:

 - Axion API gateway: https://github.com/Mickss/axion-api-gateway
 - Axion auth service: https://github.com/Mickss/axion-auth-service
 - Axion email service: https://github.com/Mickss/axion-mail-service

Ui is build with React application: https://github.com/Mickss/disc-golf-ui
   
## Running application

To run the application, profile has to be set up in start configuration:
- add Environment variable: `spring.profiles.active=dev`
