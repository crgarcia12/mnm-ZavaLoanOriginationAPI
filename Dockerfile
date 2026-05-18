FROM gradle:7.6-jdk11 AS build
WORKDIR /app
COPY . .
RUN gradle war --no-daemon

FROM tomcat:9-jdk11
ENV DB_HOST=sqlserver
ENV DB_PORT=1433
ENV DB_NAME=ZavaBankDB
ENV DB_USER=sa
ENV DB_PASSWORD=YourStrong!Passw0rd
ENV KYC_SERVICE_BASE_URL=http://zava-kyc-service:8080
ENV RISK_SERVICE_BASE_URL=http://zava-risk-engine:8080
ENV LEDGER_SERVICE_BASE_URL=http://zava-ledger:8080
COPY --from=build /app/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war
RUN rm -rf /usr/local/tomcat/webapps/ROOT
EXPOSE 8080
CMD ["catalina.sh", "run"]
