FROM node:22-alpine AS build
WORKDIR /app
COPY web/package*.json ./
RUN npm ci
COPY web/ .
RUN npm run build
FROM nginx:1.27-alpine
COPY --from=build /app/dist/event-ticketing-web/browser /usr/share/nginx/html
COPY web/nginx.conf /etc/nginx/conf.d/default.conf
