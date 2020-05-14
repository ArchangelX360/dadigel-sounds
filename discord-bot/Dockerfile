FROM hypriot/rpi-java:jre-1.8.111

ENV PORT 8092
EXPOSE ${PORT}

ENV CONFIGURATION_FILENAME discord.yml

COPY build/libs/discord-bot-0.0.1-SNAPSHOT.jar /opt/discord-bot.jar

ENTRYPOINT [
    "java",
    "-Dserver.port=${PORT}",
    "-jar",
    "/opt/discord-bot.jar",
    "--spring.config.name=${CONFIGURATION_FILENAME}"
]
