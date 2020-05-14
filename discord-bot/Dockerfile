FROM hypriot/rpi-java:jre-1.8.111

ENV PORT 8092
EXPOSE ${PORT}

ENV CONFIGURATION_FOLDER .
ENV CONFIGURATION_FILENAME discord.yml

COPY build/libs/discord-bot-0.0.1-SNAPSHOT.jar discord-bot.jar

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "--Dspring.config.location=${CONFIGURATION_FOLDER}/${CONFIGURATION_FILENAME}", "-jar", "discord-bot.jar"]
