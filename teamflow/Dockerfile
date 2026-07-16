# ============================================================
#  MULTI-STAGE DOCKERFILE
#  Stage 1 ("build") compiles the app INSIDE a container that has
#  Maven + JDK 21, so you do NOT need Java or Maven on your laptop.
#  Stage 2 ("run") is a small image that only contains the finished
#  app + a Java runtime. Smaller final image = faster, safer deploys.
# ============================================================

# ---------- Stage 1: build the application ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the build file first and download dependencies. Docker caches this
# layer, so dependencies are only re-downloaded when pom.xml changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Now copy the source and build the runnable app (skip tests for a fast image).
COPY src ./src

# Generate a fresh RSA keypair for JWT signing. Keys are NEVER committed to
# git (see .gitignore) - each image gets its own pair, baked in at build time.
RUN mkdir -p src/main/resources/jwt \
 && openssl genrsa -out /tmp/rsa.pem 2048 \
 && openssl rsa -pubout -in /tmp/rsa.pem -out src/main/resources/jwt/publicKey.pem \
 && openssl pkcs8 -topk8 -nocrypt -inform pem -in /tmp/rsa.pem -outform pem -out src/main/resources/jwt/privateKey.pem \
 && rm /tmp/rsa.pem

RUN mvn -q -B clean package -DskipTests

# ---------- Stage 2: run the application ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy only the built output from the build stage.
COPY --from=build /app/target/quarkus-app/ ./

EXPOSE 8080
CMD ["java", "-jar", "quarkus-run.jar"]
