package pt.esmad.matriz;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Regras de organização do Backend do ADR 0001 (camadas por módulo com DDD leve), verificadas sobre
 * as classes compiladas, sem Quarkus nem base de dados.
 *
 * <p>As camadas não estão em sub-packages: distinguem-se pelo sufixo do nome ({@code *Recurso},
 * {@code *Servico}, {@code *Repositorio}) e pela anotação {@code @Entity}. Uma classe aninhada pertence
 * à camada da classe onde está declarada (por exemplo, {@code GestoresServico.NovoGestor} é da camada
 * de serviço).
 *
 * <p>Os ciclos entre módulos (contas ↔ projetos, contas ↔ tarefas, projetos ↔ tarefas) são um
 * compromisso aceite e não são verificados aqui; ver o ADR 0001, secção «Dependências circulares entre
 * módulos».
 *
 * <p>Etiqueta {@code arquitetura}: a CI corre estas regras num passo próprio, antes dos outros testes
 * unitários ({@code make testes-arquitetura} faz o mesmo localmente).
 */
@ArchTag("arquitetura")
@AnalyzeClasses(packages = "pt.esmad.matriz", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String BASE = "pt.esmad.matriz.";
    private static final List<String> MODULOS = List.of("contas", "projetos", "tarefas", "comentarios", "historico");

    /** Classe de apoio dos serviços de Projetos (carrega o projeto e verifica o acesso), sem sufixo Servico. */
    private static final String APOIO_SERVICOS = "AcessoProjeto";

    /** Regra 2: recurso → serviço → repositório e entidades. */
    @ArchTest
    static final ArchRule camadas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Recursos").definedBy(deTopoTerminadaEm("Recurso"))
            .layer("Serviços").definedBy(deTopoTerminadaEm("Servico").or(deTopoChamada(APOIO_SERVICOS)))
            .layer("Repositórios").definedBy(deTopoTerminadaEm("Repositorio"))
            .layer("Entidades").definedBy(entidade())
            .whereLayer("Recursos").mayNotBeAccessedByAnyLayer()
            .whereLayer("Serviços").mayOnlyBeAccessedByLayers("Recursos")
            .whereLayer("Repositórios").mayOnlyBeAccessedByLayers("Serviços")
            .whereLayer("Entidades").mayOnlyBeAccessedByLayers("Serviços", "Repositórios")
            .as("Recursos só usam serviços e DTOs; repositórios e entidades só são usados pelos serviços")
            .because("os recursos recebem e devolvem DTOs, nunca entidades, e o serviço é o caso de uso"
                    + " (ADR 0001, regra 2)");

    /** Regra 5: entre módulos, só através do serviço do outro módulo. */
    @ArchTest
    static final ArchRule entreModulosSoPorServicos = classes()
            .that().resideInAnyPackage(MODULOS.stream().map(m -> BASE + m + "..").toArray(String[]::new))
            .should(naoUsarEntidadesRepositoriosNemRecursosDeOutroModulo())
            .as("Um módulo não usa entidades, repositórios nem recursos de outro módulo")
            .because("entre módulos, só através do serviço do outro módulo (ADR 0001, regra 5)");

    /** Regra 1: o package comum serve todos os módulos e não depende de nenhum. */
    @ArchTest
    static final ArchRule comumNaoDependeDosModulos = noClasses()
            .that().resideInAPackage(BASE + "comum..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(MODULOS.stream().map(m -> BASE + m + "..").toArray(String[]::new))
            .as("O package comum não depende de nenhum módulo")
            .because("comum tem só o que é partilhado: formato de erro, autenticação e configuração"
                    + " (ADR 0001, regra 1)");

    private static JavaClass deTopo(JavaClass classe) {
        JavaClass atual = classe;
        Optional<JavaClass> exterior = atual.getEnclosingClass();
        while (exterior.isPresent()) {
            atual = exterior.get();
            exterior = atual.getEnclosingClass();
        }
        return atual;
    }

    private static DescribedPredicate<JavaClass> deTopoTerminadaEm(String sufixo) {
        return DescribedPredicate.describe("declarada numa classe terminada em " + sufixo,
                c -> deTopo(c).getSimpleName().endsWith(sufixo));
    }

    private static DescribedPredicate<JavaClass> deTopoChamada(String nome) {
        return DescribedPredicate.describe("declarada na classe " + nome,
                c -> deTopo(c).getSimpleName().equals(nome));
    }

    private static DescribedPredicate<JavaClass> entidade() {
        return DescribedPredicate.describe("entidade JPA", c -> deTopo(c).isAnnotatedWith(Entity.class));
    }

    private static Optional<String> modulo(JavaClass classe) {
        String pacote = classe.getPackageName();
        return MODULOS.stream()
                .filter(m -> pacote.equals(BASE + m) || pacote.startsWith(BASE + m + "."))
                .findFirst();
    }

    private static ArchCondition<JavaClass> naoUsarEntidadesRepositoriosNemRecursosDeOutroModulo() {
        DescribedPredicate<JavaClass> interna = entidade()
                .or(deTopoTerminadaEm("Repositorio"))
                .or(deTopoTerminadaEm("Recurso"));
        return new ArchCondition<>("não usar entidades, repositórios nem recursos de outro módulo") {
            @Override
            public void check(JavaClass classe, ConditionEvents eventos) {
                Optional<String> origem = modulo(classe);
                for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                    JavaClass alvo = dependencia.getTargetClass();
                    Optional<String> destino = modulo(alvo);
                    if (destino.isPresent() && !destino.equals(origem) && interna.test(alvo)) {
                        eventos.add(SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
                    }
                }
            }
        };
    }
}
