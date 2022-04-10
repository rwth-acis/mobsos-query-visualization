FROM openjdk:17-jdk-alpine

ENV HTTP_PORT=8080
ENV HTTPS_PORT=8443
ENV LAS2PEER_PORT=9011

RUN apk add --update bash mysql-client dos2unix curl && rm -f /var/cache/apk/*
RUN addgroup -g 1000 -S las2peer && \
    adduser -u 1000 -S las2peer -G las2peer

COPY --chown=las2peer:las2peer . /src
WORKDIR /src

# run the rest as unprivileged user
USER las2peer
RUN dos2unix ./gradlew
RUN dos2unix /src/gradle.properties
RUN chmod +x ./gradlew && ./gradlew build
RUN dos2unix /src/etc/i5.las2peer.services.mobsos.queryVisualization.QueryVisualizationService.properties
RUN dos2unix /src/docker-entrypoint.sh

EXPOSE $HTTP_PORT
EXPOSE $HTTPS_PORT
EXPOSE $LAS2PEER_PORT
ENTRYPOINT ["/src/docker-entrypoint.sh"]
