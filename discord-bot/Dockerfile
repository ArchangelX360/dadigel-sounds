FROM hypriot/rpi-java:jre-1.8.111

ENV PORT 8092
EXPOSE ${PORT}

COPY build/libs/discord-bot-0.0.1-SNAPSHOT.jar discord-bot.jar

ENTRYPOINT ['java', '-Dserver.port=${PORT}', '-jar', 'discord-bot.jar']
