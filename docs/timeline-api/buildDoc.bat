if not exist "target" mkdir "target"
npx redoc-cli bundle timeline-api-swagger2.yml -o target\timeline-api-doc.html
