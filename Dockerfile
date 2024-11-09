FROM cgr.dev/chainguard/wolfi-base:latest

EXPOSE 8080

COPY ./build/native/nativeCompile/cirro-agent /app/application

ENTRYPOINT ["/app/application"]
