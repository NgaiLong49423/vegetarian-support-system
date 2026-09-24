FROM node:22-alpine

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ENV FIGMA_DEV_SERVER_HOST=0.0.0.0 \
    PORT=5173 \
    VITE_OPEN_BROWSER=false \
    CHOKIDAR_USEPOLLING=true

EXPOSE 5173

CMD ["npm", "run", "dev"]
