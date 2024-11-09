FROM cgr.dev/chainguard/wolfi-base:latest

EXPOSE 8080

COPY ./build/libs/cirro-agent-0.1-all.jar /app/application

ENTRYPOINT ["/app/application"]
