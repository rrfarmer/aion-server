# Use a suitable base image
FROM ubuntu:25.10

# Install dependencies including CA certificates
RUN apt-get update && \
    apt-get install -y wget tar ca-certificates && \
    update-ca-certificates

# Download and install Temurin JDK 24
RUN wget https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24+36/OpenJDK24U-jdk_x64_linux_hotspot_24_36.tar.gz -O /tmp/jdk.tar.gz && \
    mkdir -p /usr/lib/jvm && \
    tar -xzf /tmp/jdk.tar.gz -C /usr/lib/jvm && \
    ln -s /usr/lib/jvm/jdk-24+36 /usr/lib/jvm/java-24-temurin

# Set environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-24-temurin
ENV PATH="$JAVA_HOME/bin:$PATH"

# Copy all contents (including start.sh)
COPY . /app
WORKDIR /app

RUN chmod +x start.sh

EXPOSE 7777

CMD ["./start.sh"]
