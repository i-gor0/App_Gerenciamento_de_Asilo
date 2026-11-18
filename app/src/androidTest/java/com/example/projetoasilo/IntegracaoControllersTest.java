package com.example.projetoasilo;

import BancoDeDados.*;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class IntegracaoControllersTest {

    private Context context;
    private BancoDeDados.CriaBanco banco;
    private SQLiteDatabase db;

    private BancoControllerPerfil perfilController;
    private BancoControllerUser userController;
    private BancoControllerQuarto quartoController;
    private BancoControllerPaciente pacienteController;
    private BancoControllerEstoque estoqueController;

    private int perfilId;
    private int usuarioId;
    private int quartoId;
    private int pacienteId;
    private int produtoId;

    @Before
    public void prepararBanco() {

        context = ApplicationProvider.getApplicationContext();

        // Começa cada teste com um banco limpo.
        context.deleteDatabase("Banco_Asilo.db");

        banco = new BancoDeDados.CriaBanco(context);
        db = banco.getWritableDatabase();

        // Cria um perfil de administrador.
        ContentValues perfil = new ContentValues();
        perfil.put("nome", "Administrador");
        perfil.put("podecadastrar", 1);
        perfil.put("podebaixarestoque", 1);
        perfil.put("podegerenciarusuarios", 1);

        perfilId = (int) db.insertOrThrow(
                "Perfil", null, perfil
        );

        // Cria um usuário de teste.
        ContentValues usuario = new ContentValues();
        usuario.put("perfilId", perfilId);
        usuario.put("nome", "Administrador Teste");
        usuario.put("login", "admin");
        usuario.put("senhahash", "hash_teste");
        usuario.put("ativo", 1);

        usuarioId = (int) db.insertOrThrow(
                "Usuario", null, usuario
        );

        // Cria um quarto.
        ContentValues quarto = new ContentValues();
        quarto.put("numero", "101");
        quarto.put("capacidade", 2);

        quartoId = (int) db.insertOrThrow(
                "Quarto", null, quarto
        );

        // Cria um paciente.
        ContentValues paciente = new ContentValues();
        paciente.put("quartoId", quartoId);
        paciente.put("nome", "João");
        paciente.put("datanascimento", "1950-01-01");
        paciente.put("observacoes", "Paciente de teste");

        pacienteId = (int) db.insertOrThrow(
                "Paciente", null, paciente
        );

        // Cria um produto.
        ContentValues produto = new ContentValues();
        produto.put("nome", "Fralda");
        produto.put("tipo", "Higiene");
        produto.put("codigoqr", "FRALDA001");
        produto.put("unidademedida", "Unidade");

        produtoId = (int) db.insertOrThrow(
                "Produto", null, produto
        );

        db.close();

        // Simula uma sessão autenticada.
        SharedPreferences sessao =
                context.getSharedPreferences(
                        "Sessao", Context.MODE_PRIVATE
                );

        sessao.edit()
                .putInt("usuarioId", usuarioId)
                .putString("usuarioNome", "Administrador Teste")
                .putInt("perfilId", perfilId)
                .putBoolean("logado", true)
                .commit();

        // Inicializa os controllers.
        perfilController = new BancoControllerPerfil(context);
        userController = new BancoControllerUser(context);
        quartoController = new BancoControllerQuarto(context);
        pacienteController = new BancoControllerPaciente(context);
        estoqueController = new BancoControllerEstoque(context);
    }

    @After
    public void finalizarTeste() {

        banco.close();

        context.getSharedPreferences(
                "Sessao", Context.MODE_PRIVATE
        ).edit().clear().commit();
    }

    // Método auxiliar para contar registros.
    private int contarRegistros(String tabela) {

        SQLiteDatabase database = banco.getReadableDatabase();

        Cursor cursor = database.rawQuery(
                "SELECT COUNT(*) FROM " + tabela,
                null
        );

        cursor.moveToFirst();
        int quantidade = cursor.getInt(0);

        cursor.close();
        return quantidade;
    }

    // =================================================
    // TESTES DO PERFIL
    // =================================================

    @Test
    public void perfilDeveCadastrarNovoPerfil() {

        perfilController.insereDados(
                "Funcionário", 1, 0, 0
        );

        assertEquals(2, contarRegistros("Perfil"));
    }

    @Test
    public void perfilDeveListarPerfis() {

        Cursor cursor = perfilController.listarPerfis();

        assertNotNull(cursor);
        assertTrue(cursor.getCount() >= 1);

        cursor.close();
    }

    @Test
    public void perfilDeveBuscarPerfil() {

        Cursor cursor = perfilController.buscarPerfil(perfilId);

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());

        assertEquals(
                "Administrador",
                cursor.getString(
                        cursor.getColumnIndexOrThrow("nome")
                )
        );

        cursor.close();
    }

    @Test
    public void perfilDeveVerificarPermissaoExistente() {

        assertTrue(
                perfilController.temPermissao(
                        usuarioId, "podecadastrar"
                )
        );
    }

    @Test
    public void perfilDeveNegarPermissaoNaoConcedida() {

        assertFalse(
                perfilController.temPermissao(
                        usuarioId, "podebaixarestoque"
                ) == false
        );
    }

    // =================================================
    // TESTES DO USUÁRIO
    // =================================================

    @Test
    public void usuarioDeveCadastrarNovoUsuario() {

        long id = userController.adicionarUsuario(
                "Maria",
                "maria",
                "hash_maria",
                perfilId
        );

        assertTrue(id > 0);
        assertEquals(2, contarRegistros("Usuario"));
    }

    @Test
    public void usuarioDeveListarUsuarios() {

        Cursor cursor = userController.listarUsuarios();

        assertNotNull(cursor);
        assertTrue(cursor.getCount() >= 1);

        cursor.close();
    }

    @Test
    public void usuarioDeveBuscarUsuario() {

        Cursor cursor = userController.buscarUsuario(usuarioId);

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());

        assertEquals(
                "Administrador Teste",
                cursor.getString(
                        cursor.getColumnIndexOrThrow("nome")
                )
        );

        cursor.close();
    }

    @Test
    public void usuarioDeveFazerLogin() {

        int resultado = userController.Login(
                "admin", "hash_teste", context
        );

        assertEquals(1, resultado);

        SharedPreferences sessao =
                context.getSharedPreferences(
                        "Sessao", Context.MODE_PRIVATE
                );

        assertTrue(sessao.getBoolean("logado", false));
    }

    @Test
    public void usuarioDeveRecusarSenhaIncorreta() {

        int resultado = userController.Login(
                "admin", "senha_errada", context
        );

        assertEquals(0, resultado);
    }

    @Test
    public void usuarioDeveAtualizarDados() {

        int resultado = userController.atualizarUsuario(
                usuarioId,
                "Administrador Atualizado",
                "admin",
                null,
                perfilId
        );

        assertTrue(resultado > 0);

        Cursor cursor = userController.buscarUsuario(usuarioId);

        assertTrue(cursor.moveToFirst());

        assertEquals(
                "Administrador Atualizado",
                cursor.getString(
                        cursor.getColumnIndexOrThrow("nome")
                )
        );

        cursor.close();
    }

    @Test
    public void usuarioDeveSerDesativadoEReativado() {

        int desativado = userController.excluirUsuario(usuarioId);
        assertTrue(desativado > 0);

        int login = userController.Login(
                "admin", "hash_teste", context
        );

        assertEquals(0, login);

        int ativado = userController.ativarUsuario(usuarioId);
        assertTrue(ativado > 0);

        login = userController.Login(
                "admin", "hash_teste", context
        );

        assertEquals(1, login);
    }

    @Test
    public void usuarioDeveEncerrarSessao() {

        userController.Logout(context);

        SharedPreferences sessao =
                context.getSharedPreferences(
                        "Sessao", Context.MODE_PRIVATE
                );

        assertFalse(sessao.getBoolean("logado", false));
    }

    // =================================================
    // TESTES DO QUARTO
    // =================================================

    @Test
    public void quartoDeveCadastrar() {

        quartoController.insereDados("202", 3);

        assertEquals(2, contarRegistros("Quarto"));
    }

    @Test
    public void quartoDeveListar() {

        Cursor cursor = quartoController.listarQuartos();

        assertNotNull(cursor);
        assertTrue(cursor.getCount() >= 1);

        cursor.close();
    }

    @Test
    public void quartoDeveBuscar() {

        Cursor cursor = quartoController.buscarQuarto(quartoId);

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());

        assertEquals(
                "101",
                cursor.getString(
                        cursor.getColumnIndexOrThrow("numero")
                )
        );

        cursor.close();
    }

    @Test
    public void quartoDeveAtualizar() {

        quartoController.atualizarQuarto(
                quartoId, "105", 4
        );

        Cursor cursor = quartoController.buscarQuarto(quartoId);

        assertTrue(cursor.moveToFirst());

        assertEquals(
                "105",
                cursor.getString(
                        cursor.getColumnIndexOrThrow("numero")
                )
        );

        assertEquals(
                4,
                cursor.getInt(
                        cursor.getColumnIndexOrThrow("capacidade")
                )
        );

        cursor.close();
    }

    @Test
    public void quartoDeveExcluirQuandoNaoPossuirPaciente() {

        // Cria um segundo quarto sem pacientes.
        quartoController.insereDados("202", 2);

        Cursor cursor = banco.getReadableDatabase().rawQuery(
                "SELECT Id FROM Quarto WHERE numero = '202'",
                null
        );

        assertTrue(cursor.moveToFirst());
        int novoQuartoId = cursor.getInt(0);
        cursor.close();

        quartoController.excluirQuarto(novoQuartoId);

        assertEquals(1, contarRegistros("Quarto"));
    }

    // =================================================
    // TESTES DO PACIENTE
    // =================================================

    @Test
    public void pacienteDeveCadastrar() {

        pacienteController.insereDados(
                quartoId,
                "Maria",
                "1948-05-10",
                "Paciente de teste"
        );

        assertEquals(2, contarRegistros("Paciente"));

        // Cadastro não deve criar estoque automaticamente.
        assertEquals(0, contarRegistros("Estoque"));
    }

    @Test
    public void pacienteDeveListar() {

        Cursor cursor = pacienteController.listarPacientes();

        assertNotNull(cursor);
        assertTrue(cursor.getCount() >= 1);

        cursor.close();
    }

    @Test
    public void pacienteDeveBuscar() {

        Cursor cursor = pacienteController.buscarPaciente(pacienteId);

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());

        assertEquals(
                "João",
                cursor.getString(
                        cursor.getColumnIndexOrThrow("nome")
                )
        );

        cursor.close();
    }

    @Test
    public void pacienteDeveAtualizar() {

        pacienteController.atualizarPaciente(
                pacienteId,
                quartoId,
                "João Atualizado",
                "1950-01-01",
                "Observação atualizada"
        );

        Cursor cursor = pacienteController.buscarPaciente(pacienteId);

        assertTrue(cursor.moveToFirst());

        assertEquals(
                "João Atualizado",
                cursor.getString(
                        cursor.getColumnIndexOrThrow("nome")
                )
        );

        cursor.close();
    }

    @Test
    public void pacienteDeveExcluir() {

        pacienteController.excluirPaciente(pacienteId);

        assertEquals(0, contarRegistros("Paciente"));
    }

    // =================================================
    // TESTES DO ESTOQUE
    // =================================================

    @Test
    public void estoqueDeveAdicionarProdutoGeral() {

        estoqueController.adicionarEstoqueGeral(produtoId, 50);
        estoqueController.adicionarEstoqueGeral(produtoId, 50);

        Cursor cursor = estoqueController.listarEstoqueGeral();

        assertTrue(cursor.moveToFirst());

        assertEquals(
                100,
                cursor.getInt(
                        cursor.getColumnIndexOrThrow("quantidadeatual")
                )
        );

        cursor.close();
    }

    @Test
    public void estoqueDeveTransferirProdutoParaPaciente() {

        estoqueController.adicionarEstoqueGeral(produtoId, 100);

        String resultado = estoqueController.transferirParaPaciente(
                produtoId, pacienteId, 20
        );

        assertEquals(
                "Produto transferido para o paciente com sucesso.",
                resultado
        );

        Cursor geral = estoqueController.listarEstoqueGeral();

        assertTrue(geral.moveToFirst());
        assertEquals(
                80,
                geral.getInt(
                        geral.getColumnIndexOrThrow("quantidadeatual")
                )
        );

        geral.close();

        Cursor individual = estoqueController.listarEstoque(pacienteId);

        assertTrue(individual.moveToFirst());
        assertEquals(
                20,
                individual.getInt(
                        individual.getColumnIndexOrThrow("quantidadeatual")
                )
        );

        individual.close();
    }

    @Test
    public void estoqueNaoDeveTransferirQuantidadeInsuficiente() {

        estoqueController.adicionarEstoqueGeral(produtoId, 10);

        String resultado = estoqueController.transferirParaPaciente(
                produtoId, pacienteId, 20
        );

        assertEquals(
                "Quantidade insuficiente no estoque geral.",
                resultado
        );

        Cursor geral = estoqueController.listarEstoqueGeral();

        assertTrue(geral.moveToFirst());
        assertEquals(
                10,
                geral.getInt(
                        geral.getColumnIndexOrThrow("quantidadeatual")
                )
        );

        geral.close();

        Cursor individual = estoqueController.listarEstoque(pacienteId);

        assertEquals(0, individual.getCount());

        individual.close();
    }

    @Test
    public void estoqueDeveSomarTransferenciasDoMesmoProduto() {

        estoqueController.adicionarEstoqueGeral(produtoId, 100);

        estoqueController.transferirParaPaciente(
                produtoId, pacienteId, 10
        );

        estoqueController.transferirParaPaciente(
                produtoId, pacienteId, 15
        );

        Cursor cursor = estoqueController.listarEstoque(pacienteId);

        assertTrue(cursor.moveToFirst());

        assertEquals(
                25,
                cursor.getInt(
                        cursor.getColumnIndexOrThrow("quantidadeatual")
                )
        );

        cursor.close();

        assertEquals(1, contarRegistros("Estoque"));
    }

    @Test
    public void estoqueDeveAtualizarQuantidadeMinima() {

        estoqueController.adicionarEstoqueGeral(produtoId, 50);

        estoqueController.transferirParaPaciente(
                produtoId, pacienteId, 10
        );

        Cursor cursor = estoqueController.listarEstoque(pacienteId);

        assertTrue(cursor.moveToFirst());

        int estoqueId = cursor.getInt(
                cursor.getColumnIndexOrThrow("Id")
        );

        cursor.close();

        estoqueController.atualizarQuantidadeMinima(
                estoqueId, 5
        );

        cursor = estoqueController.listarEstoque(pacienteId);

        assertTrue(cursor.moveToFirst());

        assertEquals(
                5,
                cursor.getInt(
                        cursor.getColumnIndexOrThrow("quantidademinima")
                )
        );

        cursor.close();
    }
}
