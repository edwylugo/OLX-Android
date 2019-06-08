package edwy.lugo.olx.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.blackcat.currencyedittext.CurrencyEditText;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.santalu.maskedittext.MaskEditText;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import dmax.dialog.SpotsDialog;
import edwy.lugo.olx.R;
import edwy.lugo.olx.helper.ConfiguracaoFirebase;
import edwy.lugo.olx.helper.Permissoes;
import edwy.lugo.olx.model.Anuncio;

public class CadastrarAnuncioActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText campoTitulo, campoDescricao;
    private CurrencyEditText campoValor;
    private MaskEditText campoTelefone;
    private ImageView campoImagem1, campoImagem2, campoImagem3;
    private Spinner campoEstado, campoCategoria;
    private Anuncio anuncio;
    private StorageReference storage;
    private AlertDialog dialog;

    private String[] permissoes = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private List<String> listaFotosRecuperadas = new ArrayList<>();
    private List<String> listaURLFotos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar_anuncio);

        //Configurações iniciais
        storage = ConfiguracaoFirebase.getFirebaseStorage();

        //Validar permissões
        Permissoes.validarPermissoes(permissoes, this, 1);

        inicializarComponentes();

        //Carregar dados Spinner
        carregarDadosSpinner();
    }

    private void carregarDadosSpinner() {
//        String[] estados = new String[] {
//          "SP","MT"
//        };

        //Configura spinner Estados
        String[] estados = getResources().getStringArray(R.array.estados);
        ArrayAdapter<String> adapterEstado = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, estados);
        adapterEstado.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campoEstado.setAdapter(adapterEstado);

        //Configura spinner Categorias
        String[] categorias = getResources().getStringArray(R.array.categorias);
        ArrayAdapter<String> adapterCategorias = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categorias);
        adapterCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campoCategoria.setAdapter(adapterCategorias);

    }


    public void salvarAnuncio() {

        dialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Salvando Anúncio")
                .setCancelable(false)
                .build();
        dialog.show();

        /*Salvar image no Storage*/

        for(int i=0; i < listaFotosRecuperadas.size(); i++){
            String urlImagem = listaFotosRecuperadas.get(i);
            int tamanhoLista = listaFotosRecuperadas.size();
            salvarFotoStorage(urlImagem, tamanhoLista, i);
        }

    }

    private void salvarFotoStorage(String urlString, final int totalFotos, int contador) {

        //Criar nó no storage
        final StorageReference imagemAnuncio = storage.child("imagens")
                .child("anuncios")
                .child(anuncio.getIdAnuncio())
                .child("imagem"+contador);

        //Fazer upload do arquivo
        UploadTask uploadTask = imagemAnuncio.putFile(Uri.parse(urlString));
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                // get the image Url of the file uploaded
                imagemAnuncio.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        // getting image uri and converting into string

                        Uri firebaseUrl = uri;
                        String urlConvertida = uri.toString();
                        listaURLFotos.add(urlConvertida);

                        if (totalFotos == listaURLFotos.size()) {
                         anuncio.setFotos(listaURLFotos);
                         anuncio.salvar();
                         dialog.dismiss();
                         finish();
                        }

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                exibirMensagemErro("Falha ao fazer upload");
                Log.i("INFO", "Falha ao fazer upload: " + e.getMessage());
            }
        });

    }

    private Anuncio configurarAnuncio(){

        String estado = campoEstado.getSelectedItem().toString();
        String categorias = campoCategoria.getSelectedItem().toString();
        String titulo = campoTitulo.getText().toString();
        String valor = campoValor.getText().toString();
        String telefone = campoTelefone.getText().toString();
        String descricao = campoDescricao.getText().toString();

        Anuncio anuncio = new Anuncio();
        anuncio.setEstado(estado);
        anuncio.setCategoria(categorias);
        anuncio.setTitulo(titulo);
        anuncio.setValor(valor);
        anuncio.setTelefone(telefone);
        anuncio.setDescricao(descricao);

        return anuncio;
    }


    public void validarDadosAnuncio(View view){

        anuncio = configurarAnuncio();

        //Recuperar dados
        String fone = "";
        String valor = String.valueOf(campoValor.getRawValue());


        if (campoTelefone.getRawText() != null){
            fone = campoTelefone.getRawText().toString();
        }

        if(listaFotosRecuperadas.size() != 0){

            if (!anuncio.getEstado().isEmpty()) {
                if (!anuncio.getCategoria().isEmpty()) {
                    if (!anuncio.getTitulo().isEmpty()) {
                        if (!valor.isEmpty() && !valor.equals("0")) {
                            if (!anuncio.getTelefone().isEmpty() && fone.length() >= 10) {
                                if (!anuncio.getDescricao().isEmpty()) {
                                        salvarAnuncio();
                                } else {
                                    exibirMensagemErro("Preencha o campo descrição!");
                                }

                            } else {
                                exibirMensagemErro("Preencha o campo telefone, digite ao menos 10 números!");
                            }

                        } else {
                            exibirMensagemErro("Preencha o campo valor!");
                        }

                    } else {
                        exibirMensagemErro("Preencha o campo título!");
                    }

                } else {
                    exibirMensagemErro("Preencha o campo Categoria!");
                }

            } else {
                exibirMensagemErro("Preencha o campo estado!");
            }

        } else {
            exibirMensagemErro("Selecione ao menos uma foto!");
        }



    }

    private void exibirMensagemErro(String mensagem) {
        Toast.makeText(this,mensagem, Toast.LENGTH_SHORT).show();
    }


    private void inicializarComponentes() {
        campoTitulo = findViewById(R.id.editTitulo);
        campoDescricao = findViewById(R.id.editDescricao);
        campoValor = findViewById(R.id.editValor);
        campoTelefone = findViewById(R.id.editTelefone);
        campoEstado =  findViewById(R.id.spinnerEstado);
        campoCategoria =  findViewById(R.id.spinnerCategoria);

        campoImagem1 = findViewById(R.id.imageCadastro1);
        campoImagem2 = findViewById(R.id.imageCadastro2);
        campoImagem3 = findViewById(R.id.imageCadastro3);
        campoImagem1.setOnClickListener(this);
        campoImagem2.setOnClickListener(this);
        campoImagem3.setOnClickListener(this);


        //Configura localicdade para pt -> portugues BR -> Brasil
        Locale locale = new Locale("pt", "BR");
        campoValor.setLocale(locale);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int permissaoResultado : grantResults) {
            if (permissaoResultado == PackageManager.PERMISSION_DENIED) {
            }
        }
    }

    private void alertaValidacaoPermissao() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissões Negadas");
        builder.setMessage("Para utilizar o app é necessário aceitar as permissões");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.imageCadastro1:
                escolherImage(1);
                break;
            case R.id.imageCadastro2:
                escolherImage(2);
                break;
            case R.id.imageCadastro3:
                escolherImage(3);
                break;
        }

    }

    public void escolherImage(int requestCode) {

        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {

            //Recuperar Imagem
            Uri imagemSelecionada = data.getData();
            String caminhoImagem = imagemSelecionada.toString();

            //Configura imagem no ImageView
            if (requestCode == 1) {
                campoImagem1.setImageURI(imagemSelecionada);
            } else if (requestCode == 2) {
                campoImagem2.setImageURI(imagemSelecionada);
            } else if (requestCode == 3) {
                campoImagem3.setImageURI(imagemSelecionada);
            }

            listaFotosRecuperadas.add(caminhoImagem);

        }
    }
}
