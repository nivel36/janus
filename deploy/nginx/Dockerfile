# syntax=docker/dockerfile:1

FROM node:22 AS fe-build
WORKDIR /app

COPY apps/frontend/package*.json ./apps/frontend/
WORKDIR /app/apps/frontend
RUN npm ci
COPY apps/frontend/ /app/apps/frontend/
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/nginx/conf.d/app.conf /etc/nginx/conf.d/default.conf
COPY --from=fe-build /app/apps/frontend/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
