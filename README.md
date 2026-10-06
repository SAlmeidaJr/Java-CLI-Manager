# Java CLI Manager

Crie classes, records, enums e interfaces pelo terminal integrado do Zed.
Selecione com **↑/↓**, confirme com **Enter** e cancele menus com **Esc**.
**Ctrl+C** cancela qualquer etapa antes da criação.

```text
Qual tipo deseja criar?
❯ Class
  Record
  Enum
  Interface
↑ ↓ navegar · Enter selecionar · Esc cancelar
```

## Compilar e executar

Requer **JDK 21+**, **Maven 3.9+** e um terminal interativo no Linux.
Maven precisa de acesso à internet na primeira compilação para baixar dependências.
O comando `zed` no PATH permite abrir automaticamente o arquivo criado.

```sh
mvn verify
java -jar target/java-cli-manager.jar --project-root /caminho/do/projeto
```

O JAR inclui JLine e suas dependências; depois de compilado, não é necessário
usar Maven para executar. Sem `--project-root`, a CLI usa a pasta atual.
Use `--help` para consultar as opções.

## Instalar para usar em qualquer projeto

Clone o repositório e instale:

```sh
git clone https://github.com/SAlmeidaJr/Java-CLI-Manager.git
cd Java-CLI-Manager
mvn verify
sh scripts/install.sh
```

O instalador copia o JAR para `~/.local/share/java-cli-manager` e cria o comando
`~/.local/bin/java-cli-manager`. Não precisa de `sudo` e não altera configurações
do seu editor. Depois da instalação, você pode mover a pasta do código-fonte.

Se `~/.local/bin` ainda não estiver no seu `PATH`, adicione ao `~/.bashrc`
(ou `~/.zshrc`, caso use Zsh):

```sh
export PATH="$HOME/.local/bin:$PATH"
```

Abra um novo terminal e reinicie o Zed para que ele receba o PATH atualizado.
Agora execute em qualquer projeto:

```sh
cd /caminho/do/seu/projeto
java-cli-manager
```

Ou informe o destino inicial explicitamente:

```sh
java-cli-manager --project-root "/caminho/projeto com espaços"
```

Para instalar em outro local, use `sh scripts/install.sh --prefix /caminho` e
adicione `/caminho/bin` ao PATH. Para atualizar uma instalação, baixe a nova versão,
execute `mvn verify` e rode o instalador novamente com o mesmo prefixo.
Para desinstalar a instalação padrão, remova `~/.local/bin/java-cli-manager` e
a pasta `~/.local/share/java-cli-manager`; remova também a tarefa e o atalho que
adicionou ao Zed, se desejar.

## Criar um arquivo

1. Escolha `Class`, `Record`, `Enum` ou `Interface` com as setas.
2. Selecione **Escolher pasta** e navegue pelas subpastas com ↑/↓ e Enter.
   Use **.. (voltar)** para subir e **Criar nesta pasta** para confirmar o destino.
   A navegação começa na raiz do projeto, mas permite subir para outras pastas.
   Listas grandes rolam conforme a seleção.
3. Confira o pacote sugerido pelo caminho dentro de `src/main/java` ou
   `src/test/java`. Você pode editar ou apagar a sugestão; fora dessas estruturas,
   o campo começa vazio. Nesse modo, o pacote só altera a declaração Java:
   o arquivo é criado diretamente na pasta escolhida, sem duplicar subpastas.
4. Digite o nome sem extensão, por exemplo `Pessoa`.

Por exemplo, selecionar `src/main/java/com/exemplo/model` sugere o pacote
`com.exemplo.model` e cria `Pessoa.java` nessa pasta.
Também continuam disponíveis os destinos **src/main/java + pacote** e
**src/test/java + pacote**, que criam as subpastas a partir do pacote digitado.
Records começam com componentes vazios:
`public record Pessoa() {}`. As pastas necessárias são criadas automaticamente.

Nomes inválidos podem ser corrigidos. Arquivos existentes nunca são sobrescritos:
a CLI solicita outro nome. Se a abertura no Zed falhar, o arquivo permanece salvo
e seu caminho é mostrado. A CLI não usa IA nem precisa de rede para gerar arquivos.

## Atalho no Zed

1. Siga a instalação acima e confirme que `java-cli-manager --help` funciona no
   terminal integrado do Zed.
2. No Zed, execute `zed: open tasks` pela paleta de comandos para editar as tarefas
   globais. Adicione o objeto de [examples/zed/tasks.json](examples/zed/tasks.json)
   ao array existente. O exemplo usa o comando instalado `java-cli-manager`,
   sem caminhos específicos de outro computador.
3. Abra o arquivo de atalhos pelo comando `zed: open keymap file` e adicione a
   configuração de [examples/zed/keymap.json](examples/zed/keymap.json) ao array.
   Se já usar `Alt+J`, escolha outra combinação.
4. Abra a pasta do seu projeto Java e pressione **Alt+J**.

Mescle os exemplos com suas configurações; não substitua tarefas ou atalhos que
já utiliza. A CLI abre no terminal integrado e recebe a raiz do projeto aberto
através de `ZED_WORKTREE_ROOT`. Também é possível executá-la pela tarefa
**Java: criar arquivo** na paleta `task: spawn`.

As configurações de exemplo não são instaladas automaticamente. Após a criação,
o arquivo é aberto com `zed --existing` e o terminal da tarefa é ocultado quando
a execução termina com sucesso. Cancelamentos retornam código 130; erros, 1.

## Limites e testes

A seleção de pasta permite navegar até módulos e estruturas personalizadas.
A sugestão automática de pacote reconhece `src/main/java` e `src/test/java`,
inclusive dentro de módulos. A seleção na árvore do Zed não determina o destino.
O projeto de destino precisa suportar
records caso escolha esse tipo (Java 16+).

`mvn verify` testa geração e compilação dos quatro tipos, pacotes e identificadores,
caminhos com espaços, destinos e proteção contra sobrescrita. Para conferir a
integração, execute em um projeto temporário pelo Zed e verifique:

- ↑/↓ circulam pelas opções, inclusive no segundo menu; Enter confirma.
- Esc e Ctrl+C cancelam sem criar arquivos, e o terminal continua utilizável.
- Campos de texto permitem corrigir erros com Backspace.
- O arquivo criado abre no editor, com pacote e tipo corretos.

A interação usa [JLine](https://jline.org/docs/advanced/key-bindings/);
a integração segue as [tarefas do Zed](https://zed.dev/docs/tasks).

## Licença

Distribuído sob a [licença MIT](LICENSE).
