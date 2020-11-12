package sample;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Controller {
    @FXML
    private ImageView largeCover;
    @FXML
    private HBox infoPanel;
    @FXML
    private TextArea description;
    @FXML
    private ImageView loadingGif;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane flowPane;
    private String prevSearchTerm;
    private Timeline searchDelay = new Timeline(
        new KeyFrame(Duration.seconds(1),
           event -> searchMovie()));

    private Runnable searchRun = () -> {
        loadingGif.setVisible(true);
        List<CoverImage> coverList = new ArrayList();
        List<TVDBResult> hits = AlgoliaAPI.getSeriesHits(prevSearchTerm, "Movie", 30);
        int actualHits = 0;
        for (TVDBResult result : hits)
            if (result.getImage() != null && result.getImage().length() >= 1 && !result.getImage().equals("https://artworks.thetvdb.com/banners/images/missing/movie.jpg")) {
                actualHits++;
                Platform.runLater(() -> coverList.add(addCoverElement(result)));
            }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(actualHits);
        while (coverList.size() < actualHits) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (CoverImage covimg : coverList)
            executor.execute(covimg.fetchImage);
        executor.shutdown();
        Platform.runLater(() -> loadingGif.setVisible(false));
    };

    @FXML
    protected void initialize() {
        prevSearchTerm = "";
        loadingGif.setVisible(false);
        infoPanel.setVisible(false);
        infoPanel.setOnMouseClicked(mouseEvent -> {infoPanel.setVisible(false);flowPane.setEffect(null);searchField.setEffect(null);});
    }

    public void checkSearchTimer() {
        loadingGif.setVisible(true);
        searchDelay.stop();
        searchDelay.play();
    }

    private void searchMovie() {
        String searchTerm = searchField.getText();
        if (!prevSearchTerm.equals(searchTerm)) {
            prevSearchTerm = searchTerm;
            flowPane.getChildren().clear();
            Thread t = new Thread(searchRun);
            t.setDaemon(true);
            t.start();
        } else
            loadingGif.setVisible(true);
    }

    private CoverImage addCoverElement(TVDBResult result) {
        CoverImage imgView = new CoverImage(new Image("placeholder.png"), result);
        imgView.setPreserveRatio(true);
        imgView.setFitWidth(250);
        imgView.setEffect(new DropShadow());
        imgView.setCursor(Cursor.HAND);

        ScaleTransition scaleUpTransition = new ScaleTransition();
        scaleUpTransition.setDuration(Duration.millis(100));
        scaleUpTransition.setNode(imgView);
        scaleUpTransition.setToX(1.05);
        scaleUpTransition.setToY(1.05);

        ScaleTransition scaleDownTransition = new ScaleTransition();
        scaleDownTransition.setDuration(Duration.millis(100));
        scaleDownTransition.setNode(imgView);
        scaleDownTransition.setToX(1);
        scaleDownTransition.setToY(1);

        imgView.setOnMouseClicked(mouseEvent -> {
            if (imgView.getInfo().getOverviews() != null && imgView.getInfo().getOverviews().containsKey("eng")) {
                description.setText(imgView.getInfo().getOverviews().get("eng"));
                largeCover.setImage(imgView.getImage());
                flowPane.setEffect(new GaussianBlur(25));
                searchField.setEffect(new GaussianBlur(25));
                infoPanel.setVisible(true);
            }
        });

        imgView.setOnMouseEntered(MouseEvent -> scaleUpTransition.play());
        imgView.setOnMouseExited(MouseEvent -> scaleDownTransition.play());
        flowPane.getChildren().add(imgView);
        return imgView;
    }
}
