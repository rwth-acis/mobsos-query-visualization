FROM node:alpine

WORKDIR /app

COPY ["package.json", "package-lock.json*", "./"]

RUN npm install --production

COPY . .
EXPOSE 5500
CMD [ "node", "index.js" ]