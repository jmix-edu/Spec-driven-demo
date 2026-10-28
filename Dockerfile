# Runtime image for the booking application (spec 04_deployment/01).
# The JAR is built on the host first:
#   ./gradlew -Pvaadin.productionMode=true bootJar
# so no build tooling and no repository credentials ever enter the image.
FROM eclipse-temurin:21-jre

# non-root user; Jmix writes temp files and file storage under the working directory
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/.jmix/temp /app/.jmix/work \
    && chown -R app:app /app

WORKDIR /app
COPY --chown=app:app build/libs/booking-0.0.1-SNAPSHOT.jar /app/app.jar

USER app
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
