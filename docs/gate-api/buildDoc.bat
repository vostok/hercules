if not exist "target" mkdir "target"
npx redoc-cli bundle gate-api-swagger2.yml -o target\gate-api-doc.html
