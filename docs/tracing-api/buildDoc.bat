if not exist "target" mkdir "target"
npx redoc-cli bundle tracing-api-swagger2.yml -o target\tracing-api-doc.html
