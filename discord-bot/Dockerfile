FROM hypriot/rpi-java:jre-1.8.111

EXPOSE 8092

COPY build/libs/discord-bot-0.0.1-SNAPSHOT.jar discord-bot.jar

ENTRYPOINT ["java", "-Dserver.port=8092", "-Dspring.config.location=/opt/config/discord.yml", "-jar", "discord-bot.jar"]
