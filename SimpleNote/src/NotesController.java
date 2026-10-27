import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NotesController {
    @FXML private StackPane raiz;
    @FXML private VBox telaNotas, telaBusca, telaCalendario, menuLateral;
    @FXML private ListView<Note> listaNotas, resultadosBusca, listaDia;
    @FXML private ScrollPane painelGrade;
    @FXML private GridPane gradeNotas, gradeCalendario;
    @FXML private Label lblEstado, lblMes, lblDia;
    @FXML private Region escurecedor;
    @FXML private Button btnMenu, btnFecharMenu, btnCorPadrao, btnLayout, btnSincronizar, btnNova;
    @FXML private Button btnNavNotas, btnNavCalendario, btnNavBuscar, btnNavMais, btnTema;
    @FXML private MenuButton btnOrdenacao;
    @FXML private TextField campoBusca;

    private final DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter data = DateTimeFormatter.ofPattern("dd/MM/yy");
    private final PauseTransition ocultarMensagem = new PauseTransition(Duration.seconds(2));
    private final List<Button> cartoes = new ArrayList<>();
    private List<Note> notas = new ArrayList<>();
    private int colunasGrade = 1;
    private boolean menuAberto, modoGrade;
    private Stage janela;
    private YearMonth mes = YearMonth.now();
    private LocalDate diaEscolhido = LocalDate.now();
    private int aba; // 0 notas, 1 calendário, 2 busca
    private String ordenacao = CacheStore.configuracao("sort","recentes");
    private String tema = CacheStore.configuracao("theme","escuro");
    private final ChangeListener<Boolean> aoMaximizar = (obs,antigo,atual) ->
        definirModo(CacheStore.modoGrade(atual),false);

    @FXML private void initialize() {
        ocultarMensagem.setOnFinished(e -> lblEstado.setText(""));
        escurecedor.setVisible(false);
        menuLateral.setVisible(false);
        menuLateral.setTranslateX(-300);
        raiz.widthProperty().addListener((o,a,b) -> ajustarMenu());
        raiz.heightProperty().addListener((o,a,b) -> ajustarMenu());

        listaNotas.setPlaceholder(new Label(""));
        listaNotas.setCellFactory(lista -> criarCelula());
        resultadosBusca.setCellFactory(lista -> criarCelula());
        listaDia.setCellFactory(lista -> criarCelula());
        resultadosBusca.setOnKeyPressed(e -> abrirComEnter(e,resultadosBusca));
        listaDia.setOnKeyPressed(e -> abrirComEnter(e,listaDia));
        campoBusca.textProperty().addListener((o,a,b) -> atualizarBusca());
        painelGrade.viewportBoundsProperty().addListener((o,a,b) -> {if(modoGrade) montarGrade();});
        raiz.sceneProperty().addListener((o,a,atual) -> {
            if(atual == null)return;
            atual.windowProperty().addListener((aa,b,c)->{
                if(c instanceof Stage stage)configurarJanela(stage);
            });
            if(atual.getWindow() instanceof Stage stage)configurarJanela(stage);
        });
        raiz.setOnSwipeLeft(e -> trocarAba(Math.min(2,aba+1)));
        raiz.setOnSwipeRight(e -> trocarAba(Math.max(0,aba-1)));
        configurarOrdenacao();
        atualizarTema();
        definirModo(CacheStore.modoGrade(false),false);
        atualizarBotaoCor();
        recarregar();
        SyncService.iniciar(this::recarregar);
        String aviso=Navigation.consumirAviso();
        if(aviso!=null)mostrarAviso(aviso);
        trocarAba(0);
        Platform.runLater(()->{ajustarMenu();listaNotas.requestFocus();});
    }

    private ListCell<Note> criarCelula() {
        return new ListCell<Note>() {
            private final Label titulo=new Label();
            private final Label dataNota=new Label();
            private final HBox linha=new HBox(11,titulo,dataNota);
            {
                titulo.getStyleClass().add("nota-previa");
                titulo.setTextOverrun(OverrunStyle.ELLIPSIS);
                titulo.setMinWidth(0);
                titulo.setMaxWidth(Double.MAX_VALUE);
                titulo.setWrapText(true);
                HBox.setHgrow(titulo,Priority.ALWAYS);
                dataNota.getStyleClass().add("nota-data");
                dataNota.setMinWidth(63);
                dataNota.setAlignment(Pos.CENTER_RIGHT);
                linha.setAlignment(Pos.CENTER_LEFT);
                linha.getStyleClass().add("nota-linha");
                linha.prefWidthProperty().bind(widthProperty().subtract(18));
                setOnMouseClicked(e->{
                    if(e.getButton()==MouseButton.PRIMARY && !isEmpty())abrirNota(getItem());
                });
            }
            @Override protected void updateItem(Note nota,boolean vazio) {
                super.updateItem(nota,vazio);
                if(vazio || nota==null) {setGraphic(null);setText(null);return;}
                titulo.setText(nota.getTituloExibido());
                dataNota.setText(formatarData(nota));
                linha.setStyle("-fx-border-color: transparent transparent transparent " +
                    NoteColors.borda(nota.getCor()) + ";");
                setGraphic(linha);setText(null);
            }
        };
    }

    private void configurarJanela(Stage stage) {
        if(janela==stage)return;
        janela=stage;
        stage.maximizedProperty().addListener(aoMaximizar);
        stage.sceneProperty().addListener((o,a,atual)->{
            if(atual!=raiz.getScene())stage.maximizedProperty().removeListener(aoMaximizar);
        });
        definirModo(CacheStore.modoGrade(stage.isMaximized()),false);
    }
    private String formatarData(Note nota) {
        return nota.getAlteradoEm().toLocalDate().equals(LocalDate.now())
            ? nota.getAlteradoEm().format(hora):nota.getAlteradoEm().format(data);
    }
    private void ajustarMenu() {
        escurecedor.resizeRelocate(0,0,raiz.getWidth(),raiz.getHeight());
        menuLateral.resizeRelocate(0,0,Math.min(284,raiz.getWidth()*0.82),raiz.getHeight());
    }
    private void mostrarAviso(String mensagem) {
        lblEstado.setText(mensagem);
        ocultarMensagem.playFromStart();
    }
    public void recarregar() {
        try {
            int selecionado=listaNotas.getSelectionModel().getSelectedItem()==null?-1:listaNotas.getSelectionModel().getSelectedItem().getId();
            notas=CacheStore.listar();
            ordenarNotas();
            listaNotas.setItems(FXCollections.observableArrayList(notas));
            for(Note nota:notas)if(nota.getId()==selecionado) {listaNotas.getSelectionModel().select(nota);break;}
            if(modoGrade)montarGrade();
            atualizarBusca();
            atualizarCalendario();
            atualizarBotaoCor();
        } catch(SQLException erro) {mostrarAviso("Erro ao abrir notas locais.");erro.printStackTrace();}
    }
    private void ordenarNotas() {
        Comparator<Note> criterio=switch(ordenacao) {
            case "antigas" -> Comparator.comparing(Note::getAlteradoEm);
            case "cor" -> Comparator.comparingInt(Note::getCor).thenComparing(Note::getAlteradoEm).reversed();
            case "titulo" -> Comparator.comparing(Note::getTituloExibido,String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Note::getAlteradoEm).reversed();
        };
        notas.sort(criterio);
    }
    private void configurarOrdenacao() {
        String[][] opcoes={{"recentes","Data de modificação"},{"antigas","Mais antigas"},
            {"cor","Cor"},{"titulo","Título"}};
        for(String[] opcao:opcoes) {
            MenuItem item=new MenuItem(opcao[1]);
            item.setOnAction(e->{
                ordenacao=opcao[0];
                try {CacheStore.definirConfiguracao("sort",ordenacao);} catch(SQLException erro){erro.printStackTrace();}
                recarregar();
            });
            btnOrdenacao.getItems().add(item);
        }
        atualizarRotuloOrdenacao();
    }
    private void atualizarRotuloOrdenacao() {
        String rotulo=switch(ordenacao) {
            case "antigas"->"Ordenar por notas antigas";
            case "cor"->"Ordenar por cor";
            case "titulo"->"Ordenar por título";
            default->"Ordenar por data de modificação";
        };
        btnOrdenacao.setText(rotulo + "  ▾");
    }
    private void atualizarBotaoCor() {
        int cor=CacheStore.corPadrao();
        btnCorPadrao.setStyle("-fx-background-color:"+NoteColors.fundo(cor)+
            ";-fx-border-color:"+NoteColors.borda(cor)+";");
    }
    @FXML private void alternarLayout() {definirModo(!modoGrade,true);}
    private void definirModo(boolean grade,boolean guardar) {
        modoGrade=grade;
        listaNotas.setManaged(!grade);listaNotas.setVisible(!grade);
        painelGrade.setManaged(grade);painelGrade.setVisible(grade);
        SVGPath icone=new SVGPath();
        icone.setContent(grade ? "M3 3 H9 V9 H3 Z M12 3 H18 V9 H12 Z M3 12 H9 V18 H3 Z M12 12 H18 V18 H12 Z" :
            "M3 4 H18 M3 10 H18 M3 16 H18");
        icone.setStroke(javafx.scene.paint.Color.web("#79c9bd"));
        icone.setStrokeWidth(2.0);
        icone.setFill(javafx.scene.paint.Color.TRANSPARENT);
        btnLayout.setGraphic(icone);
        btnLayout.setTooltip(new Tooltip(grade?"Exibir lista":"Exibir blocos"));
        if(guardar) {
            try {CacheStore.definirModoGrade(janela!=null&&janela.isMaximized(),grade);}
            catch(SQLException erro) {mostrarAviso("Não foi possível guardar o layout.");}
        }
        if(grade)montarGrade();
        if(guardar)Platform.runLater(()->{
            if(grade&&!cartoes.isEmpty())cartoes.get(0).requestFocus();
            else if(!grade)listaNotas.requestFocus();
        });
    }
    private void montarGrade() {
        if(!modoGrade || painelGrade.getScene()==null&&raiz.getWidth()==0)return;
        double largura=painelGrade.getViewportBounds().getWidth();
        if(largura<100)largura=Math.max(300,raiz.getWidth()-26);
        int colunas=Math.max(1,(int)((largura+12)/174));
        colunasGrade=colunas;
        Integer focoAnterior=null;
        for(Button anterior:cartoes)if(anterior.isFocused()){focoAnterior=(Integer)anterior.getUserData();break;}
        gradeNotas.getChildren().clear();gradeNotas.getColumnConstraints().clear();cartoes.clear();
        gradeNotas.setPadding(new Insets(12));
        for(int i=0;i<colunas;i++) {
            ColumnConstraints coluna=new ColumnConstraints();
            coluna.setPercentWidth(100.0/colunas);coluna.setFillWidth(true);
            gradeNotas.getColumnConstraints().add(coluna);
        }
        for(int i=0;i<notas.size();i++) {
            Note nota=notas.get(i);
            Button cartao=criarCartao(nota);cartao.setUserData(nota.getId());cartoes.add(cartao);
            gradeNotas.add(cartao,i%colunas,i/colunas);GridPane.setHgrow(cartao,Priority.ALWAYS);
            final int indice=i;
            cartao.setOnKeyPressed(e->teclaCartao(e,indice));
        }
        if(focoAnterior!=null)for(Button cartao:cartoes)if(focoAnterior.equals(cartao.getUserData())) {
            Platform.runLater(cartao::requestFocus);break;
        }
    }
    private Button criarCartao(Note nota) {
        Label resumo=new Label(nota.getTituloExibido());
        resumo.getStyleClass().add("cartao-texto");resumo.setWrapText(true);
        resumo.setMaxWidth(Double.MAX_VALUE);resumo.setMaxHeight(92);
        resumo.setMinHeight(Region.USE_PREF_SIZE);
        Label dataNota=new Label(formatarData(nota));dataNota.getStyleClass().add("nota-data");
        HBox rodape=new HBox(dataNota);rodape.setAlignment(Pos.CENTER_RIGHT);
        Region espaco=new Region();VBox.setVgrow(espaco,Priority.ALWAYS);
        VBox conteudo=new VBox(4,resumo,espaco,rodape);
        conteudo.setAlignment(Pos.TOP_LEFT);conteudo.setMaxWidth(Double.MAX_VALUE);conteudo.setFillWidth(true);
        Button botao=new Button();botao.setGraphic(conteudo);
        conteudo.prefWidthProperty().bind(botao.widthProperty().subtract(24));
        conteudo.prefHeightProperty().bind(botao.heightProperty().subtract(24));
        botao.getStyleClass().add("cartao-nota");botao.setMaxWidth(Double.MAX_VALUE);
        botao.setPrefHeight(135);botao.setMinHeight(135);
        botao.setStyle("-fx-background-color:"+NoteColors.fundo(nota.getCor())+
            ";-fx-border-color:"+NoteColors.borda(nota.getCor())+";");
        botao.setOnAction(e->abrirNota(nota));return botao;
    }
    private void teclaCartao(KeyEvent e,int indice) {
        int proximo=indice;
        if(e.getCode()==KeyCode.RIGHT)proximo++;
        else if(e.getCode()==KeyCode.LEFT)proximo--;
        else if(e.getCode()==KeyCode.DOWN)proximo+=colunasGrade;
        else if(e.getCode()==KeyCode.UP)proximo-=colunasGrade;
        else return;
        if(proximo>=0&&proximo<cartoes.size()) {
            cartoes.get(proximo).requestFocus();
            painelGrade.setVvalue(Math.min(1.0,(double)(proximo/colunasGrade)/
                Math.max(1,(notas.size()+colunasGrade-1)/colunasGrade-1)));
        }
        e.consume();
    }
    @FXML private void novaNota() {abrirNota(null);}
    private void abrirNota(Note nota) {
        try {Navigation.abrirEditor(listaNotas,nota);}
        catch(IOException erro){mostrarAviso("Erro ao abrir nota.");erro.printStackTrace();}
    }
    private void abrirComEnter(KeyEvent e,ListView<Note> lista) {
        if(e.getCode()==KeyCode.ENTER && lista.getSelectionModel().getSelectedItem()!=null) {
            abrirNota(lista.getSelectionModel().getSelectedItem());e.consume();
        }
    }
    @FXML private void teclaLista(KeyEvent e) {abrirComEnter(e,listaNotas);}
    @FXML private void teclaBusca(KeyEvent e) {
        if(e.getCode()==KeyCode.ESCAPE) {mostrarNotas();e.consume();}
        else if(e.getCode()==KeyCode.DOWN) {resultadosBusca.requestFocus();
            resultadosBusca.getSelectionModel().selectFirst();e.consume();}
    }
    private void atualizarBusca() {
        if(resultadosBusca==null||notas==null)return;
        String busca=campoBusca.getText().strip().toLowerCase(Locale.ROOT);
        List<Note> resultado=notas.stream().filter(n->busca.isEmpty() ||
            n.getConteudo().toLowerCase(Locale.ROOT).contains(busca) ||
            n.getTituloExibido().toLowerCase(Locale.ROOT).contains(busca)).toList();
        resultadosBusca.setItems(FXCollections.observableArrayList(resultado));
    }
    @FXML private void mostrarNotas() {trocarAba(0);if(menuAberto)fecharMenu();}
    @FXML private void mostrarCalendario() {trocarAba(1);if(menuAberto)fecharMenu();}
    @FXML private void mostrarBusca() {trocarAba(2);if(menuAberto)fecharMenu();}
    private void trocarAba(int numero) {
        aba=numero;
        telaNotas.setVisible(numero==0);telaNotas.setManaged(numero==0);
        telaCalendario.setVisible(numero==1);telaCalendario.setManaged(numero==1);
        telaBusca.setVisible(numero==2);telaBusca.setManaged(numero==2);
        btnNavNotas.getStyleClass().remove("nav-ativo");
        btnNavCalendario.getStyleClass().remove("nav-ativo");
        btnNavBuscar.getStyleClass().remove("nav-ativo");
        (numero==0?btnNavNotas:numero==1?btnNavCalendario:btnNavBuscar).getStyleClass().add("nav-ativo");
        btnLayout.setDisable(numero!=0);btnNova.setVisible(numero!=2);btnNova.setManaged(numero!=2);
        if(numero==1)atualizarCalendario();
        if(numero==2)Platform.runLater(campoBusca::requestFocus);
        else if(numero==0)Platform.runLater(()->{
            if(modoGrade&&!cartoes.isEmpty())cartoes.get(0).requestFocus();
            else listaNotas.requestFocus();
        });
    }
    @FXML private void mesAnterior(){mes=mes.minusMonths(1);atualizarCalendario();}
    @FXML private void mesSeguinte(){mes=mes.plusMonths(1);atualizarCalendario();}
    private void atualizarCalendario() {
        if(gradeCalendario==null)return;
        String titulo=mes.getMonth().getDisplayName(TextStyle.FULL_STANDALONE,new Locale("pt","BR"));
        lblMes.setText(titulo.substring(0,1).toUpperCase()+titulo.substring(1)+" de "+mes.getYear());
        gradeCalendario.getChildren().clear();gradeCalendario.getColumnConstraints().clear();
        for(int c=0;c<7;c++) {
            ColumnConstraints col=new ColumnConstraints();col.setPercentWidth(100.0/7);
            gradeCalendario.getColumnConstraints().add(col);
        }
        String[] dias={"Seg","Ter","Qua","Qui","Sex","Sáb","Dom"};
        for(int c=0;c<7;c++) {
            Label l=new Label(dias[c]);l.getStyleClass().add("calendario-semana");
            l.setMaxWidth(Double.MAX_VALUE);l.setAlignment(Pos.CENTER);gradeCalendario.add(l,c,0);
        }
        int primeiro=mes.atDay(1).getDayOfWeek().getValue()-1;
        int ultimo=mes.lengthOfMonth();
        for(int dia=1;dia<=ultimo;dia++) {
            LocalDate atual=mes.atDay(dia);
            long qtd=notas.stream().filter(n->n.getAlteradoEm().toLocalDate().equals(atual)).count();
            Button b=new Button(Integer.toString(dia)+(qtd>0?" ·"+qtd:""));
            b.getStyleClass().add("calendario-botao");
            if(atual.equals(diaEscolhido))b.getStyleClass().add("calendario-selecionado");
            if(atual.equals(LocalDate.now()))b.getStyleClass().add("calendario-hoje");
            b.setMaxWidth(Double.MAX_VALUE);b.setMinWidth(0);
            b.setOnAction(e->{diaEscolhido=atual;atualizarCalendario();});
            int pos=primeiro+dia-1;
            gradeCalendario.add(b,pos%7,pos/7+1);
        }
        lblDia.setText("Notas de "+diaEscolhido.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        listaDia.setItems(FXCollections.observableArrayList(notas.stream()
            .filter(n->n.getAlteradoEm().toLocalDate().equals(diaEscolhido)).toList()));
    }
    @FXML private void sincronizarAgora() {
        mostrarAviso("Sincronizando...");btnSincronizar.setDisable(true);
        SyncService.manual(texto->{mostrarAviso(texto);btnSincronizar.setDisable(false);recarregar();});
    }
    @FXML private void alternarMenu(){if(menuAberto)fecharMenu();else abrirMenu();}
    private void abrirMenu() {
        menuAberto=true;escurecedor.setOpacity(0);escurecedor.setVisible(true);
        menuLateral.setTranslateX(-300);menuLateral.setVisible(true);
        FadeTransition f=new FadeTransition(Duration.millis(180),escurecedor);
        f.setFromValue(0);f.setToValue(1);
        TranslateTransition t=new TranslateTransition(Duration.millis(180),menuLateral);
        t.setFromX(-300);t.setToX(0);
        new ParallelTransition(f,t).play();Platform.runLater(btnFecharMenu::requestFocus);
    }
    @FXML private void fecharMenu() {
        if(!menuAberto)return;
        menuAberto=false;
        FadeTransition f=new FadeTransition(Duration.millis(160),escurecedor);f.setToValue(0);
        TranslateTransition t=new TranslateTransition(Duration.millis(160),menuLateral);t.setToX(-300);
        ParallelTransition a=new ParallelTransition(f,t);
        a.setOnFinished(e->{escurecedor.setVisible(false);menuLateral.setVisible(false);});
        a.play();btnMenu.requestFocus();
    }
    @FXML private void teclaPagina(KeyEvent e) {
        if(e.getCode()==KeyCode.ESCAPE) {
            if(menuAberto)fecharMenu();
            else if(aba!=0)mostrarNotas();
            else return;
            e.consume();
        } else if(e.isControlDown()&&e.getCode()==KeyCode.RIGHT) {
            trocarAba(Math.min(2,aba+1));e.consume();
        } else if(e.isControlDown()&&e.getCode()==KeyCode.LEFT) {
            trocarAba(Math.max(0,aba-1));e.consume();
        }
    }
    @FXML private void abrirConfiguracoesCores() {
        if(menuAberto)fecharMenu();
        ColorPalette.configuracoes((Stage)raiz.getScene().getWindow(),this::atualizarBotaoCor);
    }
    @FXML private void alternarTema() {
        tema="escuro".equals(tema)?"claro":"escuro";
        try{CacheStore.definirConfiguracao("theme",tema);}catch(SQLException erro){erro.printStackTrace();}
        atualizarTema();
    }
    private void atualizarTema() {
        raiz.getStyleClass().remove("dark-mode");
        if("escuro".equals(tema))raiz.getStyleClass().add("dark-mode");
        btnTema.setText("Tema: "+tema);
    }
    @FXML private void sobreOApp() {
        if(menuAberto)fecharMenu();
        SupportView.abrir((Stage)raiz.getScene().getWindow());
    }
}
