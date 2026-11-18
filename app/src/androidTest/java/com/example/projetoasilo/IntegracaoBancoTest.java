package com.example.projetoasilo;
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

import BancoDeDados.BancoControllerEstoque;

@RunWith(AndroidJUnit4.class)
public class IntegracaoBancoTest {

    private Context context;
    private BancoDeDados.CriaBanco banco;
    private BancoControllerEstoque controller;

    private int produtoId;
    private int pacienteId;
    private int usuarioId;

    @Before
    public void prepararBanco() {

        context = ApplicationProvider.getApplicationContext();

        // Apaga o banco anterior para começar com dados limpos.
        context.deleteDatabase("Banco_Asilo.db");

        // Cria o banco e suas tabelas.
        banco = new BancoDeDados.CriaBanco(context);

        SQLiteDatabase db = banco.getWritableDatabase();

        // Cria um perfil.
        ContentValues perfil = new ContentValues();
        perfil.put("nome", "Administrador");
        perfil.put("podecadastrar", 1);
        perfil.put("podebaixarestoque", 1);
        perfil.put("podegerenciarusuarios", 1);

        long perfilId = db.insertOrThrow("Perfil", null, perfil);

        // Cria um usuário.
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

        long quartoId = db.insertOrThrow(
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

        // Simula um usuário autenticado.
        SharedPreferences sessao =
                context.getSharedPreferences(
                        "Sessao", Context.MODE_PRIVATE
                );

        sessao.edit()
                .putInt("usuarioId", usuarioId)
                .putBoolean("logado", true)
                .apply();

        controller = new BancoControllerEstoque(context);
    }

    @After
    public void finalizarTeste() {
        banco.close();

        context.getSharedPreferences("Sessao", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    // TESTE 1: Adicionar produtos ao estoque geral.

    @Test
    public void deveAdicionarProdutoAoEstoqueGeral() {

        controller.adicionarEstoqueGeral(produtoId, 50);
        controller.adicionarEstoqueGeral(produtoId, 50);

        SQLiteDatabase db = banco.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT quantidadeatual FROM EstoqueGeral WHERE produtoId = ?",
                new String[]{String.valueOf(produtoId)}
        );

        assertTrue(cursor.moveToFirst());

        int quantidade = cursor.getInt(0);

        assertEquals(100, quantidade);

        cursor.close();
        db.close();
    }

    // TESTE 2: Transferir produto para um paciente.

    @Test
    public void deveTransferirProdutoParaPaciente() {

        controller.adicionarEstoqueGeral(produtoId, 100);

        String resultado = controller.transferirParaPaciente(
                produtoId,
                pacienteId,
                20
        );

        assertEquals(
                "Produto transferido para o paciente com sucesso.",
                resultado
        );

        SQLiteDatabase db = banco.getReadableDatabase();

        // Verifica o estoque geral.
        Cursor geral = db.rawQuery(
                "SELECT quantidadeatual FROM EstoqueGeral WHERE produtoId = ?",
                new String[]{String.valueOf(produtoId)}
        );

        assertTrue(geral.moveToFirst());
        assertEquals(80, geral.getInt(0));

        geral.close();

        // Verifica o estoque individual do paciente.
        Cursor individual = db.rawQuery(
                "SELECT quantidadeatual FROM Estoque WHERE pacienteId = ? AND produtoId = ?",
                new String[]{
                        String.valueOf(pacienteId),
                        String.valueOf(produtoId)
                }
        );

        assertTrue(individual.moveToFirst());
        assertEquals(20, individual.getInt(0));

        individual.close();

        // Verifica se a movimentação foi registrada.
        Cursor movimentacao = db.rawQuery(
                "SELECT COUNT(*) FROM Movimentacao WHERE usuarioId = ?",
                new String[]{String.valueOf(usuarioId)}
        );

        assertTrue(movimentacao.moveToFirst());
        assertEquals(1, movimentacao.getInt(0));

        movimentacao.close();
        db.close();
    }

    // TESTE 3: Não permitir transferência maior que o estoque.

    @Test
    public void naoDeveTransferirQuantidadeInsuficiente() {

        controller.adicionarEstoqueGeral(produtoId, 10);

        String resultado = controller.transferirParaPaciente(
                produtoId,
                pacienteId,
                20
        );

        assertEquals(
                "Quantidade insuficiente no estoque geral.",
                resultado
        );

        SQLiteDatabase db = banco.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT quantidadeatual FROM EstoqueGeral WHERE produtoId = ?",
                new String[]{String.valueOf(produtoId)}
        );

        assertTrue(cursor.moveToFirst());
        assertEquals(10, cursor.getInt(0));

        cursor.close();

        // Confirma que nenhum estoque individual foi criado.
        Cursor estoquePaciente = db.rawQuery(
                "SELECT COUNT(*) FROM Estoque WHERE pacienteId = ?",
                new String[]{String.valueOf(pacienteId)}
        );

        assertTrue(estoquePaciente.moveToFirst());
        assertEquals(0, estoquePaciente.getInt(0));

        estoquePaciente.close();
        db.close();
    }

    // TESTE 4: Listar estoque geral.

    @Test
    public void deveListarEstoqueGeral() {

        controller.adicionarEstoqueGeral(produtoId, 50);

        Cursor cursor = controller.listarEstoqueGeral();

        assertTrue(cursor.moveToFirst());

        assertEquals(
                "Fralda",
                cursor.getString(cursor.getColumnIndexOrThrow("nome"))
        );

        assertEquals(
                50,
                cursor.getInt(cursor.getColumnIndexOrThrow("quantidadeatual"))
        );

        cursor.close();
    }

    // TESTE 5: Listar estoque individual.

    @Test
    public void deveListarEstoqueDoPaciente() {

        controller.adicionarEstoqueGeral(produtoId, 50);

        controller.transferirParaPaciente(
                produtoId,
                pacienteId,
                15
        );

        Cursor cursor = controller.listarEstoque(pacienteId);

        assertTrue(cursor.moveToFirst());

        assertEquals(
                "Fralda",
                cursor.getString(cursor.getColumnIndexOrThrow("nome"))
        );

        assertEquals(
                15,
                cursor.getInt(cursor.getColumnIndexOrThrow("quantidadeatual"))
        );

        cursor.close();
    }
}
