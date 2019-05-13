### STAGE 1: Build ###
FROM node:8-alpine as builder

RUN npm set progress=false && npm config set depth 0
#&& npm cache clean --force
RUN mkdir /web
WORKDIR /web
COPY . .
RUN npm config set registry http://registry.npmjs.org/ \
    npm ci \
    $(npm bin)/ng build --prod --build-optimizer

#RUN npm update && ng serve --prod

### STAGE 2: Setup ###
FROM nginx:1.16.0-alpine
#FROM janeczku/debian-nginx

RUN apk update \
    apk add jq

# set back-url
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/script/start.sh /etc/nginx/start.sh
RUN chmod +x /etc/nginx/start.sh

RUN rm -rf /usr/share/nginx/html/*

COPY --from=builder /web/dist /usr/share/nginx/html

#CMD ['/bin/bash -c "envsubst < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g \'daemon off;\'"']
CMD ["/etc/nginx/start.sh"]

EXPOSE 80
