# Comandos da Matriz para correr localmente (Linux, macOS, ou Windows com WSL/Git Bash).
# `make` sem argumentos mostra a ajuda. Detalhe em README.md, "Executar com o Makefile".
#
# Requisitos: Docker com Docker Compose (aplicação); JDK 21 (testes do Backend);
# Node.js 20.19+ e npm (testes do Frontend e E2E); openssl (make configurar).

SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c
.DEFAULT_GOAL := ajuda

ENV := backend/.env
ENV_MODELO := backend/.env.example
URL_APP := http://localhost:8090
# Variáveis com palavras-passe geradas pelo make configurar (as mesmas que a CI gera)
SEGREDOS := POSTGRES_PASSWORD MATRIZ_ADMIN_PASSWORD MATRIZ_DEMO_PASSWORD

.PHONY: ajuda configurar arrancar parar registos estado bd \
        testes testes-unitarios testes-integracao testes-frontend testes-e2e cobertura verificar

ajuda: ## Mostra esta ajuda
	@echo "Matriz: comandos disponíveis"
	@echo
	@grep -E '^[a-z0-9-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "} {printf "  make %-18s %s\n", $$1, $$2}'
	@echo
	@echo "Primeira vez: make configurar && make arrancar  ->  $(URL_APP)"

configurar: ## Primeira vez: cria o backend/.env com palavras-passe aleatórias (nunca o substitui)
	@if [ -f $(ENV) ]; then \
	  echo "O $(ENV) já existe: não foi alterado (as palavras-passe mantêm-se)."; \
	  exit 0; \
	fi; \
	command -v openssl >/dev/null || { echo "É preciso o openssl para gerar as palavras-passe."; exit 1; }; \
	cp $(ENV_MODELO) $(ENV); \
	for variavel in $(SEGREDOS); do \
	  valor="$$(openssl rand -hex 16)"; \
	  sed -i.bak "s|^$${variavel}=.*|$${variavel}=$${valor}|" $(ENV); \
	done; \
	rm -f $(ENV).bak; \
	chmod 600 $(ENV); \
	demo="$$(sed -n 's/^MATRIZ_DEMO_PASSWORD=//p' $(ENV))"; \
	admin_email="$$(sed -n 's/^MATRIZ_ADMIN_EMAIL=//p' $(ENV))"; \
	admin="$$(sed -n 's/^MATRIZ_ADMIN_PASSWORD=//p' $(ENV))"; \
	echo "Criado o $(ENV) com palavras-passe aleatórias (só existem neste computador)."; \
	echo; \
	echo "  Contas de demonstração: ana|bruno|carla|daniel@empresa.pt   palavra-passe: $$demo"; \
	echo "  Administrador:          $$admin_email                    palavra-passe: $$admin"; \
	echo; \
	echo "Ficam guardadas no $(ENV) (nunca vai para o Git). Só contam no primeiro arranque:"; \
	echo "depois, as contas já existem e mudar o ficheiro não as altera."

arrancar: | $(ENV) ## Aplicação completa em Docker (PostgreSQL, Backend e Frontend)
	docker compose up -d --build --wait --wait-timeout 300
	@echo "Matriz a correr em $(URL_APP)"

parar: ## Para os contentores (os dados mantêm-se no volume matriz-dados)
	docker compose stop

registos: ## Mostra os registos dos contentores (Ctrl+C para sair)
	docker compose logs -f --tail=100

estado: ## Estado dos contentores
	docker compose ps

bd: | $(ENV) ## Só o PostgreSQL, para o modo de desenvolvimento (Backend e Frontend fora do Docker)
	docker compose up -d --wait postgres

testes: testes-unitarios testes-integracao testes-frontend ## Unitários e integração do Backend, e Frontend

testes-unitarios: ## Backend: testes unitários (sem Quarkus nem base de dados)
	cd backend && ./mvnw -B test -DexcludedGroups=integracao

testes-integracao: | $(ENV) ## Backend: testes de integração (precisa do Docker: PostgreSQL temporário)
	cd backend && ./mvnw -B test -Dgroups=integracao -Dquarkus.http.test-port=0

testes-frontend: frontend/node_modules ## Frontend: testes com Vitest
	cd frontend && npm test

testes-e2e: frontend/node_modules | $(ENV) ## E2E com Playwright contra a aplicação do make arrancar
	@curl -fsS -o /dev/null $(URL_APP) || { echo "A aplicação não está a correr em $(URL_APP): faça make arrancar."; exit 1; }
	cd frontend && npx playwright install firefox
	@# A palavra-passe é lida do backend/.env e passada só a este processo (nunca escrita noutro ficheiro)
	cd frontend && MATRIZ_E2E_URL=$(URL_APP) \
	  MATRIZ_DEMO_PASSWORD="$$(sed -n 's/^MATRIZ_DEMO_PASSWORD=//p' ../$(ENV))" npm run e2e

cobertura: | $(ENV) ## Backend: todos os testes, com cobertura e mínimo de 95% (JaCoCo)
	cd backend && ./mvnw -B verify -Dquarkus.http.test-port=0
	@echo "Relatório: backend/target/site/jacoco/index.html"

verificar: frontend/node_modules ## Frontend: tipos, lint e build
	cd frontend && npx tsc -b && npm run lint && npm run build

$(ENV):
	@echo "Falta o $(ENV): faça primeiro make configurar."; exit 1

frontend/node_modules: frontend/package-lock.json
	cd frontend && npm ci
	@touch frontend/node_modules
