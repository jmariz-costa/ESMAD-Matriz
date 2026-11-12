import { expect, test } from '@playwright/test'

/**
 * Percurso do utilizador: autenticar → abrir projeto → criar tarefa → ver a tarefa no quadro
 * (US02, US05 crit. 1, US07 crit. 1–2). Ficha PL 04, Exercício 4; cenário T18 do docs/test-plan.md.
 *
 * Dados isolados: o projeto tem um nome único e é criado pela API para este teste; no fim é
 * eliminado, o que apaga as tarefas em cascata (RN22). Os outros projetos da conta não são tocados.
 */

const EMAIL = 'ana@empresa.pt'
// A palavra-passe das contas de demonstração vem do ambiente e nunca fica escrita no repositório
const PALAVRA_PASSE = process.env.MATRIZ_DEMO_PASSWORD

let projetoId: string | undefined

test.beforeAll(() => {
  if (!PALAVRA_PASSE) {
    throw new Error('Defina MATRIZ_DEMO_PASSWORD (o valor está em backend/.env) antes de correr os testes E2E.')
  }
})

test.afterEach(async ({ page }) => {
  if (projetoId) {
    const resposta = await page.request.delete(`/api/projetos/${projetoId}`)
    expect(resposta.status(), 'limpeza do projeto de teste').toBe(204)
    projetoId = undefined
  }
})

test('cria uma tarefa e ela aparece na coluna "A fazer" do quadro', async ({ page }) => {
  const marca = `${Date.now()}-${test.info().workerIndex}`
  const nomeProjeto = `E2E ${marca}`
  const tituloTarefa = `Tarefa E2E ${marca}`

  // 1. Autenticar
  await page.goto('/entrar')
  await page.getByLabel('Email').fill(EMAIL)
  await page.getByLabel('Palavra-passe').fill(PALAVRA_PASSE!)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('heading', { name: /^Olá,/ })).toBeVisible()

  // Preparação: projeto próprio deste teste, criado com a sessão que acabou de ser aberta
  const criado = await page.request.post('/api/projetos', { data: { nome: nomeProjeto } })
  expect(criado.status()).toBe(201)
  projetoId = (await criado.json()).id

  // 2. Abrir o projeto a partir da lista de projetos
  await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Projetos' }).click()
  await page.getByLabel('Pesquisar pelo nome').fill(nomeProjeto)
  await page.getByRole('link', { name: nomeProjeto, exact: true }).click()
  await expect(page.getByRole('heading', { name: nomeProjeto })).toBeVisible()
  // Projeto novo: o quadro mostra o estado "Vazio" do wireframe (docs/ui/wireframe.html), sem colunas
  await expect(page.getByText('Este projeto ainda não tem tarefas.')).toBeVisible()
  const colunaAFazer = page.getByRole('region', { name: 'A fazer', exact: true })

  // 3. Criar a tarefa
  await page.getByRole('button', { name: '+ Nova tarefa' }).click()
  const dialogo = page.getByRole('dialog')
  await dialogo.getByLabel('Título *').fill(tituloTarefa)
  await dialogo.getByRole('button', { name: 'Criar' }).click()

  // 4. A tarefa aparece uma única vez, na coluna "A fazer", sem recarregar a página
  await expect(dialogo).toBeHidden()
  await expect(page.getByRole('status')).toContainText('Tarefa criada.')
  await expect(colunaAFazer.getByRole('button', { name: tituloTarefa, exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: tituloTarefa, exact: true })).toHaveCount(1)
  await expect(colunaAFazer.getByRole('heading')).toHaveText(/A fazer\s*1/)

  // E ficou guardada no servidor, no estado "A fazer"
  const tarefas = await page.request.get(`/api/projetos/${projetoId}/tarefas`)
  expect(tarefas.status()).toBe(200)
  const lista = await tarefas.json()
  expect(lista.total).toBe(1)
  expect(lista.itens[0]).toMatchObject({ titulo: tituloTarefa, estado: 'A_FAZER' })
})
