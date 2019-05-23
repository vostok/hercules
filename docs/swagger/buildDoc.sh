mkdir -p target
for service in gate-api management-api stream-api timeline-api tracing-api
do
   npx redoc-cli bundle $service-swagger2.yml -o target/$service-doc.html
done
