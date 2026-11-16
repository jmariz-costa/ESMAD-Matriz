# syntax=docker/dockerfile:1
# Imagem do Frontend da Matriz (Ficha PL 05, Exercício 2).
# Construir a partir da raiz do repositório:  docker build -t matriz-frontend frontend
# O nginx serve o build do Vite e reencaminha /api para o Backend (nginx.conf.template).

# --- 1. Build: Node 24, dependências exatas do package-lock.json ---
FROM node:24.22.3-alpine3.24 AS build
WORKDIR /app

# Dependências numa camada própria: só voltam a ser instaladas quando o package-lock.json muda
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- 2. Execução: nginx sem root (utilizador nginx, porta 8080) ---
FROM nginxinc/nginx-unprivileged:1.30.6-alpine3.24
# Destino de /api (nginx.conf.template). Por omissão, o Backend na rede do Docker Compose;
# no Render, o endereço público do Backend.
ENV MATRIZ_API_URL=http://backend:8081
# No arranque, a imagem gera a configuração a partir do modelo, em /tmp, porque no Compose o resto
# do sistema de ficheiros é só de leitura (read_only); o default.conf passa a incluir esse ficheiro.
ENV NGINX_ENVSUBST_OUTPUT_DIR=/tmp
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
RUN echo 'include /tmp/default.conf;' > /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
