# ====================================================================
# ParkPilot (智泊领航) - Multi-Stage Lightweight Dockerfile
# Optimized for Edge Gateways, Raspberry Pi & Cloud Production (JRE Alpine)
# ====================================================================

# ---------- Stage 1: Build & Strict Verification ----------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy project source and dependencies
COPY zhixingyuebo-java/src ./src
COPY zhixingyuebo-java/resources ./resources
COPY zhixingyuebo-java/lib ./lib

# Create target classes directory
RUN mkdir -p /build/classes

# Compile all Java sources with Java 8 bytecode compatibility
RUN javac -encoding UTF-8 -source 8 -target 8 \
    -cp "lib/mysql-connector-j-8.1.0.jar" \
    -d /build/classes \
    $(find src -name "*.java")

# Copy default properties to classes classpath
RUN cp resources/db.properties /build/classes/

# ---------- Stage 2: Minimalist Runtime Environment ----------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install runtime dependencies for alpine
RUN apk add --no-cache bash tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# Copy compiled classes and mysql driver
COPY --from=builder /build/classes /app/classes
COPY --from=builder /build/lib/mysql-connector-j-8.1.0.jar /app/lib/mysql-connector-j-8.1.0.jar

# Expose standard environment variables (overridable via docker-compose / k8s)
ENV LANG=C.UTF-8 \
    TZ=Asia/Shanghai \
    ZXYB_JDBC_URL="" \
    ZXYB_JDBC_USERNAME="" \
    ZXYB_JDBC_PASSWORD=""

# Default command: launch ParkPilot main application
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-cp", "/app/classes:/app/lib/mysql-connector-j-8.1.0.jar", "com.zhixingyuebo.MainApp"]
