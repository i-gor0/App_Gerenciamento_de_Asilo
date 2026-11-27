package com.example.projetoasilo;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import BancoDeDados.BancoControllerPerfil;
import BancoDeDados.BancoControllerUser;

/**
 * Tela de login (content_main.xml) + "casca" de navegação que hospeda os fragments.
 * A casca é montada em código, então não precisa de nenhum XML novo
 * nem de alteração no AndroidManifest.
 */
public class MainActivity extends AppCompatActivity {

    private FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Util.logado(this)) {
            mostrarHome();
        } else {
            mostrarLogin();
        }
    }

    // ------------------------------------------------------------------
    // LOGIN  (content_main.xml)
    // ------------------------------------------------------------------
    private void mostrarLogin() {
        removerFragments();
        setContentView(R.layout.content_main);

        EditText edtEmail = findViewById(R.id.edtEmail);   // campo de login
        EditText edtSenha = findViewById(R.id.edtSenha);
        Button btnLogin = findViewById(R.id.btnLogin);

        criarAdminSeBancoVazio();

        btnLogin.setOnClickListener(v -> {
            String login = edtEmail.getText().toString().trim();
            String senha = edtSenha.getText().toString();

            if (login.isEmpty() || senha.isEmpty()) {
                Util.toast(this, "Preencha login e senha.");
                return;
            }

            int resultado = new BancoControllerUser(this)
                    .Login(login, Util.sha256(senha), this);

            if (resultado == 1) {
                edtSenha.setText("");
                mostrarHome();
            } else {
                Util.toast(this, "Login ou senha inválidos.");
            }
        });
    }

    /** Primeiro acesso: sem nenhum usuário não dá para logar, então cria admin / admin. */
    private void criarAdminSeBancoVazio() {
        try {
            BancoControllerUser users = new BancoControllerUser(this);
            if (Util.contar(users.listarUsuarios()) > 0) return;

            BancoControllerPerfil perfis = new BancoControllerPerfil(this);
            perfis.insereDados("Administrador", 1, 1, 1);

            int perfilId = -1;
            Cursor p = perfis.listarPerfis();
            if (p.moveToFirst()) perfilId = Util.num(p, "id", "_id");
            p.close();

            if (perfilId > 0) {
                users.adicionarUsuario("Administrador", "admin", Util.sha256("admin"), perfilId);
                Util.toastLongo(this, "Primeiro acesso: login admin / senha admin");
            }
        } catch (Exception ignorado) {
            // se algo falhar o login simplesmente continua exigindo um usuário existente
        }
    }

    // ------------------------------------------------------------------
    // HOME: barra de navegação + área dos fragments
    // ------------------------------------------------------------------
    private void mostrarHome() {
        removerFragments();

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setFitsSystemWindows(true);

        HorizontalScrollView rolagem = new HorizontalScrollView(this);
        rolagem.setHorizontalScrollBarEnabled(false);
        LinearLayout barra = new LinearLayout(this);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        int pad = Util.dp(this, 4);
        barra.setPadding(pad, pad, pad, pad);

        barra.addView(botaoMenu("Estoque", () -> navegar(new VisualizarEstoqueFragment())));
        barra.addView(botaoMenu("Quartos", () -> navegar(new GerenciarQuartosFragment())));
        barra.addView(botaoMenu("Novo paciente", () -> navegar(new CadastrarPacienteFragment())));
        barra.addView(botaoMenu("Sair", this::sair));
        rolagem.addView(barra);

        container = new FrameLayout(this);
        container.setId(View.generateViewId());

        raiz.addView(rolagem, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        raiz.addView(container, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(raiz);
        navegar(new VisualizarEstoqueFragment());
    }

    private Button botaoMenu(String texto, Runnable acao) {
        Button b = new Button(this);
        b.setText(texto);
        b.setAllCaps(false);
        b.setOnClickListener(v -> acao.run());
        return b;
    }

    /** Navegação pela barra superior: zera a pilha e abre a tela. */
    private void navegar(Fragment f) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        abrir(f, false);
    }

    /** Usado pelos fragments para abrir outra tela (com botão voltar). */
    public void abrir(Fragment f, boolean comVoltar) {
        FragmentTransaction t = getSupportFragmentManager().beginTransaction()
                .replace(container.getId(), f);
        if (comVoltar) t.addToBackStack(null);
        t.commit();
    }

    /** Volta para a tela anterior (ex.: depois de salvar um cadastro). */
    public void voltar() {
        getSupportFragmentManager().popBackStack();
    }

    private void sair() {
        new BancoControllerUser(this).Logout(this);
        mostrarLogin();
    }

    /** Evita fragments "órfãos" ao trocar entre login e home ou ao recriar a Activity. */
    private void removerFragments() {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction t = fm.beginTransaction();
        for (Fragment f : fm.getFragments()) t.remove(f);
        t.commitNowAllowingStateLoss();
    }
}
