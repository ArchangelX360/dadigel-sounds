FROM docker.pkg.github.com/archangelx360/dadigel-sounds/bot-runtime:latest

EXPOSE 8092

COPY build/libs/discord-bot-0.0.1-SNAPSHOT.jar /opt/discord-bot.jar

ENTRYPOINT ["java", "-Dserver.port=8092", "-jar", "/opt/discord-bot.jar"]
