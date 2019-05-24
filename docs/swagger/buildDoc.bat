if not exist "target" mkdir "target"
for %%a in (gate-api management-api stream-api timeline-api tracing-api) do npx redoc-cli bundle %%a-swagger2.yml -o target\%%a-doc.html
