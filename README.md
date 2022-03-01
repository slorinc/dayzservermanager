# dayzservermanager
Projects aims to help DayZ Dedicated Server admins run their servers

## Current features
* Battleye RCON connection
* REST endpoint to expose Player Count
* Update Discord on schedule with the Player Count

## Installation
* Edit `src/main/resources/application.yaml` and add your RCON and Discord Bot credentials
* Install Java 11 JRE or JDK (eg: `chocolatey.exe install microsoft-openjdk`)
* Run DayZ Server Manager with `./gradlew :bootRun`

## Resources
* API Documentation: http://localhost:8080/swagger-ui.html
* BE RCON protocol desctiption: https://www.battleye.com/downloads/BERConProtocol.txt
