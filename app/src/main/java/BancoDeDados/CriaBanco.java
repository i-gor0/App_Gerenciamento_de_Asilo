package BancoDeDados;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CriaBanco extends SQLiteOpenHelper {

    private static final String NOME_BANCO = "Banco_Asilo.db";
    private static final int VERSAO = 1;

    public CriaBanco(Context context) {
        super(context, NOME_BANCO, null, VERSAO);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Perfil (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nome TEXT NOT NULL, " +
                "podecadastrar INTEGER DEFAULT 0, " +
                "podebaixarestoque INTEGER DEFAULT 0, " +
                "podegerenciarusuarios INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE Usuario (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "perfilId INTEGER NOT NULL, " +
                "nome TEXT NOT NULL, " +
                "login TEXT NOT NULL UNIQUE, " +
                "senhahash TEXT NOT NULL, " +
                "ativo INTEGER DEFAULT 1, " +
                "FOREIGN KEY (perfilId) REFERENCES Perfil(Id));");

        db.execSQL("CREATE TABLE Quarto (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "numero TEXT NOT NULL UNIQUE, " +
                "capacidade INTEGER);");

        db.execSQL("CREATE TABLE Paciente (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "quartoId INTEGER NOT NULL, " +
                "nome TEXT NOT NULL, " +
                "datanascimento TEXT, " +
                "observacoes TEXT, " +
                "FOREIGN KEY (quartoId) REFERENCES Quarto(Id));");

        db.execSQL("CREATE TABLE Produto (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nome TEXT NOT NULL, " +
                "tipo TEXT, " +
                "codigoqr TEXT NOT NULL UNIQUE, " +
                "unidademedida TEXT);");

        db.execSQL("CREATE TABLE Estoque (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pacienteId INTEGER NOT NULL, " +
                "produtoId INTEGER NOT NULL, " +
                "quantidadeatual INTEGER DEFAULT 0, " +
                "quantidademinima INTEGER DEFAULT 0, " +
                "UNIQUE (pacienteId, produtoId), " +
                "FOREIGN KEY (pacienteId) REFERENCES Paciente(Id), " +
                "FOREIGN KEY (produtoId) REFERENCES Produto(Id));");

        db.execSQL("CREATE TABLE Movimentacao (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "estoqueId INTEGER NOT NULL, " +
                "usuarioId INTEGER NOT NULL, " +
                "tipo TEXT NOT NULL, " +
                "quantidade INTEGER NOT NULL DEFAULT 1, " +
                "datahora TEXT NOT NULL, " +
                "FOREIGN KEY (estoqueId) REFERENCES Estoque(Id), " +
                "FOREIGN KEY (usuarioId) REFERENCES Usuario(Id));");

        db.execSQL("CREATE TABLE Notificacao (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "movimentacaoId INTEGER NOT NULL, " +
                "mensagem TEXT NOT NULL, " +
                "datahora TEXT NOT NULL, " +
                "lida INTEGER DEFAULT 0, " +
                "FOREIGN KEY (movimentacaoId) REFERENCES Movimentacao(Id));");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Notificacao");
        db.execSQL("DROP TABLE IF EXISTS Movimentacao");
        db.execSQL("DROP TABLE IF EXISTS Estoque");
        db.execSQL("DROP TABLE IF EXISTS Produto");
        db.execSQL("DROP TABLE IF EXISTS Paciente");
        db.execSQL("DROP TABLE IF EXISTS Quarto");
        db.execSQL("DROP TABLE IF EXISTS Usuario");
        db.execSQL("DROP TABLE IF EXISTS Perfil");
        onCreate(db);
    }
}