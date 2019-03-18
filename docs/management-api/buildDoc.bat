if not exist "target" mkdir "target"
npx redoc-cli bundle management-api-swagger2.yml -o target\management-api-doc.html
