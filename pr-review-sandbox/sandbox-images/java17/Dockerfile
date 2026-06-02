FROM eclipse-temurin:17-jdk-alpine

RUN addgroup -S sandbox && adduser -S sandbox -G sandbox && \
    mkdir -p /sandbox/code && chown -R sandbox:sandbox /sandbox

WORKDIR /sandbox
COPY compile-run.sh /sandbox/compile-run.sh
RUN chmod +x /sandbox/compile-run.sh

USER sandbox

ENTRYPOINT ["/sandbox/compile-run.sh"]
